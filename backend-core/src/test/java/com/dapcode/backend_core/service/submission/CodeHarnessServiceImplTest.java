package com.dapcode.backend_core.service.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodeHarnessServiceImplTest {

    private final CodeHarnessServiceImpl harness = new CodeHarnessServiceImpl();

    private String compileAndRunJava(Path tempDir, String slug, String code, String stdin) throws Exception {
        String wrapped = harness.prepareSourceCode(slug, 62, code);
        Path dir = Files.createTempDirectory(tempDir, slug.replaceAll("[^a-zA-Z0-9]", "_"));
        Path mainJava = dir.resolve("Main.java");
        Files.writeString(mainJava, wrapped, StandardCharsets.UTF_8);

        Process compile = new ProcessBuilder("javac", mainJava.toString())
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start();
        String compileOut = readAll(compile.getInputStream());
        assertTrue(compile.waitFor(20, TimeUnit.SECONDS), "javac timed out for " + slug);
        assertEquals(0, compile.exitValue(), "javac failed for " + slug + ":\n" + compileOut);

        Process run = new ProcessBuilder("java", "-cp", dir.toString(), "Main")
                .directory(dir.toFile())
                .redirectErrorStream(false)
                .start();
        try (OutputStream os = run.getOutputStream()) {
            os.write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        String stdout = readAll(run.getInputStream());
        String stderr = readAll(run.getErrorStream());
        assertTrue(run.waitFor(10, TimeUnit.SECONDS), "java timed out for " + slug);
        assertEquals(0, run.exitValue(), "java failed for " + slug + " stderr:\n" + stderr);
        return stdout.trim();
    }

    private String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        is.transferTo(bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Java: Chọn đúng hàm chính kể cả khi có hàm phụ public (dfs, gcd, canShip, isPalindrome, isValid)")
    void testJavaHelperMethodDisambiguation(@TempDir Path tempDir) throws Exception {
        String shipCode = """
                class Solution {
                    public boolean canShip(int[] weights, int days, int cap) {
                        int d = 1, cur = 0;
                        for (int w : weights) {
                            if (cur + w > cap) { d++; cur = 0; }
                            cur += w;
                        }
                        return d <= days;
                    }
                    public int shipWithinDays(int[] weights, int days) {
                        int l = 0, r = 0;
                        for (int w : weights) { l = Math.max(l, w); r += w; }
                        while (l < r) {
                            int m = l + (r - l) / 2;
                            if (canShip(weights, days, m)) r = m;
                            else l = m + 1;
                        }
                        return l;
                    }
                }
                """;
        assertEquals("15", compileAndRunJava(tempDir, "capacity-to-ship-packages-within-d-days",
                shipCode, "weights = [1,2,3,4,5,6,7,8,9,10], days = 5"));

        String gcdCode = """
                class Solution {
                    public int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }
                    public String gcdOfStrings(String str1, String str2) {
                        if (!(str1 + str2).equals(str2 + str1)) return "";
                        return str1.substring(0, gcd(str1.length(), str2.length()));
                    }
                }
                """;
        assertEquals("ABC", compileAndRunJava(tempDir, "greatest-common-divisor-of-strings",
                gcdCode, "str1 = \"ABCABC\", str2 = \"ABC\""));

        String robberCode = """
                class Solution {
                    public int[] dfs(TreeNode root) {
                        if (root == null) return new int[]{0, 0};
                        int[] l = dfs(root.left), r = dfs(root.right);
                        return new int[]{root.val + l[1] + r[1], Math.max(l[0], l[1]) + Math.max(r[0], r[1])};
                    }
                    public int rob(TreeNode root) {
                        int[] res = dfs(root);
                        return Math.max(res[0], res[1]);
                    }
                }
                """;
        assertEquals("7", compileAndRunJava(tempDir, "house-robber-iii",
                robberCode, "root = [3,2,3,null,3,null,1]"));

        String palPartCode = """
                class Solution {
                    public boolean isPalindrome(String s, int l, int r) {
                        while (l < r) if (s.charAt(l++) != s.charAt(r--)) return false;
                        return true;
                    }
                    public List<List<String>> partition(String s) {
                        List<List<String>> res = new ArrayList<>();
                        res.add(List.of(s));
                        return res;
                    }
                }
                """;
        assertEquals("[[a]]", compileAndRunJava(tempDir, "palindrome-partitioning",
                palPartCode, "s = \"a\""));
    }

    @Test
    @DisplayName("Java: Đảo thứ tự tham số (MountainArray), Pair<K,V>, hoisted imports, Codec, Node, LCA, double[], single quotes")
    void testJavaSpecialFeatures(@TempDir Path tempDir) throws Exception {
        String mountainCode = """
                class Solution {
                    public int findInMountainArray(int target, MountainArray mountainArr) {
                        for (int i = 0; i < mountainArr.length(); i++) {
                            if (mountainArr.get(i) == target) return i;
                        }
                        return -1;
                    }
                }
                """;
        assertEquals("2", compileAndRunJava(tempDir, "find-in-mountain-array",
                mountainCode, "mountainArr = [1,2,3,4,5,3,1], target = 3"));

        String pairImportCode = """
                package demo;
                import java.util.ArrayList;
                public class Solution {
                    public int solve(int[] nums) {
                        Pair<Integer, Integer> p = new Pair<>(nums[0], nums.length);
                        return p.getKey() + p.getValue();
                    }
                }
                """;
        assertEquals("13", compileAndRunJava(tempDir, "two-sum-demo",
                pairImportCode, "nums = [10, 20, 30]"));

        String timeMapCode = """
                class TimeMap {
                    Map<String, List<Pair<String, Integer>>> map = new HashMap<>();
                    public TimeMap() {}
                    public void set(String key, String value, int timestamp) {
                        map.computeIfAbsent(key, k -> new ArrayList<>()).add(new Pair<>(value, timestamp));
                    }
                    public String get(String key, int timestamp) {
                        List<Pair<String, Integer>> list = map.get(key);
                        if (list == null) return "";
                        for (int i = list.size() - 1; i >= 0; i--) {
                            if (list.get(i).getValue() <= timestamp) return list.get(i).getKey();
                        }
                        return "";
                    }
                }
                """;
        assertEquals("[null, null, \"bar\", \"bar\"]", compileAndRunJava(tempDir, "time-based-key-value-store",
                timeMapCode, "[\"TimeMap\", \"set\", \"get\", \"get\"]\n[[], [\"foo\", \"bar\", 1], [\"foo\", 1], [\"foo\", 3]]"));

        String codecCode = """
                public class Codec {
                    public String serialize(TreeNode root) {
                        return root == null ? "" : String.valueOf(root.val);
                    }
                    public TreeNode deserialize(String data) {
                        return data.isEmpty() ? null : new TreeNode(Integer.parseInt(data));
                    }
                }
                """;
        assertEquals("[1]", compileAndRunJava(tempDir, "serialize-and-deserialize-binary-tree",
                codecCode, "root = [1]"));

        String cloneGraphCode = """
                class Solution {
                    Map<Node, Node> map = new HashMap<>();
                    public Node cloneGraph(Node node) {
                        if (node == null) return null;
                        if (map.containsKey(node)) return map.get(node);
                        Node copy = new Node(node.val);
                        map.put(node, copy);
                        for (Node nb : node.neighbors) copy.neighbors.add(cloneGraph(nb));
                        return copy;
                    }
                }
                """;
        assertEquals("[[2, 4], [1, 3], [2, 4], [1, 3]]", compileAndRunJava(tempDir, "clone-graph",
                cloneGraphCode, "adjList = [[2,4],[1,3],[2,4],[1,3]]"));

        String randomListCode = """
                class Solution {
                    public Node copyRandomList(Node head) {
                        if (head == null) return null;
                        Map<Node, Node> map = new HashMap<>();
                        for (Node c = head; c != null; c = c.next) map.put(c, new Node(c.val));
                        for (Node c = head; c != null; c = c.next) {
                            map.get(c).next = map.get(c.next);
                            map.get(c).random = map.get(c.random);
                        }
                        return map.get(head);
                    }
                }
                """;
        assertEquals("[[7, null], [13, 0]]", compileAndRunJava(tempDir, "copy-list-with-random-pointer",
                randomListCode, "head = [[7,null],[13,0]]"));

        String lcaCode = """
                class Solution {
                    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
                        if (root == null || root == p || root == q) return root;
                        TreeNode l = lowestCommonAncestor(root.left, p, q);
                        TreeNode r = lowestCommonAncestor(root.right, p, q);
                        if (l != null && r != null) return root;
                        return l != null ? l : r;
                    }
                }
                """;
        assertEquals("6", compileAndRunJava(tempDir, "lowest-common-ancestor-of-a-binary-search-tree",
                lcaCode, "root = [6,2,8,0,4,7,9,null,null,3,5], p = 2, q = 8"));

        String evalDivCode = """
                class Solution {
                    public double[] calcEquation(List<List<String>> equations, double[] values, List<List<String>> queries) {
                        return new double[]{values[0], values[1]};
                    }
                }
                """;
        assertEquals("[1.5, 2.5]", compileAndRunJava(tempDir, "evaluate-division",
                evalDivCode, "equations = [[\"a\",\"b\"]], values = [1.5,2.5], queries = [[\"a\",\"b\"]]"));

        String singleQuoteCode = """
                class Solution {
                    public List<String> findWords(char[][] board, String[] words) {
                        return List.of(words[0] + ":" + board[0][0]);
                    }
                }
                """;
        assertEquals("[oath:o]", compileAndRunJava(tempDir, "word-search-ii",
                singleQuoteCode, "board = [['o','a'],['e','t']], words = ['oath','eat']"));
    }

    @Test
    @DisplayName("C++: Đủ STL headers, bỏ qua private/nested helpers, parse đủ kiểu 2D/uint32/Design/Node/Codec/LCA")
    void testCppDriverGenerationCoverage() {
        String privateHelperCpp = """
                class Solution {
                private:
                    void backtrack(int start, int n, int k, vector<int>& cur, vector<vector<int>>& res) {}
                public:
                    struct UnionFind { int find(int x) { return x; } };
                    vector<vector<int>> combine(int n, int k) { return {}; }
                };
                """;
        String wrappedComb = harness.prepareSourceCode("combinations", 54, privateHelperCpp);
        assertTrue(wrappedComb.contains("#include <climits>"));
        assertTrue(wrappedComb.contains("#include <numeric>"));
        assertTrue(wrappedComb.contains("#include <cstring>"));
        assertTrue(wrappedComb.contains("#include <cstdint>"));
        assertTrue(wrappedComb.contains("sol.combine(arg0, arg1)"), wrappedComb);
        assertFalse(wrappedComb.contains("sol.backtrack("), wrappedComb);
        assertFalse(wrappedComb.contains("sol.find("), wrappedComb);

        String minPathCpp = """
                class Solution {
                public:
                    void Helper(vector<vector<int>>& grid, vector<vector<int>>& dp, int i, int k) {}
                    int minPathSum(vector<vector<int>>& grid) { return 0; }
                };
                """;
        String wrappedMinPath = harness.prepareSourceCode("minimum-path-sum", 54, minPathCpp);
        assertTrue(wrappedMinPath.contains("sol.minPathSum(arg0)"), wrappedMinPath);
        assertTrue(wrappedMinPath.contains("parseVector2DInt"), wrappedMinPath);

        String addBinCpp = """
                class Solution {
                public:
                    string addBinary(string a, string b) { return a + b; }
                };
                """;
        String wrappedAddBin = harness.prepareSourceCode("add-binary", 54, addBinCpp);
        assertFalse(wrappedAddBin.contains("parseStringAfter"), wrappedAddBin);
        assertTrue(wrappedAddBin.contains("getInputVal(kvs, 0, \"a\")"), wrappedAddBin);
        assertTrue(wrappedAddBin.contains("getInputVal(kvs, 1, \"b\")"), wrappedAddBin);

        String interleaveCpp = """
                class Solution {
                public:
                    bool isInterleave(string s1, string s2, string s3) { return true; }
                };
                """;
        String wrappedInter = harness.prepareSourceCode("interleaving-string", 54, interleaveCpp);
        assertTrue(wrappedInter.contains("sol.isInterleave(arg0, arg1, arg2)"), wrappedInter);

        String wordSearchCpp = """
                class Solution {
                public:
                    bool exist(vector<vector<char>>& board, string word) { return false; }
                };
                """;
        String wrappedWs = harness.prepareSourceCode("word-search", 54, wordSearchCpp);
        assertTrue(wrappedWs.contains("parseVector2DChar"), wrappedWs);
        assertTrue(wrappedWs.contains("sol.exist(arg0, arg1)"), wrappedWs);

        String accountsCpp = """
                class Solution {
                public:
                    vector<vector<string>> accountsMerge(vector<vector<string>>& accounts) { return accounts; }
                };
                """;
        String wrappedAcc = harness.prepareSourceCode("accounts-merge", 54, accountsCpp);
        assertTrue(wrappedAcc.contains("parseVector2DString"), wrappedAcc);

        String mergeKCpp = """
                class Solution {
                public:
                    ListNode* mergeKLists(vector<ListNode*>& lists) { return nullptr; }
                };
                """;
        String wrappedMk = harness.prepareSourceCode("merge-k-sorted-lists", 54, mergeKCpp);
        assertTrue(wrappedMk.contains("parseVectorListNode"), wrappedMk);

        String revBitsCpp = """
                class Solution {
                public:
                    uint32_t reverseBits(uint32_t n) { return n; }
                };
                """;
        String wrappedRb = harness.prepareSourceCode("reverse-bits", 54, revBitsCpp);
        assertTrue(wrappedRb.contains("parseUint32Val"), wrappedRb);
        assertTrue(wrappedRb.contains("sol.reverseBits(arg0)"), wrappedRb);

        String ipoCpp = """
                class Solution {
                public:
                    int findMaximizedCapital(int k, int w, vector<int>& profits, vector<int>& capital) { return w; }
                };
                """;
        String wrappedIpo = harness.prepareSourceCode("ipo", 54, ipoCpp);
        assertTrue(wrappedIpo.contains("sol.findMaximizedCapital(arg0, arg1, arg2, arg3)"), wrappedIpo);

        String lruCpp = """
                struct DNode { int k, v; };
                class LRUCache {
                public:
                    LRUCache(int capacity) {}
                    int get(int key) { return -1; }
                    void put(int key, int value) {}
                };
                """;
        String wrappedLru = harness.prepareSourceCode("lru-cache", 54, lruCpp);
        assertTrue(wrappedLru.contains("LRUCache* obj = nullptr;"), wrappedLru);
        assertTrue(wrappedLru.contains("obj = new LRUCache(c0);"), wrappedLru);
        assertTrue(wrappedLru.contains("cmds[i] == \"get\""), wrappedLru);
        assertTrue(wrappedLru.contains("cmds[i] == \"put\""), wrappedLru);
        assertFalse(wrappedLru.contains("Solution sol;"), wrappedLru);

        String kthCpp = """
                class KthLargest {
                public:
                    KthLargest(int k, vector<int>& nums) {}
                    int add(int val) { return val; }
                };
                """;
        String wrappedKth = harness.prepareSourceCode("kth-largest-element-in-a-stream", 54, kthCpp);
        assertTrue(wrappedKth.contains("new KthLargest(c0, c1)"), wrappedKth);
        assertTrue(wrappedKth.contains("parseVectorInt"), wrappedKth);

        for (String slug : List.of("clone-graph", "copy-list-with-random-pointer", "construct-quad-tree")) {
            String wrappedNode = harness.prepareSourceCode(slug, 54, "class Solution { public: Node* f(Node* x) { return x; } };");
            assertTrue(wrappedNode.contains("class Node"), "Missing class Node for " + slug);
        }
        String wrappedCodec = harness.prepareSourceCode("serialize-and-deserialize-binary-tree", 54,
                "class Codec { public: string serialize(TreeNode* r){return \"\";} TreeNode* deserialize(string d){return nullptr;} };");
        assertTrue(wrappedCodec.contains("Codec ser, deser;"), wrappedCodec);
        assertFalse(wrappedCodec.contains("Solution sol;"), wrappedCodec);

        String wrappedLca = harness.prepareSourceCode("lowest-common-ancestor-of-a-binary-search-tree", 54,
                "class Solution { public: TreeNode* lowestCommonAncestor(TreeNode* root, TreeNode* p, TreeNode* q) { return root; } };");
        assertTrue(wrappedLca.contains("findTreeNode(root, pVal)"), wrappedLca);
        assertTrue(wrappedLca.contains("cout << ans->val"), wrappedLca);

        String wrappedEncCodec = harness.prepareSourceCode("encode-and-decode-strings", 54,
                "class Codec { public: string encode(vector<string>& strs){return \"\";} vector<string> decode(string s){return {};} };");
        assertTrue(wrappedEncCodec.contains("Codec sol;"), wrappedEncCodec);
    }

    @Test
    @DisplayName("Python 3.8: Polyfill cache, pairwise, bisect_left(key=...) đều được nhúng hợp lệ và chạy chuẩn")
    void testPythonPolyfills(@TempDir Path tempDir) throws Exception {
        String pyCode = """
                class Solution:
                    def testPoly(self, nums: list[int]) -> int:
                        @cache
                        def fib(k):
                            return k if k <= 1 else fib(k - 1) + fib(k - 2)
                        diffs = sum(b - a for a, b in pairwise(nums))
                        idx = bisect_left([(x, -x) for x in nums], 3, key=lambda pair: pair[0])
                        return fib(6) + diffs + idx
                """;
        String wrapped = harness.prepareSourceCode("python-polyfill-test", 71, pyCode);
        assertTrue(wrapped.contains("cache = lru_cache(maxsize=None)"));
        assertTrue(wrapped.contains("def pairwise(iterable):"));
        assertTrue(wrapped.contains("_make_key_bisect"));

        Path script = tempDir.resolve("test_py.py");
        Files.writeString(script, wrapped, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("python", script.toString()).start();
        try (OutputStream os = p.getOutputStream()) {
            os.write("nums = [1, 2, 3, 5]".getBytes(StandardCharsets.UTF_8));
        }
        String out = readAll(p.getInputStream()).trim();
        String err = readAll(p.getErrorStream()).trim();
        assertTrue(p.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, p.exitValue(), "Python failed: " + err);
        assertEquals("14", out);
    }
}
