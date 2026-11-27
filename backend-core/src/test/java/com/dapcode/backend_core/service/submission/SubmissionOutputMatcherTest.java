package com.dapcode.backend_core.service.submission;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SubmissionOutputMatcherTest {

    private SubmissionServiceImpl submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionServiceImpl(
                null, null, null, null, null, mock(SubmissionStreamManager.class), null);
    }

    private boolean match(String actual, String expected) {
        return Boolean.TRUE.equals(
                ReflectionTestUtils.invokeMethod(submissionService, "isOutputMatched", actual, expected));
    }

    @Test
    @DisplayName("1. Chấm điểm: Khớp chính xác khoảng trắng, giá trị rỗng, số thực (1e-5) và mảng 2D không thứ tự")
    void testIsOutputMatchedEdgeCases() {
        assertTrue(match(" [1, 2, 3]\r\n", "[1,2,3]\n"));
        assertTrue(match("None", "[]"));
        assertTrue(match("null", ""));
        assertTrue(match("3.141592", "3.141598"));
        assertTrue(match("[[1,2],[3,4]]", "[[4,3],[2,1]]"));
        assertFalse(match("[1, 2]", "[1, 3]"));
    }

    @Test
    @DisplayName("2. Nhận diện ngôn ngữ: Tự động phân biệt Python (71), C++ (54) và Java (62)")
    void testResolveLanguageId() {
        int pyId = ReflectionTestUtils.invokeMethod(
                submissionService, "resolveLanguageId", 71, "class Solution:\n    def twoSum(self, nums, target):");
        int cppId = ReflectionTestUtils.invokeMethod(
                submissionService, "resolveLanguageId", 71, "#include <vector>\nclass Solution { public: };");
        int javaId = ReflectionTestUtils.invokeMethod(
                submissionService, "resolveLanguageId", 71, "public class Solution { public int[] twoSum() {} }");

        assertEquals(71, pyId);
        assertEquals(54, cppId);
        assertEquals(62, javaId);
    }

    @Test
    @DisplayName("3. Mảng 1D không phân biệt thứ tự chỉ áp dụng cho bài cho phép bất kỳ thứ tự nào")
    void testUnordered1DArrays() {
        assertTrue(SubmissionOutputValidator.isOutputMatched("majority-element-ii", "nums = [1,2]", "[2, 1]", "[1, 2]"));
        assertTrue(SubmissionOutputValidator.isOutputMatched("top-k-frequent-elements", "nums = [1,1,2,2], k = 2", "[2, 1]", "[1, 2]"));
        assertTrue(SubmissionOutputValidator.isOutputMatched("word-search-ii", "", "[\"eat\", \"oath\"]", "['oath', 'eat']"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("sort-colors", "nums = [2,0,1]", "[1, 0, 2]", "[0, 1, 2]"));
    }

    @Test
    @DisplayName("4. Validator chuyên biệt: reorganize-string, longest-happy-string, longest-palindromic-substring")
    void testStringSemanticValidators() {
        assertTrue(SubmissionOutputValidator.isOutputMatched("reorganize-string", "s = \"aaabc\"", "acaba", "abaca"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("reorganize-string", "s = \"aaabc\"", "aaabc", "abaca"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("reorganize-string", "s = \"aaabc\"", "acabd", "abaca"));
        assertTrue(SubmissionOutputValidator.isOutputMatched("reorganize-string", "s = \"aaab\"", "", ""));
        assertFalse(SubmissionOutputValidator.isOutputMatched("reorganize-string", "s = \"aaab\"", "aba", ""));

        assertTrue(SubmissionOutputValidator.isOutputMatched("longest-happy-string", "a = 1, b = 1, c = 7", "ccbccacc", "ccaccbcc"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("longest-happy-string", "a = 1, b = 1, c = 7", "cccacbcc", "ccaccbcc"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("longest-happy-string", "a = 1, b = 1, c = 7", "ccaccbc", "ccaccbcc"));

        assertTrue(SubmissionOutputValidator.isOutputMatched("longest-palindromic-substring", "s = \"babad\"", "bab", "aba"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("longest-palindromic-substring", "s = \"babad\"", "bbb", "aba"));
        assertFalse(SubmissionOutputValidator.isOutputMatched("longest-palindromic-substring", "s = \"babad\"", "bad", "aba"));
    }

    @Test
    @DisplayName("5. Validator chuyên biệt: course-schedule-ii, delete-node-in-a-bst, insert-into-a-bst, alien-dictionary")
    void testGraphAndBstSemanticValidators() {
        assertTrue(SubmissionOutputValidator.isOutputMatched(
                "course-schedule-ii", "numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]",
                "[0, 2, 1, 3]", "[0, 1, 2, 3]"));
        assertFalse(SubmissionOutputValidator.isOutputMatched(
                "course-schedule-ii", "numCourses = 4, prerequisites = [[1,0],[2,0],[3,1],[3,2]]",
                "[1, 0, 2, 3]", "[0, 1, 2, 3]"));

        assertTrue(SubmissionOutputValidator.isOutputMatched(
                "delete-node-in-a-bst", "root = [50,30,70,null,40,60,80], key = 50",
                "[60, 30, 70, null, 40, null, 80]", "[70, 60, 80, 30, null, null, null, null, 40]"));
        assertFalse(SubmissionOutputValidator.isOutputMatched(
                "delete-node-in-a-bst", "root = [50,30,70,null,40,60,80], key = 50",
                "[60, 70, 30, null, 40, null, 80]", "[70, 60, 80, 30, null, null, null, null, 40]"));

        assertTrue(SubmissionOutputValidator.isOutputMatched(
                "insert-into-a-binary-search-tree", "root = [4,2,7,1,3], val = 5",
                "[5, 2, 7, 1, 3, null, null, null, null, null, 4]", "[4, 2, 7, 1, 3, 5]"));

        assertTrue(SubmissionOutputValidator.isOutputMatched(
                "alien-dictionary", "words = [\"z\",\"x\",\"a\",\"zb\",\"zx\"]",
                "", ""));
        assertTrue(SubmissionOutputValidator.isOutputMatched(
                "alien-dictionary", "words = [\"ab\",\"adc\"]",
                "abcd", "cbda"));
        assertFalse(SubmissionOutputValidator.isOutputMatched(
                "alien-dictionary", "words = [\"ab\",\"adc\"]",
                "adbc", "cbda"));
    }
}
