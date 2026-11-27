package com.dapcode.backend_core.service.submission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

final class SubmissionOutputValidator {

    private static final Set<String> EMPTY_EQUIVALENTS = Set.of(
            "", "[]", "[[]]", "none", "null", "n/a", "\"\"", "''");

    private static final Set<String> UNORDERED_1D_SLUGS = Set.of(
            "two-sum",
            "majority-element-ii",
            "top-k-frequent-elements",
            "minimum-height-trees",
            "find-all-anagrams-in-a-string",
            "intersection-of-two-arrays",
            "word-break-ii",
            "word-search-ii",
            "letter-combinations-of-a-phone-number",
            "generate-parentheses",
            "restore-ip-addresses");

    private SubmissionOutputValidator() {
    }

    static boolean isOutputMatched(String actual, String expected) {
        return isOutputMatched(null, null, actual, expected);
    }

    static boolean isOutputMatched(String problemSlug, String inputData, String actual, String expected) {
        if (isStandardMatch(actual, expected)) {
            return true;
        }
        if (problemSlug == null || problemSlug.isBlank()) {
            return false;
        }

        String slug = problemSlug.toLowerCase().trim();

        if (UNORDERED_1D_SLUGS.contains(slug) && isUnordered1DMatch(actual, expected)) {
            return true;
        }

        if (inputData == null || inputData.isBlank()) {
            return false;
        }

        return switch (slug) {
            case "reorganize-string" -> validateReorganizeString(inputData, actual, expected);
            case "longest-happy-string" -> validateLongestHappyString(inputData, actual, expected);
            case "longest-palindromic-substring" -> validateLongestPalindrome(inputData, actual, expected);
            case "course-schedule-ii" -> validateCourseScheduleII(inputData, actual, expected);
            case "delete-node-in-a-bst" -> validateBstModification(inputData, actual, false);
            case "insert-into-a-binary-search-tree" -> validateBstModification(inputData, actual, true);
            case "alien-dictionary" -> validateAlienDictionary(inputData, actual, expected);
            default -> false;
        };
    }

    private static boolean isStandardMatch(String actual, String expected) {
        String a = actual == null ? "" : actual.trim().replace("\r\n", "\n");
        String e = expected == null ? "" : expected.trim().replace("\r\n", "\n");
        if (a.equals(e)) {
            return true;
        }
        String aNorm = a.replaceAll("\\s+", "").toLowerCase();
        String eNorm = e.replaceAll("\\s+", "").toLowerCase();
        if (aNorm.equals(eNorm)) {
            return true;
        }
        if (EMPTY_EQUIVALENTS.contains(aNorm) && EMPTY_EQUIVALENTS.contains(eNorm)) {
            return true;
        }
        aNorm = aNorm.replace("none", "null").replace("(", "[").replace(")", "]").replace(",]", "]");
        eNorm = eNorm.replace("none", "null").replace("(", "[").replace(")", "]").replace(",]", "]");
        if (aNorm.equals(eNorm)) {
            return true;
        }
        String aUnquoted = aNorm.replaceAll("[\"'`]", "");
        String eUnquoted = eNorm.replaceAll("[\"'`]", "");
        if (aUnquoted.equals(eUnquoted)) {
            return true;
        }
        try {
            double da = Double.parseDouble(aUnquoted);
            double de = Double.parseDouble(eUnquoted);
            if (Math.abs(da - de) < 1e-5) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        if (aUnquoted.startsWith("[[") && aUnquoted.endsWith("]]")
                && eUnquoted.startsWith("[[") && eUnquoted.endsWith("]]")) {
            if (canonicalizeNestedArray(aUnquoted).equals(canonicalizeNestedArray(eUnquoted))) {
                return true;
            }
        }
        if (aUnquoted.startsWith("[") && aUnquoted.endsWith("]")
                && eUnquoted.startsWith("[") && eUnquoted.endsWith("]")
                && !aUnquoted.startsWith("[[") && !eUnquoted.startsWith("[[")) {
            String[] aItems = aUnquoted.substring(1, aUnquoted.length() - 1).split(",");
            String[] eItems = eUnquoted.substring(1, eUnquoted.length() - 1).split(",");
            if (aItems.length == eItems.length && aItems.length > 0) {
                boolean allFloatEqual = true;
                for (int i = 0; i < aItems.length; i++) {
                    try {
                        if (Math.abs(Double.parseDouble(aItems[i]) - Double.parseDouble(eItems[i])) >= 1e-5) {
                            allFloatEqual = false;
                            break;
                        }
                    } catch (NumberFormatException ex) {
                        allFloatEqual = false;
                        break;
                    }
                }
                if (allFloatEqual) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String canonicalizeNestedArray(String s) {
        String inner = s.substring(1, s.length() - 1);
        List<String> groups = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '[') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && start != -1) {
                    String groupContent = inner.substring(start + 1, i);
                    String[] items = groupContent.isEmpty() ? new String[0] : groupContent.split(",");
                    Arrays.sort(items);
                    groups.add("[" + String.join(",", items) + "]");
                    start = -1;
                }
            }
        }
        Collections.sort(groups);
        return "[" + String.join(",", groups) + "]";
    }

