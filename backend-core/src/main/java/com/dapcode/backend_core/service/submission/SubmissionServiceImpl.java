package com.dapcode.backend_core.service.submission;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.dto.submission.SubmissionDetailResponse;
import com.dapcode.backend_core.dto.submission.SubmissionProgressEvent;
import com.dapcode.backend_core.dto.submission.SubmissionResponse;
import com.dapcode.backend_core.dto.submission.SubmitCodeRequest;
import com.dapcode.backend_core.dto.submission.TestCaseResult;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.Submission;
import com.dapcode.backend_core.entity.TestCase;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.judge0.Judge0Client;
import com.dapcode.backend_core.judge0.Judge0Response;
import com.dapcode.backend_core.judge0.Judge0Status;
import com.dapcode.backend_core.judge0.Judge0SubmissionRequest;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.SubmissionRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final Judge0Client judge0Client;
    private final SubmissionStreamManager streamManager;
    private final CodeHarnessService codeHarnessService;
    private final TransactionTemplate transactionTemplate;

    public SubmissionServiceImpl(
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository,
            UserRepository userRepository,
            Judge0Client judge0Client,
            SubmissionStreamManager streamManager,
            CodeHarnessService codeHarnessService) {
        this(problemRepository, testCaseRepository, submissionRepository, userRepository,
                judge0Client, streamManager, codeHarnessService, null);
    }

    @Autowired
    public SubmissionServiceImpl(
            ProblemRepository problemRepository,
            TestCaseRepository testCaseRepository,
            SubmissionRepository submissionRepository,
            UserRepository userRepository,
            Judge0Client judge0Client,
            SubmissionStreamManager streamManager,
            CodeHarnessService codeHarnessService,
            TransactionTemplate transactionTemplate) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.judge0Client = judge0Client;
        this.streamManager = streamManager;
        this.codeHarnessService = codeHarnessService;
        this.transactionTemplate = transactionTemplate;
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> action.get());
        }
        return action.get();
    }

    private record SubmissionSetup(
            Problem problem,
            List<TestCase> testCases,
            UUID submissionId,
            int resolvedLanguageId) {
    }

    private record FinalizationResult(
            int xpEarned,
            int currentStreak) {
    }

    private record EvaluationSummary(
            String finalStatus,
            double totalRuntimeMs,
            int maxMemoryKb,
            int passedCount,
            List<TestCaseResult> testCaseResults) {
    }

    private Problem findProblemBySlug(String slug) {
        return problemRepository.findBySlug(slug).orElseThrow(() -> new AppException(ErrorCode.PROBLEM_NOT_FOUND));
    }

    private Users findUserById(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private int resolveLanguageId(Integer requestedId, String code) {
        if (code == null || code.isBlank()) {
            return requestedId != null ? requestedId : 71;
        }
        String clean = code.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "")
                .replaceAll("(?m)#.*$", "");
        if (clean.contains("def ") && clean.contains("self") && !clean.contains("public:")
                && !clean.contains("public ")) {
            return 71;
        }
        if (clean.contains("public:") || clean.contains("vector<") || clean.contains("#include")) {
            return 54;
        }
        if (!clean.contains("public:") && (clean.contains("public class ") || clean.contains("public int")
                || clean.contains("public boolean") || clean.contains("public String")
                || clean.contains("public List<"))) {
            return 62;
        }
        return requestedId != null ? requestedId : 71;
    }

    private boolean isOutputMatched(String actual, String expected) {
        return SubmissionOutputValidator.isOutputMatched(actual, expected);
    }

    private boolean isOutputMatched(String problemSlug, String inputData, String actual, String expected) {
        return SubmissionOutputValidator.isOutputMatched(problemSlug, inputData, actual, expected);
    }

    private EvaluationSummary evalutateResults(String problemSlug, List<TestCase> testCases,
            List<Judge0Response> judge0Responses, UUID submissionId) {
        List<TestCaseResult> testCaseResults = new ArrayList<>();

        int passedCount = 0;

        double totalRuntimeMs = 0.0;
        int maxMemoryKb = 0;
        String finalStatus = "ACCEPTED";

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            Judge0Response res = (i < judge0Responses.size()) ? judge0Responses.get(i) : null;

            double timeMs = (res != null && res.getTime() != null) ? res.getTime() * 1000.0 : 0.0;

            int memKb = (res != null && res.getMemory() != null) ? res.getMemory() : 0;
            totalRuntimeMs += timeMs;
            maxMemoryKb = Math.max(maxMemoryKb, memKb);

            String caseStatus = "INTERNAL_ERROR";
            if (res != null) {
                Judge0Status jStatus = res.getJudge0Status();
                if (jStatus == Judge0Status.ACCEPTED || jStatus == Judge0Status.WRONG_ANSWER) {
                    if (isOutputMatched(problemSlug, tc.getInputData(), res.getStdout(), tc.getExpectedOutput())) {
                        caseStatus = "ACCEPTED";
                        passedCount++;
                    } else {
                        caseStatus = "WRONG_ANSWER";
                    }
                } else {
                    caseStatus = jStatus.name();
                }
            }

            if (!"ACCEPTED".equals(caseStatus) && "ACCEPTED".equals(finalStatus)) {
                finalStatus = caseStatus;
            }

            TestCaseResult tcResult = TestCaseResult.builder()
                    .testCaseId(tc.getId())
                    .status(caseStatus)
                    .runtimeMs(timeMs)
                    .memoryKb(memKb)
                    .isHidden(tc.isHidden())
                    .inputData(tc.isHidden() ? null : tc.getInputData())
                    .expectedOutput(tc.isHidden() ? null : tc.getExpectedOutput())
                    .actualOutput(tc.isHidden() ? null
                            : (res != null ? (res.getStdout() != null ? res.getStdout() : "") : null))
                    .errMes(res != null ? (res.getStderr() != null ? res.getStderr() : res.getCompile_output()) : null)
                    .build();
            testCaseResults.add(tcResult);

            SubmissionProgressEvent progressEvent = SubmissionProgressEvent.builder().submissionId(submissionId)
                    .currentTest(i + 1)
                    .totalTests(testCases.size())
                    .status(caseStatus)
                    .runtimeMs(timeMs)
                    .memoryKb(memKb)
                    .message("Test case " + (i + 1) + "/" + testCases.size() + ": " + caseStatus)
                    .build();

            streamManager.sendProgress(submissionId, progressEvent);
        }

        return new EvaluationSummary(finalStatus, totalRuntimeMs, maxMemoryKb, passedCount, testCaseResults);
    }

    private List<Judge0Response> executePerCase(Problem prob, List<TestCase> testCases, int resolvedLanguageId,
            String preparedCode) {
        List<Judge0SubmissionRequest> requests = testCases.stream()
                .map(tc -> Judge0SubmissionRequest.builder()
                        .language_id(resolvedLanguageId)
                        .compiler_options(resolvedLanguageId == 54 ? "-std=c++17" : null)
                        .source_code(preparedCode)
                        .stdin(tc.getInputData())
                        .expected_output(tc.getExpectedOutput())
                        .cpu_time_limit(prob.getTimeLimitMs() / 1000.0)
                        .memory_limit(resolvedLanguageId == 62
                                ? Math.max(prob.getMemoryLimitKb(), 1048576)
                                : prob.getMemoryLimitKb())
                        .build())
                .toList();
        return judge0Client.executeBatch(requests);
    }

    private List<Judge0Response> executeWithSingleCompileOptimization(
            Problem prob,
            List<TestCase> testCases,
            int resolvedLanguageId,
            String rawSourceCode,
            String preparedCode) {
        if (testCases.size() <= 1 || !codeHarnessService.supportsBatchExecution(resolvedLanguageId, rawSourceCode)) {
            return executePerCase(prob, testCases, resolvedLanguageId, preparedCode);
        }

        double perCaseLimitSec = prob.getTimeLimitMs() / 1000.0;
        double batchCpuLimitSec = Math.min(10.0, Math.max(2.0, perCaseLimitSec * Math.min(testCases.size(), 5)));

        Judge0SubmissionRequest batchReq = Judge0SubmissionRequest.builder()
                .language_id(resolvedLanguageId)
                .compiler_options(resolvedLanguageId == 54 ? "-std=c++17" : null)
                .source_code(preparedCode)
                .stdin(BatchExecutionProtocol.encodeBatchInput(testCases))
                .cpu_time_limit(batchCpuLimitSec)
                .memory_limit(resolvedLanguageId == 62
                        ? Math.max(prob.getMemoryLimitKb(), 1048576)
                        : prob.getMemoryLimitKb())
                .build();

        List<Judge0Response> batchList = judge0Client.executeBatch(List.of(batchReq));
        if (batchList != null && !batchList.isEmpty()) {
            Judge0Response batchRes = batchList.get(0);
            Judge0Status jStatus = batchRes.getJudge0Status();

            if (jStatus == Judge0Status.COMPILATION_ERROR) {
                return Collections.nCopies(testCases.size(), batchRes);
            }

            if (jStatus == Judge0Status.ACCEPTED || jStatus == Judge0Status.WRONG_ANSWER) {
                List<BatchExecutionProtocol.CaseOutcome> outcomes = BatchExecutionProtocol.parseBatchOutput(
                        batchRes.getStdout(), testCases.size());
                if (outcomes != null) {
                    List<Judge0Response> syntheticResponses = new ArrayList<>(outcomes.size());
                    for (BatchExecutionProtocol.CaseOutcome outcome : outcomes) {
                        Judge0Response item = new Judge0Response();
                        item.setTime(outcome.runtimeMs() / 1000.0);
                        item.setMemory(batchRes.getMemory());

                        Judge0Response.Status st = new Judge0Response.Status();
                        if (!outcome.ok()) {
                            st.setId(Judge0Status.RUNTIME_ERROR_NZEC.getId());
                            st.setDescription("Runtime Error (NZEC)");
                            item.setStderr(encodeBase64(outcome.payload()));
                        } else if (outcome.runtimeMs() > prob.getTimeLimitMs()) {
                            st.setId(Judge0Status.TIME_LIMIT_EXCEEDED.getId());
                            st.setDescription("Time Limit Exceeded");
                        } else {
                            st.setId(Judge0Status.ACCEPTED.getId());
                            st.setDescription("Accepted");
                            item.setStdout(encodeBase64(outcome.payload()));
                        }
                        item.setStatus(st);
                        syntheticResponses.add(item);
                    }
                    return syntheticResponses;
                }
            }

            log.info("Single-compile batch ended with {} for slug={} (lang={}), falling back to per-case execution",
                    jStatus, prob.getSlug(), resolvedLanguageId);
        }

        return executePerCase(prob, testCases, resolvedLanguageId, preparedCode);
    }

    private static String encodeBase64(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public SubmissionResponse submitAndEvaluate(SubmitCodeRequest request) {
        SubmissionSetup setup = executeInTransaction(() -> {
            Problem prob = findProblemBySlug(request.getProblemSlug());

            List<TestCase> testCases = testCaseRepository.findByProblemId(prob.getId());
            if (testCases.isEmpty()) {
                throw new AppException(ErrorCode.TEST_CASE_NOT_FOUND);
            }

            Users user = findUserById(request.getUserId());

            int resolvedLanguageId = resolveLanguageId(request.getLanguageId(), request.getSourceCode());

            Submission submission = Submission.builder()
                    .problem(prob)
                    .user(user)
                    .languageId(resolvedLanguageId)
                    .sourceCode(request.getSourceCode())
                    .status("PENDING")
                    .build();
            submission = submissionRepository.save(submission);

            return new SubmissionSetup(prob, testCases, submission.getId(), resolvedLanguageId);
        });

        EvaluationSummary summary;
        try {
            String preparedCode = codeHarnessService.prepareSourceCode(
                    setup.problem().getSlug(),
                    setup.resolvedLanguageId(),
                    request.getSourceCode());

            List<Judge0Response> judge0Responses = executeWithSingleCompileOptimization(
                    setup.problem(),
                    setup.testCases(),
                    setup.resolvedLanguageId(),
                    request.getSourceCode(),
                    preparedCode);

            summary = evalutateResults(setup.problem().getSlug(), setup.testCases(), judge0Responses,
                    setup.submissionId());
        } catch (RuntimeException ex) {
            executeInTransaction(() -> {
                submissionRepository.findById(setup.submissionId()).ifPresent(sub -> {
                    sub.setStatus("INTERNAL_ERROR");
                    submissionRepository.save(sub);
                });
                return null;
            });
            throw ex;
        }

        final EvaluationSummary finalSummary = summary;
        FinalizationResult fin = executeInTransaction(() -> {
            int xpEarned = 0;
            int currentStreak = 0;

            if (request.getUserId() != null) {
                if ("ACCEPTED".equals(finalSummary.finalStatus())) {
                    Users lockedUser = userRepository.findByIdForUpdate(request.getUserId())
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                    boolean daGiai = submissionRepository.existsByUserIdAndProblemIdAndStatus(
                            lockedUser.getId(), setup.problem().getId(), "ACCEPTED");
                    if (!daGiai) {
                        xpEarned = switch (setup.problem().getDifficulty().toLowerCase()) {
                            case "easy" -> 20;
                            case "medium" -> 50;
                            case "hard" -> 100;
                            default -> 20;
                        };
                        lockedUser.setXp(lockedUser.getXp() + xpEarned);
                    }

                    ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
                    LocalDate today = LocalDate.now(zone);
                    var lastAcceptedOpt = submissionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                            lockedUser.getId(), "ACCEPTED");

                    if (lastAcceptedOpt.isEmpty() || lastAcceptedOpt.get().getCreatedAt() == null) {
                        lockedUser.setStreakCount(1);
                    } else {
                        LocalDate lastDate = lastAcceptedOpt.get().getCreatedAt().atZone(zone).toLocalDate();
                        if (lastDate.equals(today)) {
                            if (lockedUser.getStreakCount() <= 0) {
                                lockedUser.setStreakCount(1);
                            }
                        } else if (lastDate.equals(today.minusDays(1))) {
                            lockedUser.setStreakCount(lockedUser.getStreakCount() + 1);
                        } else {
                            lockedUser.setStreakCount(1);
                        }
                    }
                    userRepository.save(lockedUser);
                    currentStreak = lockedUser.getStreakCount();
                } else {
                    Users freshUser = findUserById(request.getUserId());
                    currentStreak = freshUser != null ? freshUser.getStreakCount() : 0;
                }
            }

            Submission managedSubmission = submissionRepository.findById(setup.submissionId())
                    .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
            managedSubmission.setStatus(finalSummary.finalStatus());
            managedSubmission.setTotalRuntimeMs(finalSummary.totalRuntimeMs());
            managedSubmission.setMaxMemoryKb(finalSummary.maxMemoryKb());
            submissionRepository.save(managedSubmission);

            return new FinalizationResult(xpEarned, currentStreak);
        });

        FinalizationResult safeFin = fin != null ? fin : new FinalizationResult(0, 0);

        SubmissionResponse response = SubmissionResponse.builder()
                .submissionId(setup.submissionId())
                .slug(setup.problem().getSlug())
                .status(finalSummary.finalStatus())
                .totalRuntimeMs((int) finalSummary.totalRuntimeMs())
                .maxMemoryKb(finalSummary.maxMemoryKb())
                .passedTestCases(finalSummary.passedCount())
                .xpEarned(safeFin.xpEarned())
                .currentStreak(safeFin.currentStreak())
                .testCaseResults(finalSummary.testCaseResults())
                .build();

        streamManager.sendCompleted(setup.submissionId(), response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SseEmitter subscribeStream(UUID submissionId, Users currUser) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (currUser != null && !currUser.getId().equals(submission.getUser().getId())
                && !"ROLE_ADMIN".equals(currUser.getRole())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        SseEmitter emitter = streamManager.createEmitter(submissionId);

        if (!"PENDING".equals(submission.getStatus())) {
            SubmissionDetailResponse detail = SubmissionDetailResponse.builder()
                    .id(submission.getId())
                    .problemSlug(submission.getProblem().getSlug())
                    .problemTitle(submission.getProblem().getTitle())
                    .status(submission.getStatus())
                    .sourceCode(submission.getSourceCode())
                    .languageId(submission.getLanguageId())
                    .totalRuntimeMs(submission.getTotalRuntimeMs())
                    .maxMemoryKb(submission.getMaxMemoryKb())
                    .createdAt(submission.getCreatedAt())
                    .build();
            streamManager.sendCompleted(submissionId, detail);
        }
        return emitter;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmissionById(UUID id, Users currUser) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (currUser != null && !currUser.getId().equals(submission.getUser().getId())
                && !"ROLE_ADMIN".equals(currUser.getRole())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return SubmissionDetailResponse.builder().id(submission.getId())
                .problemSlug(submission.getProblem().getSlug())
                .problemTitle(submission.getProblem().getTitle())
                .status(submission.getStatus())
                .sourceCode(submission.getSourceCode())
                .languageId(submission.getLanguageId())
                .totalRuntimeMs(submission.getTotalRuntimeMs())
                .maxMemoryKb(submission.getMaxMemoryKb())
                .createdAt(submission.getCreatedAt()).build();
    }

    @Override
    public List<String> getSolvedProblemSlugs(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return submissionRepository.findSolvedProblemSlugsByUserId(userId);
    }

}
