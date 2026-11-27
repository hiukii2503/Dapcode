package com.dapcode.backend_core.service.submission;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dapcode.backend_core.entity.TestCase;

final class BatchExecutionProtocol {

    static final String BATCH_HEADER = "<<<DAPCODE_BATCH_V1>>>";
    static final String CASE_SEP = "<<<DAPCODE_CASE_SEP_9F3A>>>";
    static final String RES_START_PREFIX = "<<<DAPCODE_RES_START:";
    static final String RES_END_PREFIX = "<<<DAPCODE_RES_END:";
    static final String MARKER_SUFFIX = ">>>";

    private static final Pattern FRAME_PATTERN = Pattern.compile(
            "<<<DAPCODE_RES_START:(\\d+):(OK|ERR):([0-9.eE+-]+)>>>\\r?\\n(.*?)<<<DAPCODE_RES_END:\\1>>>",
            Pattern.DOTALL);

    record CaseOutcome(int index, boolean ok, double runtimeMs, String payload) {
    }

    private BatchExecutionProtocol() {
    }

    static String encodeBatchInput(List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();
        sb.append(BATCH_HEADER).append('\n');
        for (int i = 0; i < testCases.size(); i++) {
            if (i > 0) {
                sb.append('\n').append(CASE_SEP).append('\n');
            }
            String in = testCases.get(i).getInputData();
            if (in != null) {
                sb.append(in.replace("\r\n", "\n"));
            }
        }
        return sb.toString();
    }

    static List<CaseOutcome> parseBatchOutput(String stdout, int expectedCount) {
        if (stdout == null || stdout.isEmpty() || expectedCount <= 0) {
            return null;
        }
        Matcher m = FRAME_PATTERN.matcher(stdout);
        List<CaseOutcome> parsed = new ArrayList<>(expectedCount);
        int expectedIdx = 0;
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            if (idx != expectedIdx) {
                return null;
            }
            boolean ok = "OK".equals(m.group(2));
            double ms = 0.0;
            try {
                ms = Double.parseDouble(m.group(3));
            } catch (NumberFormatException ignored) {
            }
            String payload = m.group(4);
            if (payload.endsWith("\r\n")) {
                payload = payload.substring(0, payload.length() - 2);
            } else if (payload.endsWith("\n")) {
                payload = payload.substring(0, payload.length() - 1);
            }
            parsed.add(new CaseOutcome(idx, ok, ms, payload));
            expectedIdx++;
        }
        return parsed.size() == expectedCount ? parsed : null;
    }
}