    private static boolean isUnordered1DMatch(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        String a = actual.trim();
        String e = expected.trim();
        if (!a.startsWith("[") || !a.endsWith("]") || !e.startsWith("[") || !e.endsWith("]")) {
            return false;
        }
        if (a.startsWith("[[") || e.startsWith("[[")) {
            return false;
        }
        List<String> aTokens = parseArrayTokens(a);
        List<String> eTokens = parseArrayTokens(e);
        if (aTokens.size() != eTokens.size()) {
            return false;
        }
        List<String> aNorm = new ArrayList<>(aTokens.size());
        List<String> eNorm = new ArrayList<>(eTokens.size());
        for (String t : aTokens) aNorm.add(unquote(t));
        for (String t : eTokens) eNorm.add(unquote(t));
        Collections.sort(aNorm);
        Collections.sort(eNorm);
        return aNorm.equals(eNorm);
    }

    private static boolean validateReorganizeString(String inputData, String actual, String expected) {
        String exp = unquote(expected == null ? "" : expected.trim());
        String act = unquote(actual == null ? "" : actual.trim());
        if (exp.isEmpty()) {
            return act.isEmpty();
        }
        Map<String, String> kvs = parseInputMap(inputData);
        String s = unquote(kvs.getOrDefault("s", firstVal(kvs)));
        if (act.length() != s.length()) {
            return false;
        }
        for (int i = 1; i < act.length(); i++) {
            if (act.charAt(i) == act.charAt(i - 1)) {
                return false;
            }
        }
        char[] aChars = act.toCharArray();
        char[] sChars = s.toCharArray();
        Arrays.sort(aChars);
        Arrays.sort(sChars);
        return Arrays.equals(aChars, sChars);
    }

    private static boolean validateLongestHappyString(String inputData, String actual, String expected) {
        String exp = unquote(expected == null ? "" : expected.trim());
        String act = unquote(actual == null ? "" : actual.trim());
        if (act.length() != exp.length()) {
            return false;
        }
        if (act.contains("aaa") || act.contains("bbb") || act.contains("ccc")) {
            return false;
        }
        Map<String, String> kvs = parseInputMap(inputData);
        int maxA = parseIntSafe(kvs.getOrDefault("a", "0"));
        int maxB = parseIntSafe(kvs.getOrDefault("b", "0"));
        int maxC = parseIntSafe(kvs.getOrDefault("c", "0"));
        int ca = 0, cb = 0, cc = 0;
        for (int i = 0; i < act.length(); i++) {
            char ch = act.charAt(i);
            if (ch == 'a') ca++;
            else if (ch == 'b') cb++;
            else if (ch == 'c') cc++;
            else return false;
        }
        return ca <= maxA && cb <= maxB && cc <= maxC;
    }

    private static boolean validateLongestPalindrome(String inputData, String actual, String expected) {
        String exp = unquote(expected == null ? "" : expected.trim());
        String act = unquote(actual == null ? "" : actual.trim());
        if (act.length() != exp.length()) {
            return false;
        }
        for (int l = 0, r = act.length() - 1; l < r; l++, r--) {
            if (act.charAt(l) != act.charAt(r)) {
                return false;
            }
        }
        Map<String, String> kvs = parseInputMap(inputData);
        String s = unquote(kvs.getOrDefault("s", firstVal(kvs)));
        return s.contains(act);
    }

    private static boolean validateCourseScheduleII(String inputData, String actual, String expected) {
        String expNorm = expected == null ? "" : expected.replaceAll("\\s+", "");
        String actNorm = actual == null ? "" : actual.replaceAll("\\s+", "");
        if ("[]".equals(expNorm)) {
            return "[]".equals(actNorm);
        }
        Map<String, String> kvs = parseInputMap(inputData);
        int numCourses = parseIntSafe(kvs.getOrDefault("numCourses", "0"));
        if (numCourses <= 0) {
            return false;
        }
        List<Integer> order = parseIntList(actual);
        if (order == null || order.size() != numCourses) {
            return false;
        }
        int[] pos = new int[numCourses];
        Arrays.fill(pos, -1);
        for (int i = 0; i < order.size(); i++) {
            int c = order.get(i);
            if (c < 0 || c >= numCourses || pos[c] != -1) {
                return false;
            }
            pos[c] = i;
        }
        String prereqRaw = kvs.getOrDefault("prerequisites", "[]");
        for (String pairStr : parseArrayTokens(prereqRaw)) {
            List<Integer> pair = parseIntList(pairStr);
            if (pair != null && pair.size() == 2) {
                int course = pair.get(0);
                int pre = pair.get(1);
                if (course >= 0 && course < numCourses && pre >= 0 && pre < numCourses) {
                    if (pos[pre] >= pos[course]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static final class SimpleTreeNode {
        final int val;
        SimpleTreeNode left;
        SimpleTreeNode right;

        SimpleTreeNode(int val) {
            this.val = val;
        }
    }

    private static boolean validateBstModification(String inputData, String actual, boolean isInsert) {
        Map<String, String> kvs = parseInputMap(inputData);
        String rootRaw = kvs.getOrDefault("root", "[]");
        int targetVal = parseIntSafe(kvs.getOrDefault(isInsert ? "val" : "key", "0"));

        List<Integer> inputVals = extractNonNullTreeValues(rootRaw);
        if (inputVals == null) {
            return false;
        }
        List<Integer> expectedVals = new ArrayList<>(inputVals);
        if (isInsert) {
            expectedVals.add(targetVal);
        } else {
            expectedVals.remove(Integer.valueOf(targetVal));
        }
        Collections.sort(expectedVals);

        String actTrim = actual == null ? "" : actual.trim();
        String actLower = actTrim.replaceAll("\\s+", "").toLowerCase();
        if (EMPTY_EQUIVALENTS.contains(actLower)) {
            return expectedVals.isEmpty();
        }

        SimpleTreeNode root = buildTreeFromLevelOrder(actTrim);
        if (root == null) {
            return false;
        }

        List<Integer> actualVals = new ArrayList<>();
        if (!isValidBstAndCollect(root, Long.MIN_VALUE, Long.MAX_VALUE, actualVals)) {
            return false;
        }
        Collections.sort(actualVals);
        return actualVals.equals(expectedVals);
    }

    private static SimpleTreeNode buildTreeFromLevelOrder(String raw) {
        if (!raw.startsWith("[") || !raw.endsWith("]")) {
            return null;
        }
        List<String> tokens = parseArrayTokens(raw);
        if (tokens.isEmpty()) {
            return null;
        }
        String first = tokens.get(0).trim().toLowerCase();
        if ("null".equals(first) || "none".equals(first)) {
            return null;
        }
        Integer rootVal = parseStrictInt(tokens.get(0));
        if (rootVal == null) {
            return null;
        }
        SimpleTreeNode root = new SimpleTreeNode(rootVal);
        Queue<SimpleTreeNode> q = new LinkedList<>();
        q.offer(root);
        int idx = 1;
        while (!q.isEmpty() && idx < tokens.size()) {
            SimpleTreeNode curr = q.poll();
            if (idx < tokens.size()) {
                String t = tokens.get(idx++).trim();
                if (!t.equalsIgnoreCase("null") && !t.equalsIgnoreCase("none")) {
                    Integer v = parseStrictInt(t);
                    if (v == null) return null;
                    curr.left = new SimpleTreeNode(v);
                    q.offer(curr.left);
                }
            }
            if (idx < tokens.size()) {
                String t = tokens.get(idx++).trim();
                if (!t.equalsIgnoreCase("null") && !t.equalsIgnoreCase("none")) {
                    Integer v = parseStrictInt(t);
                    if (v == null) return null;
                    curr.right = new SimpleTreeNode(v);
                    q.offer(curr.right);
                }
            }
        }
        while (idx < tokens.size()) {
            String t = tokens.get(idx++).trim();
            if (!t.equalsIgnoreCase("null") && !t.equalsIgnoreCase("none")) {
                return null;
            }
        }
        return root;
    }

    private static boolean isValidBstAndCollect(SimpleTreeNode node, long min, long max, List<Integer> out) {
        if (node == null) {
            return true;
        }
        if (node.val <= min || node.val >= max) {
            return false;
        }
        out.add(node.val);
        return isValidBstAndCollect(node.left, min, node.val, out)
                && isValidBstAndCollect(node.right, node.val, max, out);
    }

    private static List<Integer> extractNonNullTreeValues(String raw) {
        List<Integer> vals = new ArrayList<>();
        for (String tok : parseArrayTokens(raw)) {
            String t = tok.trim();
            if (t.isEmpty() || t.equalsIgnoreCase("null") || t.equalsIgnoreCase("none")) {
                continue;
            }
            Integer v = parseStrictInt(t);
            if (v == null) {
                return null;
            }
            vals.add(v);
        }
        return vals;
    }

    private static boolean validateAlienDictionary(String inputData, String actual, String expected) {
        String exp = unquote(expected == null ? "" : expected.trim());
        String act = unquote(actual == null ? "" : actual.trim());
        if (exp.isEmpty()) {
            return act.isEmpty();
        }
        Map<String, String> kvs = parseInputMap(inputData);
        List<String> rawWords = parseArrayTokens(kvs.getOrDefault("words", firstVal(kvs)));
        List<String> words = new ArrayList<>(rawWords.size());
        Set<Character> allChars = new HashSet<>();
        for (String rw : rawWords) {
            String w = unquote(rw);
            words.add(w);
            for (int i = 0; i < w.length(); i++) {
                allChars.add(w.charAt(i));
            }
        }
        if (act.length() != allChars.size()) {
            return false;
        }
        Map<Character, Integer> rank = new HashMap<>();
        for (int i = 0; i < act.length(); i++) {
            char ch = act.charAt(i);
            if (!allChars.contains(ch) || rank.put(ch, i) != null) {
                return false;
            }
        }
        for (int i = 0; i + 1 < words.size(); i++) {
            String w1 = words.get(i);
            String w2 = words.get(i + 1);
            int minLen = Math.min(w1.length(), w2.length());
            boolean foundDiff = false;
            for (int j = 0; j < minLen; j++) {
                if (w1.charAt(j) != w2.charAt(j)) {
                    if (rank.get(w1.charAt(j)) > rank.get(w2.charAt(j))) {
                        return false;
                    }
                    foundDiff = true;
                    break;
                }
            }
            if (!foundDiff && w1.length() > w2.length()) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> parseInputMap(String inputData) {
        Map<String, String> map = new LinkedHashMap<>();
        if (inputData == null || inputData.isBlank()) {
            return map;
        }
        String[] parts = inputData.trim().split("(?:,|\\r?\\n)\\s*(?=[a-zA-Z_][a-zA-Z0-9_]*\\s*=)");
        for (String p : parts) {
            int eq = p.indexOf('=');
            if (eq != -1) {
                map.put(p.substring(0, eq).trim(), p.substring(eq + 1).trim());
            }
        }
        return map;
    }

    private static String firstVal(Map<String, String> map) {
        return map.isEmpty() ? "" : map.values().iterator().next();
    }

    private static List<String> parseArrayTokens(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) return list;
        s = s.trim();
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start == -1 || end == -1 || start >= end) return list;
        String inner = s.substring(start + 1, end).trim();
        if (inner.isEmpty()) return list;

        int depth = 0;
        char quote = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (quote != 0) {
                if (c == '\\' && i + 1 < inner.length()) {
                    cur.append(c).append(inner.charAt(++i));
                    continue;
                }
                if (c == quote) quote = 0;
                cur.append(c);
            } else if (c == '"' || c == '\'') {
                quote = c;
                cur.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                cur.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                cur.append(c);
            } else if (c == ',' && depth == 0) {
                if (cur.length() > 0) {
                    list.add(cur.toString().trim());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            list.add(cur.toString().trim());
        }
        return list;
    }

    private static List<Integer> parseIntList(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null;
        List<String> toks = parseArrayTokens(trimmed);
        List<Integer> res = new ArrayList<>(toks.size());
        for (String t : toks) {
            Integer v = parseStrictInt(t);
            if (v == null) return null;
            res.add(v);
        }
        return res;
    }

    private static Integer parseStrictInt(String s) {
        if (s == null) return null;
        String t = unquote(s).trim();
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseIntSafe(String s) {
        Integer v = parseStrictInt(s);
        return v != null ? v : 0;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
