package com.dapcode.backend_core.service.submission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CodeHarnessServiceImpl implements CodeHarnessService {

    public static final Map<String, String> DESIGN_CLASS_NAMES = Map.ofEntries(
            Map.entry("design-hashset", "MyHashSet"),
            Map.entry("design-hashmap", "MyHashMap"),
            Map.entry("min-stack", "MinStack"),
            Map.entry("implement-queue-using-stacks", "MyQueue"),
            Map.entry("implement-stack-using-queues", "MyStack"),
            Map.entry("design-circular-queue", "MyCircularQueue"),
            Map.entry("online-stock-span", "StockSpanner"),
            Map.entry("implement-trie-prefix-tree", "Trie"),
            Map.entry("design-add-and-search-words-data-structure", "WordDictionary"),
            Map.entry("lru-cache", "LRUCache"),
            Map.entry("time-based-key-value-store", "TimeMap"),
            Map.entry("design-twitter", "Twitter"),
            Map.entry("find-median-from-data-stream", "MedianFinder"),
            Map.entry("range-sum-query-2d-immutable", "NumMatrix"),
            Map.entry("maximum-frequency-stack", "FreqStack"),
            Map.entry("detect-squares", "DetectSquares"),
            Map.entry("lfu-cache", "LFUCache"),
            Map.entry("kth-largest-element-in-a-stream", "KthLargest"));

    public static final Set<String> DESIGN_SLUGS = DESIGN_CLASS_NAMES.keySet();

    private static final Set<String> HELPER_METHOD_NAMES = Set.of(
            "dfs", "bfs", "backtrack", "backtracking", "helper", "dfshelper", "bfshelper",
            "solvehelper", "fun", "prec", "swap", "gcd", "canship", "sumofsquares",
            "validpalindromeutil", "quicksort", "mergesort", "heapify", "findparent",
            "union", "findroot", "getroot", "memo", "rec", "traverse", "inorder",
            "preorder", "postorder", "print", "debug", "compare", "cmp", "hash",
            "check", "isvalidpos", "ispal", "expandaroundcenter", "expand");

    private static final Set<String> CPP_TYPE_KEYWORDS = Set.of(
            "int", "bool", "char", "double", "float", "long", "short", "unsigned", "signed",
            "void", "string", "uint32_t", "int64_t", "uint64_t", "size_t", "const");

    private record ParsedParam(String type, String name) {
    }

    private record ParsedMethod(String returnType, String name, List<ParsedParam> params, boolean isConstructor) {
    }

    private String stripComments(String code) {
        if (code == null) {
            return "";
        }
        return code.replaceAll("(?s)/\\*.*?\\*/", "")
                   .replaceAll("(?m)//.*$", "")
                   .replaceAll("(?m)#.*$", "");
    }

    private String stripCommentsKeepStringsClean(String code) {
        if (code == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(code.length());
        int n = code.length();
        int i = 0;
        while (i < n) {
            char c = code.charAt(i);
            if (c == '/' && i + 1 < n && code.charAt(i + 1) == '/') {
                while (i < n && code.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < n && code.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                i += 2;
                sb.append(' ');
            } else if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(quote);
                i++;
                while (i < n) {
                    char sc = code.charAt(i);
                    if (sc == '\\' && i + 1 < n) {
                        sb.append("  ");
                        i += 2;
                        continue;
                    }
                    if (sc == quote) {
                        sb.append(quote);
                        i++;
                        break;
                    }
                    sb.append(' ');
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private boolean isDesignProblem(String slug, String code) {
        if (slug != null) {
            String clean = slug.toLowerCase().trim();
            if ("serialize-and-deserialize-binary-tree".equals(clean)
                    || "encode-and-decode-strings".equals(clean)) {
                return false;
            }
            if (DESIGN_SLUGS.contains(clean)) {
                return true;
            }
        }
        return !Pattern.compile("\\bclass\\s+(?:Solution|Codec)\\b").matcher(stripComments(code)).find();
    }

    @Override
    public String prepareSourceCode(String slug, int languageId, String rawSourceCode) {
        if (rawSourceCode == null || rawSourceCode.isBlank()) {
            return rawSourceCode;
        }

        return switch (languageId) {
            case 71 -> wrapPython(slug, rawSourceCode);
            case 54 -> wrapCpp(slug, rawSourceCode);
            case 62 -> wrapJava(slug, rawSourceCode);
            default -> rawSourceCode;
        };
    }

    @Override
    public boolean supportsBatchExecution(int languageId, String rawSourceCode) {
        if (rawSourceCode == null || rawSourceCode.isBlank()) {
            return false;
        }
        return switch (languageId) {
            case 71 -> !rawSourceCode.contains("__main__");
            case 54 -> !rawSourceCode.contains("int main");
            case 62 -> !rawSourceCode.contains("public static void main");
            default -> false;
        };
    }

    private String wrapPython(String slug, String code) {
        if (code.contains("__main__")) {
            return code;
        }
        String cleanSlug = slug != null ? slug.toLowerCase().trim() : "";
        String uncommented = stripComments(code);

        String prefix = """
from __future__ import annotations
from typing import *
from functools import *
from itertools import *
from collections import *
from bisect import *
from math import *
from heapq import *
from operator import *
import collections
import heapq
import math
import json
import sys
import re
import random
import bisect as _bisect_mod
from random import randint

try:
    cache
except NameError:
    cache = lru_cache(maxsize=None)

try:
    pairwise
except NameError:
    def pairwise(iterable):
        a, b = tee(iterable)
        next(b, None)
        return zip(a, b)

try:
    _bisect_mod.bisect_left([1], 1, key=lambda v: v)
except TypeError:
    def _make_key_bisect(_orig_fn, _is_left):
        def _wrapped(a, x, lo=0, hi=None, *, key=None):
            if hi is None:
                hi = len(a)
            if key is None:
                return _orig_fn(a, x, lo, hi)
            while lo < hi:
                mid = (lo + hi) // 2
                mv = key(a[mid])
                if (_is_left and mv < x) or ((not _is_left) and not (x < mv)):
                    lo = mid + 1
                else:
                    hi = mid
            return lo
        return _wrapped
    bisect_left = _make_key_bisect(_bisect_mod.bisect_left, True)
    bisect_right = _make_key_bisect(_bisect_mod.bisect_right, False)
    bisect = bisect_right
    _bisect_mod.bisect_left = bisect_left
    _bisect_mod.bisect_right = bisect_right
    _bisect_mod.bisect = bisect_right

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val
        self.next = next

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right

class MountainArray:
    def __init__(self, arr):
        self._arr = list(arr) if isinstance(arr, list) else []
    def get(self, index: int) -> int:
        return self._arr[index]
    def length(self) -> int:
        return len(self._arr)

_GUESS_PICK = 0
def guess(num: int) -> int:
    if num > _GUESS_PICK:
        return -1
    if num < _GUESS_PICK:
        return 1
    return 0

def _to_list_node(arr, pos=-1):
    if not isinstance(arr, list):
        return arr
    dummy = ListNode(0)
    curr = dummy
    nodes = []
    for x in arr:
        curr.next = ListNode(x)
        curr = curr.next
        nodes.append(curr)
    if isinstance(pos, int) and 0 <= pos < len(nodes) and nodes:
        nodes[-1].next = nodes[pos]
    return dummy.next

def _from_list_node(node):
    res = []
    visited = set()
    while node is not None and id(node) not in visited:
        visited.add(id(node))
        res.append(node.val)
        node = node.next
    return res

def _to_tree_node(arr):
    if isinstance(arr, int):
        return TreeNode(arr)
    if not isinstance(arr, list) or not arr or arr[0] is None:
        return None
    root = TreeNode(arr[0])
    q = collections.deque([root])
    i = 1
    while q and i < len(arr):
        curr = q.popleft()
        if i < len(arr):
            if arr[i] is not None:
                curr.left = TreeNode(arr[i])
                q.append(curr.left)
            i += 1
        if i < len(arr):
            if arr[i] is not None:
                curr.right = TreeNode(arr[i])
                q.append(curr.right)
            i += 1
    return root

def _find_tree_node(root, val):
    if root is None:
        return None
    if root.val == val:
        return root
    left = _find_tree_node(root.left, val)
    return left if left is not None else _find_tree_node(root.right, val)

def _from_tree_node(root):
    if root is None:
        return []
    res = []
    q = collections.deque([root])
    while q:
        curr = q.popleft()
        if curr is None:
            res.append(None)
        else:
            res.append(curr.val)
            q.append(curr.left)
            q.append(curr.right)
    while res and res[-1] is None:
        res.pop()
    return res

def _to_graph_node(adj, cls):
    if not isinstance(adj, list) or len(adj) == 0:
        return None
    nodes = [cls(i + 1) for i in range(len(adj))]
    for i, neighbors in enumerate(adj):
        nodes[i].neighbors = [nodes[nb - 1] for nb in neighbors if 1 <= nb <= len(adj)]
    return nodes[0]

def _from_graph_node(node):
    if node is None:
        return []
    visited = {}
    q = collections.deque([node])
    while q:
        curr = q.popleft()
        if curr.val in visited:
            continue
        visited[curr.val] = [nb.val for nb in curr.neighbors]
        for nb in curr.neighbors:
            if nb.val not in visited:
                q.append(nb)
    if not visited:
        return []
    mx = max(visited.keys())
    return [visited.get(i, []) for i in range(1, mx + 1)]

def _to_random_list(arr, cls):
    if not isinstance(arr, list) or not arr:
        return None
    nodes = [cls(int(item[0])) for item in arr]
    for i in range(len(nodes) - 1):
        nodes[i].next = nodes[i + 1]
    for i, item in enumerate(arr):
        r_idx = item[1] if len(item) > 1 else None
        if r_idx is not None and 0 <= r_idx < len(nodes):
            nodes[i].random = nodes[r_idx]
    return nodes[0]

def _from_random_list(head):
    if head is None:
        return []
    nodes = []
    idx_map = {}
    curr = head
    i = 0
    while curr is not None and id(curr) not in idx_map:
        idx_map[id(curr)] = i
        nodes.append(curr)
        curr = curr.next
        i += 1
    res = []
    for n in nodes:
        r_idx = idx_map.get(id(n.random), None) if n.random is not None else None
        res.append([n.val, r_idx])
    return res

def _from_quad_tree(root):
    if root is None:
        return []
    res = []
    q = collections.deque([root])
    while q:
        curr = q.popleft()
        if curr is None:
            res.append(None)
        else:
            is_leaf = 1 if curr.isLeaf else 0
            val = (1 if curr.val else 0) if curr.isLeaf else 1
            res.append([is_leaf, val])
            if not curr.isLeaf:
                q.append(curr.topLeft)
                q.append(curr.topRight)
                q.append(curr.bottomLeft)
                q.append(curr.bottomRight)
            else:
                q.extend([None, None, None, None])
    while res and res[-1] is None:
        res.pop()
    return res

""";

        StringBuilder nodeDef = new StringBuilder();
        if (!uncommented.contains("class Node")) {
            if ("clone-graph".equals(cleanSlug)) {
                nodeDef.append("""
class Node:
    def __init__(self, val = 0, neighbors = None):
        self.val = val
        self.neighbors = neighbors if neighbors is not None else []
""");
            } else if ("copy-list-with-random-pointer".equals(cleanSlug)) {
                nodeDef.append("""
class Node:
    def __init__(self, x: int, next: 'Node' = None, random: 'Node' = None):
        self.val = int(x)
        self.next = next
        self.random = random
""");
            } else if ("construct-quad-tree".equals(cleanSlug)) {
                nodeDef.append("""
class Node:
    def __init__(self, val=False, isLeaf=False, topLeft=None, topRight=None, bottomLeft=None, bottomRight=None):
        self.val = val
        self.isLeaf = isLeaf
        self.topLeft = topLeft
        self.topRight = topRight
        self.bottomLeft = bottomLeft
        self.bottomRight = bottomRight
""");
            }
        }

        String runner = """

def _dapcode_run_one(_raw_input):
    import sys, re, inspect, ast, json
    global _GUESS_PICK
    _GUESS_PICK = 0

    _slug = "__SLUG__"
    if not _raw_input:
        return

    _lines = [_l.strip() for _l in _raw_input.split('\\n') if _l.strip()]
    if len(_lines) >= 2 and _lines[0].startswith('[') and _lines[1].startswith('['):
        try:
            _commands = json.loads(_lines[0])
            _args_list = json.loads(_lines[1])
            _cls_name = _commands[0]
            _cls = globals().get(_cls_name)
            if _cls:
                _obj = _cls(*_args_list[0])
                _results = [None]
                for _cmd, _m_args in zip(_commands[1:], _args_list[1:]):
                    _fn = getattr(_obj, _cmd)
                    _res = _fn(*_m_args)
                    _results.append(_res)
                print(json.dumps(_results))
                return
        except Exception:
            pass

    _scope = {}
    if _raw_input.startswith('{') and _raw_input.endswith('}'):
        try:
            _scope = json.loads(_raw_input)
        except Exception:
            pass

    if not _scope:
        _parts = re.split(r'(?:,|\\r?\\n)\\s*(?=[a-zA-Z_]\\w*\\s*=)', _raw_input)
        for _part in _parts:
            if '=' in _part:
                _k, _v = _part.split('=', 1)
                _v_str = _v.strip()
                try:
                    _scope[_k.strip()] = eval(_v_str, {"true": True, "false": False, "null": None})
                except Exception:
                    try:
                        _scope[_k.strip()] = ast.literal_eval(_v_str)
                    except Exception:
                        if re.fullmatch(r'[01]{32}', _v_str):
                            _scope[_k.strip()] = int(_v_str, 2)
    if not _scope and _lines:
        for _idx, _l in enumerate(_lines):
            try:
                _scope[f'_arg_{_idx}'] = eval(_l, {"true": True, "false": False, "null": None})
            except Exception:
                pass

    if 'pick' in _scope:
        try:
            _GUESS_PICK = int(_scope['pick'])
        except Exception:
            pass

    if _slug == 'serialize-and-deserialize-binary-tree' and 'Codec' in globals():
        _root_val = _scope.get('root', next(iter(_scope.values()), []))
        _root_node = _to_tree_node(_root_val)
        _ser = Codec()
        _deser = Codec()
        _ans = _deser.deserialize(_ser.serialize(_root_node))
        print(json.dumps(_from_tree_node(_ans)))
        return

    if _slug == 'encode-and-decode-strings':
        _cls = globals().get('Solution') or globals().get('Codec')
        _sol = _cls()
        _strs = _scope.get('strs', _scope.get('dummy_input', next(iter(_scope.values()), [])))
        _enc = _sol.encode(_strs)
        _dec = _sol.decode(_enc)
        print(json.dumps(_dec))
        return

    if 'Solution' not in globals():
        return

    _sol = Solution()
    _methods = [_name for _name, _fn in inspect.getmembers(_sol, predicate=inspect.ismethod) if not _name.startswith('_')]
    if _methods:
        _target_method = getattr(_sol, _methods[0])
        _sig = inspect.signature(_target_method)
        _params = list(_sig.parameters.values())
        _raw_args = []
        for _param in _params:
            if _param.name in _scope:
                _raw_args.append((_param, _scope[_param.name]))
        if len(_raw_args) < len(_params) and _scope:
            _raw_args = list(zip(_params, _scope.values()))

        _args = []
        _root_tree_ref = None
        for _param, _val in _raw_args:
            _ann = str(_param.annotation) if _param.annotation is not inspect.Parameter.empty else ''
            if _slug == 'clone-graph':
                _val = _to_graph_node(_val, Node)
            elif _slug == 'copy-list-with-random-pointer':
                _val = _to_random_list(_val, Node)
            elif 'MountainArray' in _ann or _param.name == 'mountainArr':
                _val = MountainArray(_val)
            elif 'TreeNode' in _ann or _param.name in ('root', 'subRoot', 'tree', 't1', 't2'):
                if isinstance(_val, int) and _root_tree_ref is not None:
                    _found = _find_tree_node(_root_tree_ref, _val)
                    _val = _found if _found is not None else TreeNode(_val)
                else:
                    _val = _to_tree_node(_val)
                    if _root_tree_ref is None and isinstance(_val, TreeNode):
                        _root_tree_ref = _val
            elif _param.name == 'lists' or re.search(r'(?:List|list)\\s*\\[.*ListNode', _ann):
                _val = [_to_list_node(_x) for _x in _val] if isinstance(_val, list) else []
            elif 'ListNode' in _ann or _param.name in ('head', 'headA', 'headB', 'list1', 'list2', 'l1', 'l2'):
                _pos = _scope.get('pos', -1) if _param.name == 'head' else -1
                if isinstance(_val, list) and _val and isinstance(_val[0], list):
                    _val = [_to_list_node(_x) for _x in _val]
                else:
                    _val = _to_list_node(_val, _pos)
            _args.append(_val)

        _res = _target_method(*_args)
        _ret_ann = str(_sig.return_annotation) if _sig.return_annotation is not inspect.Signature.empty else ''

        if _res is None and ('None' in _ret_ann or _slug in ('walls-and-gates', 'merge-sorted-array', 'reorder-list', 'reverse-string', 'rotate-array', 'rotate-image', 'set-matrix-zeroes', 'sort-colors', 'surrounded-regions')) and not ('TreeNode' in _ret_ann or 'ListNode' in _ret_ann):
            _res = _args[0] if _args else None

        if _slug == 'clone-graph':
            print(json.dumps(_from_graph_node(_res)))
        elif _slug == 'copy-list-with-random-pointer':
            print(json.dumps(_from_random_list(_res)))
        elif _slug == 'construct-quad-tree':
            print(json.dumps(_from_quad_tree(_res)))
        elif _slug == 'lowest-common-ancestor-of-a-binary-search-tree' and isinstance(_res, TreeNode):
            print(_res.val)
        elif isinstance(_res, TreeNode):
            print(json.dumps(_from_tree_node(_res)))
        elif isinstance(_res, ListNode):
            print(json.dumps(_from_list_node(_res)))
        elif _res is None and ('TreeNode' in _ret_ann or 'ListNode' in _ret_ann):
            print('[]')
        elif isinstance(_res, list):
            print(json.dumps(_res))
        else:
            print(_res)

def _dapcode_main():
    import sys, io, time, traceback
    _all = sys.stdin.read()
    _hdr = '<<<DAPCODE_BATCH_V1>>>'
    if not _all.startswith(_hdr):
        try:
            _dapcode_run_one(_all.strip())
        except Exception:
            traceback.print_exc(file=sys.stderr)
            sys.exit(1)
        return
    _body = _all[len(_hdr):]
    if _body.startswith('\\r\\n'):
        _body = _body[2:]
    elif _body.startswith('\\n'):
        _body = _body[1:]
    _cases = _body.split('\\n<<<DAPCODE_CASE_SEP_9F3A>>>\\n')
    _real_out = sys.stdout
    for _ci, _c_in in enumerate(_cases):
        _buf = io.StringIO()
        sys.stdout = _buf
        _ok = True
        _err = ''
        _t0 = time.perf_counter()
        try:
            _dapcode_run_one(_c_in.strip())
        except Exception:
            _ok = False
            _err = traceback.format_exc()
        _ms = (time.perf_counter() - _t0) * 1000.0
        sys.stdout = _real_out
        _payload = (_buf.getvalue() if _ok else _err).rstrip('\\r\\n')
        _real_out.write(f"<<<DAPCODE_RES_START:{_ci}:{'OK' if _ok else 'ERR'}:{_ms:.3f}>>>\\n{_payload}\\n<<<DAPCODE_RES_END:{_ci}>>>\\n")

if __name__ == '__main__':
    _dapcode_main()
""".replace("__SLUG__", cleanSlug.replace("\"", ""));
        return prefix + nodeDef + "\n" + code + runner;
    }

    private String wrapCpp(String slug, String code) {
        if (code.contains("int main")) {
            return code;
        }

        String cleanSlug = slug != null ? slug.toLowerCase().trim() : "";
        String uncommented = stripComments(code);

        StringBuilder nodeDefs = new StringBuilder();
        if (!uncommented.contains("struct ListNode") && !uncommented.contains("class ListNode")) {
            nodeDefs.append("""
struct ListNode {
    int val;
    ListNode *next;
    ListNode() : val(0), next(nullptr) {}
    ListNode(int x) : val(x), next(nullptr) {}
    ListNode(int x, ListNode *next) : val(x), next(next) {}
};
""");
        }
        if (!uncommented.contains("struct TreeNode") && !uncommented.contains("class TreeNode")) {
            nodeDefs.append("""
struct TreeNode {
    int val;
    TreeNode *left;
    TreeNode *right;
    TreeNode() : val(0), left(nullptr), right(nullptr) {}
    TreeNode(int x) : val(x), left(nullptr), right(nullptr) {}
    TreeNode(int x, TreeNode *left, TreeNode *right) : val(x), left(left), right(right) {}
};
""");
        }
        if ("guess-number-higher-or-lower".equals(cleanSlug) && !uncommented.contains("int guess(")) {
            nodeDefs.append("""
static int guess(int num) {
    if (num > _GUESS_PICK) return -1;
    if (num < _GUESS_PICK) return 1;
    return 0;
}
""");
        }
        if ("find-in-mountain-array".equals(cleanSlug)
                && !uncommented.contains("class MountainArray")
                && !uncommented.contains("struct MountainArray")) {
            nodeDefs.append("""
class MountainArray {
    vector<int> arr_;
public:
    MountainArray(vector<int> a) : arr_(move(a)) {}
    int get(int index) { return arr_[index]; }
    int length() { return (int)arr_.size(); }
};
""");
        }
        if (!uncommented.contains("class Node") && !uncommented.contains("struct Node")) {
            if ("clone-graph".equals(cleanSlug)) {
                nodeDefs.append("""
class Node {
public:
    int val;
    vector<Node*> neighbors;
    Node() : val(0), neighbors(vector<Node*>()) {}
    Node(int _val) : val(_val), neighbors(vector<Node*>()) {}
    Node(int _val, vector<Node*> _neighbors) : val(_val), neighbors(_neighbors) {}
};
""");
            } else if ("copy-list-with-random-pointer".equals(cleanSlug)) {
                nodeDefs.append("""
class Node {
public:
    int val;
    Node* next;
    Node* random;
    Node(int _val) : val(_val), next(nullptr), random(nullptr) {}
};
""");
            } else if ("construct-quad-tree".equals(cleanSlug)) {
                nodeDefs.append("""
class Node {
public:
    bool val;
    bool isLeaf;
    Node* topLeft;
    Node* topRight;
    Node* bottomLeft;
    Node* bottomRight;
    Node() : val(false), isLeaf(false), topLeft(nullptr), topRight(nullptr), bottomLeft(nullptr), bottomRight(nullptr) {}
    Node(bool _val, bool _isLeaf) : val(_val), isLeaf(_isLeaf), topLeft(nullptr), topRight(nullptr), bottomLeft(nullptr), bottomRight(nullptr) {}
    Node(bool _val, bool _isLeaf, Node* _tl, Node* _tr, Node* _bl, Node* _br)
        : val(_val), isLeaf(_isLeaf), topLeft(_tl), topRight(_tr), bottomLeft(_bl), bottomRight(_br) {}
};
""");
            }
        }

        String headers = """
#include <iostream>
#include <vector>
#include <string>
#include <sstream>
#include <unordered_map>
#include <unordered_set>
#include <algorithm>
#include <cmath>
#include <queue>
#include <stack>
#include <deque>
#include <list>
#include <map>
#include <set>
#include <climits>
#include <limits>
#include <numeric>
#include <cstring>
#include <cctype>
#include <cstdint>
#include <functional>
#include <bitset>
#include <tuple>
#include <array>
#include <utility>
#include <iomanip>
#include <chrono>

using namespace std;

static int _POS_CYCLE = -1;
static int _GUESS_PICK = 0;

""" + nodeDefs + """

static string trimStr(const string& s) {
    size_t b = 0, e = s.size();
    while (b < e && isspace((unsigned char)s[b])) b++;
    while (e > b && isspace((unsigned char)s[e - 1])) e--;
    return s.substr(b, e - b);
}

static string extractRhs(const string& part) {
    int depth = 0;
    char quote = 0;
    for (size_t i = 0; i < part.size(); i++) {
        char c = part[i];
        if (quote) {
            if (c == '\\\\' && i + 1 < part.size()) { i++; continue; }
            if (c == quote) quote = 0;
        } else {
            if (c == '"' || c == '\\'') quote = c;
            else if (c == '[' || c == '{' || c == '(') depth++;
            else if (c == ']' || c == '}' || c == ')') depth--;
            else if (c == '=' && depth == 0) return trimStr(part.substr(i + 1));
        }
    }
    return trimStr(part);
}

static vector<string> splitTopLevel(const string& s, char sep) {
    vector<string> parts;
    int depth = 0;
    char quote = 0;
    size_t start = 0;
    for (size_t i = 0; i < s.size(); i++) {
        char c = s[i];
        if (quote) {
            if (c == '\\\\' && i + 1 < s.size()) { i++; continue; }
            if (c == quote) quote = 0;
        } else {
            if (c == '"' || c == '\\'') quote = c;
            else if (c == '[' || c == '{' || c == '(') depth++;
            else if (c == ']' || c == '}' || c == ')') depth--;
            else if (c == sep && depth == 0) {
                string t = trimStr(s.substr(start, i - start));
                if (!t.empty()) parts.push_back(t);
                start = i + 1;
            }
        }
    }
    string tail = trimStr(s.substr(start));
    if (!tail.empty()) parts.push_back(tail);
    return parts;
}

static vector<string> parseArrayTokens(const string& raw) {
    string s = extractRhs(raw);
    size_t start = s.find('[');
    size_t end = s.rfind(']');
    if (start == string::npos || end == string::npos || start >= end) return {};
    string inner = trimStr(s.substr(start + 1, end - start - 1));
    if (inner.empty()) return {};
    return splitTopLevel(inner, ',');
}

static string unquoteStr(const string& raw) {
    string s = trimStr(raw);
    if (s.size() >= 2 && ((s.front() == '"' && s.back() == '"') || (s.front() == '\\'' && s.back() == '\\''))) {
        string inner = s.substr(1, s.size() - 2);
        string out;
        out.reserve(inner.size());
        for (size_t i = 0; i < inner.size(); i++) {
            if (inner[i] == '\\\\' && i + 1 < inner.size()) {
                char nxt = inner[++i];
                if (nxt == 'n') out.push_back('\\n');
                else if (nxt == 't') out.push_back('\\t');
                else if (nxt == 'r') out.push_back('\\r');
                else out.push_back(nxt);
            } else {
                out.push_back(inner[i]);
            }
        }
        return out;
    }
    return s;
}

static string parseStringVal(const string& part) {
    return unquoteStr(extractRhs(part));
}

static char parseCharVal(const string& part) {
    string s = parseStringVal(part);
    return s.empty() ? ' ' : s[0];
}

static long long parseLongLongVal(const string& part) {
    string s = unquoteStr(extractRhs(part));
    if (s.empty()) return 0;
    try { return stoll(s); } catch (...) { return 0; }
}

static int parseIntVal(const string& part) {
    return (int)parseLongLongVal(part);
}

static uint32_t parseUint32Val(const string& part) {
    string s = unquoteStr(extractRhs(part));
    if (s.empty()) return 0;
    bool isBin32 = (s.size() == 32);
    for (char c : s) {
        if (c != '0' && c != '1') { isBin32 = false; break; }
    }
    if (isBin32) {
        uint32_t v = 0;
        for (char c : s) v = (v << 1) | (uint32_t)(c - '0');
        return v;
    }
    try { return (uint32_t)stoull(s); } catch (...) { return 0; }
}

static double parseDoubleVal(const string& part) {
    string s = unquoteStr(extractRhs(part));
    if (s.empty()) return 0.0;
    try { return stod(s); } catch (...) { return 0.0; }
}

static bool parseBoolVal(const string& part) {
    string s = unquoteStr(extractRhs(part));
    for (char& c : s) c = (char)tolower((unsigned char)c);
    return s == "true" || s == "1";
}

static vector<int> parseVectorInt(const string& s) {
    vector<int> res;
    for (const string& tok : parseArrayTokens(s)) {
        res.push_back(parseIntVal(tok));
    }
    return res;
}

static vector<long long> parseVectorLongLong(const string& s) {
    vector<long long> res;
    for (const string& tok : parseArrayTokens(s)) {
        res.push_back(parseLongLongVal(tok));
    }
    return res;
}

static vector<double> parseVectorDouble(const string& s) {
    vector<double> res;
    for (const string& tok : parseArrayTokens(s)) {
        res.push_back(parseDoubleVal(tok));
    }
    return res;
}

static vector<string> parseVectorString(const string& s) {
    vector<string> res;
    for (const string& tok : parseArrayTokens(s)) {
        res.push_back(unquoteStr(tok));
    }
    return res;
}

static vector<char> parseVectorChar(const string& s) {
    vector<char> res;
    for (const string& tok : parseArrayTokens(s)) {
        string u = unquoteStr(tok);
        res.push_back(u.empty() ? ' ' : u[0]);
    }
    return res;
}

static vector<vector<int>> parseVector2DInt(const string& s) {
    vector<vector<int>> res;
    for (const string& row : parseArrayTokens(s)) {
        res.push_back(parseVectorInt(row));
    }
    return res;
}

static vector<vector<char>> parseVector2DChar(const string& s) {
    vector<vector<char>> res;
    for (const string& row : parseArrayTokens(s)) {
        res.push_back(parseVectorChar(row));
    }
    return res;
}

static vector<vector<string>> parseVector2DString(const string& s) {
    vector<vector<string>> res;
    for (const string& row : parseArrayTokens(s)) {
        res.push_back(parseVectorString(row));
    }
    return res;
}

static ListNode* parseListNode(const string& s, int cyclePos = -1) {
    vector<int> vals = parseVectorInt(s);
    ListNode dummy(0);
    ListNode* curr = &dummy;
    vector<ListNode*> nodes;
    for (int v : vals) {
        curr->next = new ListNode(v);
        curr = curr->next;
        nodes.push_back(curr);
    }
    if (cyclePos >= 0 && cyclePos < (int)nodes.size() && !nodes.empty()) {
        nodes.back()->next = nodes[cyclePos];
    }
    return dummy.next;
}

static vector<ListNode*> parseVectorListNode(const string& s) {
    vector<ListNode*> res;
    for (const string& row : parseArrayTokens(s)) {
        res.push_back(parseListNode(row, -1));
    }
    return res;
}

static TreeNode* parseTreeNode(const string& s) {
    string rhs = extractRhs(s);
    if (rhs.find('[') == string::npos) {
        if (rhs.empty() || rhs == "null" || rhs == "None") return nullptr;
        return new TreeNode(parseIntVal(rhs));
    }
    vector<string> tokens = parseArrayTokens(rhs);
    if (tokens.empty() || tokens[0] == "null" || tokens[0] == "None") return nullptr;
    TreeNode* root = new TreeNode(parseIntVal(tokens[0]));
    queue<TreeNode*> q;
    q.push(root);
    size_t idx = 1;
    while (!q.empty() && idx < tokens.size()) {
        TreeNode* curr = q.front();
        q.pop();
        if (idx < tokens.size()) {
            string t = tokens[idx++];
            if (t != "null" && t != "None") {
                curr->left = new TreeNode(parseIntVal(t));
                q.push(curr->left);
            }
        }
        if (idx < tokens.size()) {
            string t = tokens[idx++];
            if (t != "null" && t != "None") {
                curr->right = new TreeNode(parseIntVal(t));
                q.push(curr->right);
            }
        }
    }
    return root;
}

static TreeNode* findTreeNode(TreeNode* root, int val) {
    if (!root) return nullptr;
    if (root->val == val) return root;
    TreeNode* l = findTreeNode(root->left, val);
    return l ? l : findTreeNode(root->right, val);
}

static vector<pair<string, string>> readInputKeyValues() {
    vector<string> lines;
    string l;
    while (getline(cin, l)) {
        string t = trimStr(l);
        if (!t.empty()) lines.push_back(t);
    }
    if (lines.empty()) return {};
    vector<string> rawParts;
    if (lines.size() > 1) {
        for (const string& ln : lines) {
            auto sub = splitTopLevel(ln, ',');
            for (auto& sp : sub) rawParts.push_back(sp);
        }
    } else {
        rawParts = splitTopLevel(lines[0], ',');
    }
    vector<pair<string, string>> kvs;
    for (const string& part : rawParts) {
        int depth = 0;
        char quote = 0;
        size_t eqPos = string::npos;
        for (size_t i = 0; i < part.size(); i++) {
            char c = part[i];
            if (quote) {
                if (c == '\\\\' && i + 1 < part.size()) { i++; continue; }
                if (c == quote) quote = 0;
            } else {
                if (c == '"' || c == '\\'') quote = c;
                else if (c == '[' || c == '{' || c == '(') depth++;
                else if (c == ']' || c == '}' || c == ')') depth--;
                else if (c == '=' && depth == 0) { eqPos = i; break; }
            }
        }
        if (eqPos != string::npos) {
            string k = trimStr(part.substr(0, eqPos));
            string v = trimStr(part.substr(eqPos + 1));
            if (k == "pos") { _POS_CYCLE = parseIntVal(v); continue; }
            kvs.push_back({k, v});
        } else {
            kvs.push_back({"", trimStr(part)});
        }
    }
    return kvs;
}

static string getInputVal(const vector<pair<string, string>>& kvs, size_t idx, const string& paramName) {
    if (!paramName.empty()) {
        for (const auto& kv : kvs) {
            if (kv.first == paramName) return kv.second;
        }
    }
    if (idx < kvs.size()) return kvs[idx].second;
    return "";
}

static void printResult(bool b) { cout << (b ? "True" : "False"); }
static void printResult(int x) { cout << x; }
static void printResult(uint32_t x) { cout << x; }
static void printResult(long x) { cout << x; }
static void printResult(unsigned long x) { cout << x; }
static void printResult(long long x) { cout << x; }
static void printResult(double x) { cout << setprecision(10) << x; }
static void printResult(char c) { cout << c; }
static void printResult(const string& s) { cout << s; }

static void printResult(const vector<int>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << res[i] << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<long long>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << res[i] << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<double>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << setprecision(10) << res[i] << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<bool>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << (res[i] ? "True" : "False") << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<char>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << "\\"" << res[i] << "\\"" << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<string>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) cout << "\\"" << res[i] << "\\"" << (i + 1 < res.size() ? ", " : "");
    cout << "]";
}

static void printResult(const vector<vector<int>>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) {
        printResult(res[i]);
        if (i + 1 < res.size()) cout << ", ";
    }
    cout << "]";
}

static void printResult(const vector<vector<char>>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) {
        printResult(res[i]);
        if (i + 1 < res.size()) cout << ", ";
    }
    cout << "]";
}

static void printResult(const vector<vector<string>>& res) {
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) {
        printResult(res[i]);
        if (i + 1 < res.size()) cout << ", ";
    }
    cout << "]";
}

static void printResult(ListNode* head) {
    cout << "[";
    bool first = true;
    unordered_set<ListNode*> visited;
    while (head && visited.insert(head).second) {
        if (!first) cout << ", ";
        cout << head->val;
        first = false;
        head = head->next;
    }
    cout << "]";
}

static void printResult(TreeNode* root) {
    if (!root) { cout << "[]"; return; }
    vector<string> out;
    queue<TreeNode*> q;
    q.push(root);
    while (!q.empty()) {
        TreeNode* curr = q.front();
        q.pop();
        if (!curr) {
            out.push_back("null");
        } else {
            out.push_back(to_string(curr->val));
            q.push(curr->left);
            q.push(curr->right);
        }
    }
    while (!out.empty() && out.back() == "null") out.pop_back();
    cout << "[";
    for (size_t i = 0; i < out.size(); i++) cout << out[i] << (i + 1 < out.size() ? ", " : "");
    cout << "]";
}

static void printDesignResult(bool b) { cout << (b ? "true" : "false"); }
static void printDesignResult(int x) { cout << x; }
static void printDesignResult(long long x) { cout << x; }
static void printDesignResult(double x) {
    if (floor(x) == x && fabs(x) < 1e15) {
        cout << fixed << setprecision(1) << x << defaultfloat;
    } else {
        cout << defaultfloat << setprecision(10) << x;
    }
}
static void printDesignResult(const string& s) { cout << "\\"" << s << "\\""; }
static void printDesignResult(const vector<int>& res) { printResult(res); }
static void printDesignResult(const vector<string>& res) { printResult(res); }
""";

        String driver = wrapCppBatchMain(buildCppDriver(cleanSlug, code));
        return headers + "\n" + code + "\n" + driver;
    }

    private String wrapCppBatchMain(String singleCaseDriver) {
        String renamed = singleCaseDriver
                .replace("int main() {", "static void _dapcode_run_case() {")
                .replace("    ios_base::sync_with_stdio(false);\n", "")
                .replace("    cin.tie(NULL);\n", "")
                .replace("return 0;", "return;");
        return renamed + """

int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    ostringstream _inBuf;
    _inBuf << cin.rdbuf();
    string _allIn = _inBuf.str();
    const string _hdr = "<<<DAPCODE_BATCH_V1>>>";
    if (_allIn.rfind(_hdr, 0) != 0) {
        istringstream _singleIn(_allIn);
        cin.clear();
        streambuf* _origCin = cin.rdbuf(_singleIn.rdbuf());
        _dapcode_run_case();
        cin.rdbuf(_origCin);
        return 0;
    }
    size_t _pos = _hdr.size();
    if (_pos < _allIn.size() && _allIn[_pos] == '\\r') _pos++;
    if (_pos < _allIn.size() && _allIn[_pos] == '\\n') _pos++;
    string _body = _allIn.substr(_pos);
    const string _sep = "\\n<<<DAPCODE_CASE_SEP_9F3A>>>\\n";
    vector<string> _cases;
    size_t _start = 0;
    while (true) {
        size_t _idx = _body.find(_sep, _start);
        if (_idx == string::npos) {
            _cases.push_back(_body.substr(_start));
            break;
        }
        _cases.push_back(_body.substr(_start, _idx - _start));
        _start = _idx + _sep.size();
    }
    streambuf* _origCin = cin.rdbuf();
    streambuf* _origCout = cout.rdbuf();
    for (size_t _ci = 0; _ci < _cases.size(); _ci++) {
        _POS_CYCLE = -1;
        _GUESS_PICK = 0;
        istringstream _caseIn(_cases[_ci]);
        ostringstream _caseOut;
        cin.clear();
        cout.clear();
        cin.rdbuf(_caseIn.rdbuf());
        cout.rdbuf(_caseOut.rdbuf());
        cout << defaultfloat << setprecision(6);
        bool _ok = true;
        string _err;
        auto _t0 = chrono::steady_clock::now();
        try {
            _dapcode_run_case();
        } catch (const exception& _e) {
            _ok = false;
            _err = _e.what();
        } catch (...) {
            _ok = false;
            _err = "Unknown C++ runtime exception";
        }
        auto _t1 = chrono::steady_clock::now();
        double _ms = chrono::duration<double, milli>(_t1 - _t0).count();
        cin.rdbuf(_origCin);
        cout.rdbuf(_origCout);
        string _payload = _ok ? _caseOut.str() : _err;
        while (!_payload.empty() && (_payload.back() == '\\n' || _payload.back() == '\\r')) _payload.pop_back();
        cout << "<<<DAPCODE_RES_START:" << _ci << ":" << (_ok ? "OK" : "ERR") << ":" << fixed << setprecision(3) << _ms << defaultfloat << ">>>\\n"
             << _payload << "\\n<<<DAPCODE_RES_END:" << _ci << ">>>\\n";
    }
    return 0;
}
""";
    }

    private String buildCppDriver(String cleanSlug, String code) {
        if (isDesignProblem(cleanSlug, code)) {
            return buildCppDesignDriver(cleanSlug, code);
        }

        String uncommented = stripComments(code);

        if ("serialize-and-deserialize-binary-tree".equals(cleanSlug)) {
            return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    TreeNode* root = parseTreeNode(getInputVal(kvs, 0, "root"));
    Codec ser, deser;
    TreeNode* ans = deser.deserialize(ser.serialize(root));
    printResult(ans);
    return 0;
}
""";
        }

        if ("encode-and-decode-strings".equals(cleanSlug)) {
            boolean usesCodec = Pattern.compile("\\bclass\\s+Codec\\b").matcher(uncommented).find()
                    && !Pattern.compile("\\bclass\\s+Solution\\b").matcher(uncommented).find();
            String cls = usesCodec ? "Codec" : "Solution";
            return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    vector<string> strs = parseVectorString(getInputVal(kvs, 0, "strs"));
    __CLS__ sol;
    string encoded = sol.encode(strs);
    printResult(sol.decode(encoded));
    return 0;
}
""".replace("__CLS__", cls);
        }

        if ("clone-graph".equals(cleanSlug)) {
            String method = pickCppPublicMethodName(code, "cloneGraph");
            return """
static Node* _parseGraphNode(const string& raw) {
    vector<vector<int>> adj = parseVector2DInt(raw);
    if (adj.empty()) return nullptr;
    vector<Node*> nodes(adj.size());
    for (size_t i = 0; i < adj.size(); i++) nodes[i] = new Node((int)i + 1);
    for (size_t i = 0; i < adj.size(); i++) {
        for (int nb : adj[i]) {
            if (nb >= 1 && nb <= (int)adj.size()) nodes[i]->neighbors.push_back(nodes[nb - 1]);
        }
    }
    return nodes[0];
}
static void _printGraphNode(Node* node) {
    if (!node) { cout << "[]"; return; }
    map<int, vector<int>> visited;
    queue<Node*> q;
    q.push(node);
    int mx = 0;
    while (!q.empty()) {
        Node* curr = q.front();
        q.pop();
        if (visited.count(curr->val)) continue;
        mx = max(mx, curr->val);
        vector<int> nbs;
        for (Node* nb : curr->neighbors) {
            nbs.push_back(nb->val);
            if (!visited.count(nb->val)) q.push(nb);
        }
        visited[curr->val] = nbs;
    }
    vector<vector<int>> out;
    for (int i = 1; i <= mx; i++) out.push_back(visited[i]);
    printResult(out);
}
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    Node* node = _parseGraphNode(getInputVal(kvs, 0, "adjList"));
    Solution sol;
    _printGraphNode(sol.__METHOD__(node));
    return 0;
}
""".replace("__METHOD__", method);
        }

        if ("copy-list-with-random-pointer".equals(cleanSlug)) {
            String method = pickCppPublicMethodName(code, "copyRandomList");
            return """
static Node* _parseRandomList(const string& raw) {
    vector<string> rows = parseArrayTokens(raw);
    if (rows.empty()) return nullptr;
    vector<Node*> nodes;
    vector<int> randIdx;
    for (const string& row : rows) {
        vector<string> pairToks = parseArrayTokens(row);
        int val = pairToks.size() > 0 ? parseIntVal(pairToks[0]) : 0;
        int rIdx = -1;
        if (pairToks.size() > 1 && pairToks[1] != "null" && pairToks[1] != "None") {
            rIdx = parseIntVal(pairToks[1]);
        }
        nodes.push_back(new Node(val));
        randIdx.push_back(rIdx);
    }
    for (size_t i = 0; i + 1 < nodes.size(); i++) nodes[i]->next = nodes[i + 1];
    for (size_t i = 0; i < nodes.size(); i++) {
        if (randIdx[i] >= 0 && randIdx[i] < (int)nodes.size()) nodes[i]->random = nodes[randIdx[i]];
    }
    return nodes[0];
}
static void _printRandomList(Node* head) {
    if (!head) { cout << "[]"; return; }
    vector<Node*> nodes;
    unordered_map<Node*, int> idxMap;
    Node* curr = head;
    while (curr && !idxMap.count(curr)) {
        idxMap[curr] = (int)nodes.size();
        nodes.push_back(curr);
        curr = curr->next;
    }
    cout << "[";
    for (size_t i = 0; i < nodes.size(); i++) {
        if (i > 0) cout << ", ";
        cout << "[" << nodes[i]->val << ", ";
        if (nodes[i]->random && idxMap.count(nodes[i]->random)) cout << idxMap[nodes[i]->random];
        else cout << "null";
        cout << "]";
    }
    cout << "]";
}
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    Node* head = _parseRandomList(getInputVal(kvs, 0, "head"));
    Solution sol;
    _printRandomList(sol.__METHOD__(head));
    return 0;
}
""".replace("__METHOD__", method);
        }

        if ("construct-quad-tree".equals(cleanSlug)) {
            String method = pickCppPublicMethodName(code, "construct");
            return """
static void _printQuadTree(Node* root) {
    if (!root) { cout << "[]"; return; }
    vector<string> res;
    queue<Node*> q;
    q.push(root);
    while (!q.empty()) {
        Node* curr = q.front();
        q.pop();
        if (!curr) {
            res.push_back("null");
        } else {
            int leaf = curr->isLeaf ? 1 : 0;
            int v = curr->isLeaf ? (curr->val ? 1 : 0) : 1;
            res.push_back("[" + to_string(leaf) + ", " + to_string(v) + "]");
            if (!curr->isLeaf) {
                q.push(curr->topLeft);
                q.push(curr->topRight);
                q.push(curr->bottomLeft);
                q.push(curr->bottomRight);
            } else {
                q.push(nullptr); q.push(nullptr); q.push(nullptr); q.push(nullptr);
            }
        }
    }
    while (!res.empty() && res.back() == "null") res.pop_back();
    cout << "[";
    for (size_t i = 0; i < res.size(); i++) {
        if (i > 0) cout << ", ";
        cout << res[i];
    }
    cout << "]";
}
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    vector<vector<int>> grid = parseVector2DInt(getInputVal(kvs, 0, "grid"));
    Solution sol;
    _printQuadTree(sol.__METHOD__(grid));
    return 0;
}
""".replace("__METHOD__", method);
        }

        if ("lowest-common-ancestor-of-a-binary-search-tree".equals(cleanSlug)) {
            String method = pickCppPublicMethodName(code, "lowestCommonAncestor");
            return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    TreeNode* root = parseTreeNode(getInputVal(kvs, 0, "root"));
    int pVal = parseIntVal(getInputVal(kvs, 1, "p"));
    int qVal = parseIntVal(getInputVal(kvs, 2, "q"));
    TreeNode* p = findTreeNode(root, pVal);
    if (!p) p = new TreeNode(pVal);
    TreeNode* q = findTreeNode(root, qVal);
    if (!q) q = new TreeNode(qVal);
    Solution sol;
    TreeNode* ans = sol.__METHOD__(root, p, q);
    if (ans) cout << ans->val;
    else cout << "null";
    return 0;
}
""".replace("__METHOD__", method);
        }

        if ("guess-number-higher-or-lower".equals(cleanSlug)) {
            String method = pickCppPublicMethodName(code, "guessNumber");
            return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
    int n = parseIntVal(getInputVal(kvs, 0, "n"));
    _GUESS_PICK = parseIntVal(getInputVal(kvs, 1, "pick"));
    Solution sol;
    printResult(sol.__METHOD__(n));
    return 0;
}
""".replace("__METHOD__", method);
        }

        List<ParsedMethod> publicMethods = parseCppClassMethods(code, "Solution");
        ParsedMethod target = selectBestCppMethod(publicMethods);
        if (target == null) {
            return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    Solution sol;
    printResult(sol.solve());
    return 0;
}
""";
        }

        StringBuilder decls = new StringBuilder();
        List<String> argNames = new ArrayList<>();
        String firstTreeArg = null;
        for (int i = 0; i < target.params().size(); i++) {
            ParsedParam param = target.params().get(i);
            String arg = "arg" + i;
            argNames.add(arg);
            String valExpr = "getInputVal(kvs, " + i + ", \"" + escapeCppString(param.name()) + "\")";
            String normType = normalizeCppType(param.type());

            if (normType.contains("vector<vector<int>>")) {
                decls.append("    vector<vector<int>> ").append(arg).append(" = parseVector2DInt(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<vector<char>>")) {
                decls.append("    vector<vector<char>> ").append(arg).append(" = parseVector2DChar(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<vector<string>>")) {
                decls.append("    vector<vector<string>> ").append(arg).append(" = parseVector2DString(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<ListNode*>")) {
                decls.append("    vector<ListNode*> ").append(arg).append(" = parseVectorListNode(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<double>")) {
                decls.append("    vector<double> ").append(arg).append(" = parseVectorDouble(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<longlong>")) {
                decls.append("    vector<long long> ").append(arg).append(" = parseVectorLongLong(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<string>")) {
                decls.append("    vector<string> ").append(arg).append(" = parseVectorString(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<char>")) {
                decls.append("    vector<char> ").append(arg).append(" = parseVectorChar(").append(valExpr).append(");\n");
            } else if (normType.contains("vector<int>")) {
                decls.append("    vector<int> ").append(arg).append(" = parseVectorInt(").append(valExpr).append(");\n");
            } else if (normType.contains("MountainArray")) {
                decls.append("    MountainArray ").append(arg).append("(parseVectorInt(").append(valExpr).append("));\n");
            } else if (normType.contains("TreeNode")) {
                if (firstTreeArg != null) {
                    decls.append("    string _raw_").append(arg).append(" = ").append(valExpr).append(";\n");
                    decls.append("    TreeNode* ").append(arg).append(" = (_raw_").append(arg).append(".find('[') == string::npos) ? findTreeNode(")
                         .append(firstTreeArg).append(", parseIntVal(_raw_").append(arg).append(")) : parseTreeNode(_raw_").append(arg).append(");\n");
                    decls.append("    if (!").append(arg).append(" && _raw_").append(arg).append(".find('[') == string::npos && !_raw_").append(arg).append(".empty()) ")
                         .append(arg).append(" = new TreeNode(parseIntVal(_raw_").append(arg).append("));\n");
                } else {
                    decls.append("    TreeNode* ").append(arg).append(" = parseTreeNode(").append(valExpr).append(");\n");
                    firstTreeArg = arg;
                }
            } else if (normType.contains("ListNode")) {
                boolean applyCycle = i == 0 || "head".equals(param.name());
                decls.append("    ListNode* ").append(arg).append(" = parseListNode(").append(valExpr)
                     .append(applyCycle ? ", _POS_CYCLE" : ", -1").append(");\n");
            } else if (normType.contains("string")) {
                decls.append("    string ").append(arg).append(" = parseStringVal(").append(valExpr).append(");\n");
            } else if (normType.contains("double") || normType.contains("float")) {
                decls.append("    double ").append(arg).append(" = parseDoubleVal(").append(valExpr).append(");\n");
            } else if (normType.equals("bool")) {
                decls.append("    bool ").append(arg).append(" = parseBoolVal(").append(valExpr).append(");\n");
            } else if (normType.equals("char")) {
                decls.append("    char ").append(arg).append(" = parseCharVal(").append(valExpr).append(");\n");
            } else if (normType.contains("uint32_t") || normType.contains("unsignedint") || normType.equals("unsigned")) {
                decls.append("    uint32_t ").append(arg).append(" = parseUint32Val(").append(valExpr).append(");\n");
            } else if (normType.contains("longlong") || normType.contains("int64_t") || normType.equals("long")) {
                decls.append("    long long ").append(arg).append(" = parseLongLongVal(").append(valExpr).append(");\n");
            } else {
                decls.append("    int ").append(arg).append(" = parseIntVal(").append(valExpr).append(");\n");
            }
        }

        boolean isVoid = "void".equals(normalizeCppType(target.returnType()));
        String callExpr = "sol." + target.name() + "(" + String.join(", ", argNames) + ")";
        String execBlock = isVoid
                ? ("    " + callExpr + ";\n" + (!argNames.isEmpty() ? "    printResult(arg0);" : ""))
                : ("    printResult(" + callExpr + ");");

        return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    auto kvs = readInputKeyValues();
__DECLS__    Solution sol;
__EXEC__
    return 0;
}
""".replace("__DECLS__", decls.toString()).replace("__EXEC__", execBlock);
    }

    private String buildCppDesignDriver(String cleanSlug, String code) {
        String className = DESIGN_CLASS_NAMES.get(cleanSlug);
        if (className == null) {
            className = findLastTopLevelCppClassName(code);
        }
        if (className == null) {
            className = "Solution";
        }

        List<ParsedMethod> members = parseCppClassMethods(code, className);
        ParsedMethod ctor = null;
        List<ParsedMethod> methods = new ArrayList<>();
        for (ParsedMethod m : members) {
            if (m.isConstructor()) {
                if (ctor == null) ctor = m;
            } else {
                methods.add(m);
            }
        }

        StringBuilder ctorBlock = new StringBuilder();
        List<String> ctorArgs = new ArrayList<>();
        if (ctor != null) {
            for (int i = 0; i < ctor.params().size(); i++) {
                String var = "c" + i;
                ctorArgs.add(var);
                appendCppDesignArgDecl(ctorBlock, "            ", var, ctor.params().get(i).type(),
                        "(i < argLists.size() && " + i + " < mArgs.size() ? mArgs[" + i + "] : \"\")");
            }
        }
        ctorBlock.append("            obj = new ").append(className).append("(").append(String.join(", ", ctorArgs)).append(");\n");
        ctorBlock.append("            cout << \"null\";\n");

        StringBuilder dispatch = new StringBuilder();
        Set<String> seenNames = new java.util.HashSet<>();
        for (ParsedMethod m : methods) {
            if (!seenNames.add(m.name())) {
                continue;
            }
            dispatch.append("        } else if (cmds[i] == \"").append(escapeCppString(m.name())).append("\") {\n");
            List<String> mArgs = new ArrayList<>();
            for (int j = 0; j < m.params().size(); j++) {
                String var = "a" + j;
                mArgs.add(var);
                appendCppDesignArgDecl(dispatch, "            ", var, m.params().get(j).type(),
                        "(" + j + " < mArgs.size() ? mArgs[" + j + "] : \"\")");
            }
            String call = "obj->" + m.name() + "(" + String.join(", ", mArgs) + ")";
            if ("void".equals(normalizeCppType(m.returnType()))) {
                dispatch.append("            if (obj) ").append(call).append(";\n");
                dispatch.append("            cout << \"null\";\n");
            } else {
                dispatch.append("            if (obj) printDesignResult(").append(call).append(");\n");
                dispatch.append("            else cout << \"null\";\n");
            }
        }

        return """
int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    string line1, line2;
    while (getline(cin, line1) && trimStr(line1).empty()) {}
    while (getline(cin, line2) && trimStr(line2).empty()) {}
    if (trimStr(line1).empty() || trimStr(line2).empty()) return 0;

    vector<string> cmds = parseVectorString(line1);
    vector<string> argLists = parseArrayTokens(line2);

    __CLASS__* obj = nullptr;
    cout << "[";
    for (size_t i = 0; i < cmds.size(); i++) {
        if (i > 0) cout << ", ";
        vector<string> mArgs = (i < argLists.size()) ? parseArrayTokens(argLists[i]) : vector<string>{};
        if (i == 0 || cmds[i] == "__CLASS__") {
__CTOR____DISPATCH__        } else {
            cout << "null";
        }
    }
    cout << "]";
    return 0;
}
""".replace("__CLASS__", className)
   .replace("__CTOR__", ctorBlock.toString())
   .replace("__DISPATCH__", dispatch.toString());
    }

    private void appendCppDesignArgDecl(StringBuilder sb, String indent, String var, String rawType, String expr) {
        String norm = normalizeCppType(rawType);
        if (norm.contains("vector<vector<int>>")) {
            sb.append(indent).append("vector<vector<int>> ").append(var).append(" = parseVector2DInt(").append(expr).append(");\n");
        } else if (norm.contains("vector<vector<string>>")) {
            sb.append(indent).append("vector<vector<string>> ").append(var).append(" = parseVector2DString(").append(expr).append(");\n");
        } else if (norm.contains("vector<string>")) {
            sb.append(indent).append("vector<string> ").append(var).append(" = parseVectorString(").append(expr).append(");\n");
        } else if (norm.contains("vector<int>")) {
            sb.append(indent).append("vector<int> ").append(var).append(" = parseVectorInt(").append(expr).append(");\n");
        } else if (norm.contains("string")) {
            sb.append(indent).append("string ").append(var).append(" = parseStringVal(").append(expr).append(");\n");
        } else if (norm.contains("double") || norm.contains("float")) {
            sb.append(indent).append("double ").append(var).append(" = parseDoubleVal(").append(expr).append(");\n");
        } else if (norm.equals("bool")) {
            sb.append(indent).append("bool ").append(var).append(" = parseBoolVal(").append(expr).append(");\n");
        } else if (norm.equals("char")) {
            sb.append(indent).append("char ").append(var).append(" = parseCharVal(").append(expr).append(");\n");
        } else if (norm.contains("longlong")) {
            sb.append(indent).append("long long ").append(var).append(" = parseLongLongVal(").append(expr).append(");\n");
        } else {
            sb.append(indent).append("int ").append(var).append(" = parseIntVal(").append(expr).append(");\n");
        }
    }

    private List<ParsedMethod> parseCppClassMethods(String code, String targetClassName) {
        List<ParsedMethod> result = new ArrayList<>();
        String clean = stripCommentsKeepStringsClean(code);

        Pattern clsPat = Pattern.compile("\\b(class|struct)\\s+" + Pattern.quote(targetClassName) + "\\b[^{;]*\\{");
        Matcher cm = clsPat.matcher(clean);
        if (!cm.find()) {
            return result;
        }
        boolean isPublic = "struct".equals(cm.group(1));
        int pos = cm.end();
        int depth = 1;
        StringBuilder stmt = new StringBuilder();

        while (pos < clean.length() && depth > 0) {
            char c = clean.charAt(pos++);
            if (c == '{') {
                if (depth == 1) {
                    String header = stmt.toString().trim();
                    stmt.setLength(0);
                    if (isPublic && !header.isEmpty()) {
                        ParsedMethod pm = parseCppMethodHeader(header, targetClassName);
                        if (pm != null) {
                            result.add(pm);
                        }
                    }
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 1) {
                    stmt.setLength(0);
                }
            } else if (depth == 1) {
                if (c == ';') {
                    stmt.setLength(0);
                } else if (c == ':') {
                    String tok = stmt.toString().trim();
                    if ("public".equals(tok)) {
                        isPublic = true;
                        stmt.setLength(0);
                    } else if ("private".equals(tok) || "protected".equals(tok)) {
                        isPublic = false;
                        stmt.setLength(0);
                    } else {
                        stmt.append(c);
                    }
                } else {
                    stmt.append(c);
                }
            }
        }
        return result;
    }

    private ParsedMethod parseCppMethodHeader(String rawHeader, String className) {
        String h = rawHeader.replaceAll("\\s+", " ").trim();
        if (h.isEmpty()) return null;
        if (h.startsWith("struct ") || h.startsWith("class ") || h.startsWith("enum ") || h.startsWith("union ")) {
            return null;
        }
        int openParen = h.indexOf('(');
        if (openParen == -1) return null;
        int pDepth = 0;
        int closeParen = -1;
        for (int i = openParen; i < h.length(); i++) {
            char c = h.charAt(i);
            if (c == '(') pDepth++;
            else if (c == ')') {
                pDepth--;
                if (pDepth == 0) {
                    closeParen = i;
                    break;
                }
            }
        }
        if (closeParen == -1) return null;

        String beforeParen = h.substring(0, openParen).trim();
        String insideParen = h.substring(openParen + 1, closeParen).trim();

        if (beforeParen.contains("~") || beforeParen.contains("operator")) {
            return null;
        }

        beforeParen = beforeParen.replaceAll("^(?:(?:static|inline|virtual|explicit|constexpr)\\s+)+", "").trim();

        if (beforeParen.equals(className)) {
            return new ParsedMethod("", className, parseCppParams(insideParen), true);
        }

        Matcher nameM = Pattern.compile("^(.*?)\\b([A-Za-z_][A-Za-z0-9_]*)$").matcher(beforeParen);
        if (!nameM.find()) return null;
        String retType = nameM.group(1).trim();
        String methodName = nameM.group(2).trim();
        if (retType.isEmpty() || methodName.equals("if") || methodName.equals("for") || methodName.equals("while")) {
            return null;
        }
        return new ParsedMethod(retType, methodName, parseCppParams(insideParen), false);
    }

    private List<ParsedParam> parseCppParams(String insideParen) {
        List<ParsedParam> params = new ArrayList<>();
        if (insideParen == null || insideParen.isBlank() || "void".equals(insideParen.trim())) {
            return params;
        }
        List<String> parts = splitAngleSafe(insideParen);
        for (String raw : parts) {
            String p = raw.trim();
            if (p.isEmpty()) continue;
            int eq = p.indexOf('=');
            if (eq != -1) p = p.substring(0, eq).trim();
            Matcher pm = Pattern.compile("^(.*?)\\b([A-Za-z_][A-Za-z0-9_]*)$").matcher(p);
            if (pm.find()) {
                String t = pm.group(1).trim();
                String n = pm.group(2).trim();
                if (!t.isEmpty() && !CPP_TYPE_KEYWORDS.contains(n)) {
                    params.add(new ParsedParam(t, n));
                    continue;
                }
            }
            params.add(new ParsedParam(p, ""));
        }
        return params;
    }

    private List<String> splitAngleSafe(String s) {
        List<String> res = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<' || c == '(' || c == '[') depth++;
            else if (c == '>' || c == ')' || c == ']') depth--;
            else if (c == ',' && depth == 0) {
                res.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start <= s.length()) res.add(s.substring(start));
        return res;
    }

    private ParsedMethod selectBestCppMethod(List<ParsedMethod> methods) {
        List<ParsedMethod> nonCtors = new ArrayList<>();
        for (ParsedMethod m : methods) {
            if (!m.isConstructor() && !"main".equals(m.name())) {
                nonCtors.add(m);
            }
        }
        if (nonCtors.isEmpty()) return null;
        if (nonCtors.size() == 1) return nonCtors.get(0);

        for (ParsedMethod m : nonCtors) {
            String lower = m.name().toLowerCase();
            if (!HELPER_METHOD_NAMES.contains(lower) && !lower.startsWith("_")) {
                return m;
            }
        }
        return nonCtors.get(0);
    }

    private String pickCppPublicMethodName(String code, String fallback) {
        ParsedMethod m = selectBestCppMethod(parseCppClassMethods(code, "Solution"));
        return m != null ? m.name() : fallback;
    }

    private String findLastTopLevelCppClassName(String code) {
        String clean = stripCommentsKeepStringsClean(code);
        Matcher m = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)\\b").matcher(clean);
        String last = null;
        while (m.find()) {
            String name = m.group(1);
            if (!"ListNode".equals(name) && !"TreeNode".equals(name) && !"Node".equals(name) && !"MountainArray".equals(name)) {
                last = name;
            }
        }
        return last;
    }

    private String normalizeCppType(String type) {
        if (type == null) return "";
        return type.replace("std::", "")
                   .replace("const", "")
                   .replace("&", "")
                   .replaceAll("\\s+", "");
    }

    private String escapeCppString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String wrapJava(String slug, String code) {
        if (code.contains("public static void main")) {
            return code;
        }

        if (isDesignProblem(slug, code)) {
            return wrapJavaDesign(code);
        }

        return wrapJavaStandard(slug, code);
    }

    private record SanitizedJava(String extraImports, String body) {
    }

    private SanitizedJava extractJavaImportsAndSanitize(String code) {
        StringBuilder imports = new StringBuilder();
        StringBuilder body = new StringBuilder();
        String[] lines = code.split("\\r?\\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
                continue;
            }
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {
                if (!trimmed.contains("javafx.util.Pair")) {
                    imports.append(trimmed).append("\n");
                }
                continue;
            }
            body.append(line).append("\n");
        }
        String sanitized = body.toString().replaceAll("\\bpublic\\s+class\\s+(?!Main\\b)", "class ");
        return new SanitizedJava(imports.toString(), sanitized);
    }

    private String javaPairHelper(String uncommented) {
        if (uncommented.contains("class Pair")) {
            return "";
        }
        return """
class Pair<K, V> {
    private final K key;
    private final V value;
    public Pair(K key, V value) { this.key = key; this.value = value; }
    public K getKey() { return key; }
    public V getValue() { return value; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> p = (Pair<?, ?>) o;
        return Objects.equals(key, p.key) && Objects.equals(value, p.value);
    }
    @Override public int hashCode() { return Objects.hash(key, value); }
    @Override public String toString() { return key + "=" + value; }
}
""";
    }

    private String wrapJavaDesign(String code) {
        String uncommented = stripComments(code);
        SanitizedJava sj = extractJavaImportsAndSanitize(code);
        String helpers = javaPairHelper(uncommented);

        return """
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.lang.reflect.*;
""" + sj.extraImports() + "\n" + helpers + "\n" + sj.body() + """

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (!firstLine) sb.append('\\n');
            sb.append(line);
            firstLine = false;
        }
        String allInput = sb.toString();
        String hdr = "<<<DAPCODE_BATCH_V1>>>";
        if (!allInput.startsWith(hdr)) {
            runSingleCase(allInput.trim());
            return;
        }
        String body = allInput.substring(hdr.length());
        if (body.startsWith("\\r\\n")) body = body.substring(2);
        else if (body.startsWith("\\n")) body = body.substring(1);
        String[] cases = body.split("\\n<<<DAPCODE_CASE_SEP_9F3A>>>\\n", -1);
        PrintStream realOut = System.out;
        for (int ci = 0; ci < cases.length; ci++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, java.nio.charset.StandardCharsets.UTF_8));
            boolean ok = true;
            String err = "";
            long t0 = System.nanoTime();
            try {
                runSingleCase(cases[ci].trim());
            } catch (Throwable t) {
                ok = false;
                Throwable root = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
                err = root.toString();
            }
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            System.setOut(realOut);
            String payload = ok ? baos.toString(java.nio.charset.StandardCharsets.UTF_8) : err;
            while (payload.endsWith("\\n") || payload.endsWith("\\r")) {
                payload = payload.substring(0, payload.length() - 1);
            }
            realOut.printf(java.util.Locale.US, "<<<DAPCODE_RES_START:%d:%s:%.3f>>>%n%s%n<<<DAPCODE_RES_END:%d>>>%n",
                    ci, ok ? "OK" : "ERR", ms, payload, ci);
        }
    }

    private static void runSingleCase(String rawInput) throws Exception {
        if (rawInput.isEmpty()) return;

        List<String> topArrays = parseTopLevelArrays(rawInput);
        if (topArrays.size() < 2) return;

        List<String> rawCommands = parseArrayTokens(topArrays.get(0));
        List<String> rawArgsList = parseArrayTokens(topArrays.get(1));

        List<String> commands = new ArrayList<>();
        for (String c : rawCommands) {
            commands.add(unquote(c));
        }
        if (commands.isEmpty()) return;

        String className = commands.get(0);
        Class<?> clazz = Class.forName(className);

        List<String> ctorRawArgs = parseArrayTokens(rawArgsList.get(0));
        Constructor<?> targetCtor = null;
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterCount() == ctorRawArgs.size()) {
                targetCtor = c;
                break;
            }
        }
        if (targetCtor == null) {
            Constructor<?>[] ctors = clazz.getDeclaredConstructors();
            if (ctors.length > 0) targetCtor = ctors[0];
            else return;
        }
        targetCtor.setAccessible(true);
        Class<?>[] ctorParamTypes = targetCtor.getParameterTypes();
        Type[] ctorGenTypes = targetCtor.getGenericParameterTypes();
        Object[] ctorArgs = new Object[ctorParamTypes.length];
        for (int i = 0; i < ctorParamTypes.length; i++) {
            ctorArgs[i] = parseArg(i < ctorRawArgs.size() ? ctorRawArgs.get(i) : "", ctorParamTypes[i], ctorGenTypes[i]);
        }
        Object instance = targetCtor.newInstance(ctorArgs);

        List<String> results = new ArrayList<>();
        results.add("null");

        for (int i = 1; i < commands.size(); i++) {
            String methodName = commands.get(i);
            String rawArgStr = i < rawArgsList.size() ? rawArgsList.get(i) : "[]";
            List<String> methodRawArgs = parseArrayTokens(rawArgStr);

            Method targetMethod = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == methodRawArgs.size()) {
                    targetMethod = m;
                    break;
                }
            }
            if (targetMethod == null) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equalsIgnoreCase(methodName)) {
                        targetMethod = m;
                        break;
                    }
                }
            }
            if (targetMethod == null) {
                results.add("null");
                continue;
            }
            targetMethod.setAccessible(true);
            Class<?>[] pTypes = targetMethod.getParameterTypes();
            Type[] gpTypes = targetMethod.getGenericParameterTypes();
            Object[] mArgs = new Object[pTypes.length];
            for (int j = 0; j < pTypes.length; j++) {
                mArgs[j] = parseArg(j < methodRawArgs.size() ? methodRawArgs.get(j) : "", pTypes[j], gpTypes[j]);
            }
            Object ret = targetMethod.invoke(instance, mArgs);
            if (targetMethod.getReturnType().equals(void.class)) {
                results.add("null");
            } else {
                results.add(formatDesignResult(ret));
            }
        }

        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            out.append(results.get(i));
            if (i < results.size() - 1) out.append(", ");
        }
        out.append("]");
        System.out.println(out.toString());
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && ((s.startsWith("\\"") && s.endsWith("\\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static List<String> parseTopLevelArrays(String s) {
        List<String> res = new ArrayList<>();
        int depth = 0;
        int start = -1;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\\\' && i + 1 < s.length()) { i++; continue; }
                if (c == quote) quote = 0;
            } else {
                if (c == '"' || c == '\\'') quote = c;
                else if (c == '[') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        res.add(s.substring(start, i + 1).trim());
                        start = -1;
                    }
                }
            }
        }
        return res;
    }

    private static List<String> parseArrayTokens(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) return list;
        s = s.trim();
        int startBracket = s.indexOf('[');
        int endBracket = s.lastIndexOf(']');
        if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) return list;
        String inner = s.substring(startBracket + 1, endBracket).trim();
        if (inner.isEmpty()) return list;

        int depth = 0;
        char quote = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (quote != 0) {
                if (c == '\\\\' && i + 1 < inner.length()) {
                    current.append(c).append(inner.charAt(++i));
                    continue;
                }
                if (c == quote) quote = 0;
                current.append(c);
            } else if (c == '"' || c == '\\'') {
                quote = c;
                current.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                current.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                if (current.length() > 0) {
                    list.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            list.add(current.toString().trim());
        }
        return list;
    }

    private static Object parseArg(String s, Class<?> type, Type genericType) {
        if (s == null) return null;
        s = s.trim();
        if (type == int.class || type == Integer.class) {
            String digits = s.replaceAll("[^0-9-]", "");
            return digits.isEmpty() ? 0 : (int) Long.parseLong(digits);
        }
        if (type == long.class || type == Long.class) {
            String digits = s.replaceAll("[^0-9-]", "");
            return digits.isEmpty() ? 0L : Long.parseLong(digits);
        }
        if (type == double.class || type == Double.class) {
            String digits = s.replaceAll("[^0-9.-]", "");
            return digits.isEmpty() ? 0.0 : Double.parseDouble(digits);
        }
        if (type == boolean.class || type == Boolean.class) {
            return unquote(s).equalsIgnoreCase("true") || s.equals("1");
        }
        if (type == char.class || type == Character.class) {
            String u = unquote(s);
            return u.isEmpty() ? ' ' : u.charAt(0);
        }
        if (type == String.class) {
            return unquote(s);
        }
        if (type == int[].class) {
            List<String> tokens = parseArrayTokens(s);
            int[] arr = new int[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String d = tokens.get(i).replaceAll("[^0-9-]", "");
                arr[i] = d.isEmpty() ? 0 : (int) Long.parseLong(d);
            }
            return arr;
        }
        if (type == int[][].class) {
            List<String> rows = parseArrayTokens(s);
            int[][] arr = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                arr[i] = (int[]) parseArg(rows.get(i), int[].class, int[].class);
            }
            return arr;
        }
        if (type == String[].class) {
            List<String> tokens = parseArrayTokens(s);
            String[] arr = new String[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) arr[i] = unquote(tokens.get(i));
            return arr;
        }
        if (List.class.isAssignableFrom(type)) {
            List<String> tokens = parseArrayTokens(s);
            List<Object> list = new ArrayList<>();
            for (String t : tokens) {
                t = t.trim();
                if (t.startsWith("\\"") || t.startsWith("'")) list.add(unquote(t));
                else {
                    try { list.add(Integer.parseInt(t)); } catch (Exception e) { list.add(t); }
                }
            }
            return list;
        }
        return s;
    }

    private static String formatDesignResult(Object res) {
        if (res == null) return "null";
        if (res instanceof Boolean) return res.toString().toLowerCase();
        if (res instanceof String) return "\\"" + res + "\\"";
        if (res instanceof int[]) return Arrays.toString((int[]) res);
        if (res instanceof double[] || res instanceof Double) return String.valueOf(res);
        if (res instanceof Object[]) return Arrays.deepToString((Object[]) res);
        return res.toString();
    }
}
""";
    }

    private String wrapJavaStandard(String slug, String code) {
        String cleanSlug = slug != null ? slug.toLowerCase().trim() : "";
        String uncommented = stripComments(code);
        SanitizedJava sj = extractJavaImportsAndSanitize(code);

        StringBuilder helpers = new StringBuilder();
        helpers.append(javaPairHelper(uncommented));
        if (!uncommented.contains("class ListNode")) {
            helpers.append("""
class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}
""");
        }
        if (!uncommented.contains("class TreeNode")) {
            helpers.append("""
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode() {}
    TreeNode(int val) { this.val = val; }
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}
""");
        }
        if (!uncommented.contains("class Node")) {
            if ("clone-graph".equals(cleanSlug)) {
                helpers.append("""
class Node {
    public int val;
    public List<Node> neighbors;
    public Node() { val = 0; neighbors = new ArrayList<Node>(); }
    public Node(int _val) { val = _val; neighbors = new ArrayList<Node>(); }
    public Node(int _val, ArrayList<Node> _neighbors) { val = _val; neighbors = _neighbors; }
}
""");
            } else if ("copy-list-with-random-pointer".equals(cleanSlug)) {
                helpers.append("""
class Node {
    int val;
    Node next;
    Node random;
    public Node(int val) { this.val = val; this.next = null; this.random = null; }
}
""");
            } else if ("construct-quad-tree".equals(cleanSlug)) {
                helpers.append("""
class Node {
    public boolean val;
    public boolean isLeaf;
    public Node topLeft;
    public Node topRight;
    public Node bottomLeft;
    public Node bottomRight;
    public Node() { this.val = false; this.isLeaf = false; }
    public Node(boolean val, boolean isLeaf) { this.val = val; this.isLeaf = isLeaf; }
    public Node(boolean val, boolean isLeaf, Node topLeft, Node topRight, Node bottomLeft, Node bottomRight) {
        this.val = val; this.isLeaf = isLeaf;
        this.topLeft = topLeft; this.topRight = topRight;
        this.bottomLeft = bottomLeft; this.bottomRight = bottomRight;
    }
}
""");
            }
        }
        if (!uncommented.contains("class GuessGame")) {
            helpers.append("""
class GuessGame {
    public static int PICK = 0;
    public int guess(int num) {
        if (num > PICK) return -1;
        if (num < PICK) return 1;
        return 0;
    }
}
""");
        }
        if (!uncommented.contains("interface MountainArray") && !uncommented.contains("class MountainArray")) {
            helpers.append("""
interface MountainArray {
    int get(int index);
    int length();
}
class MountainArrayImpl implements MountainArray {
    private final int[] arr;
    MountainArrayImpl(int[] arr) { this.arr = arr; }
    public int get(int index) { return arr[index]; }
    public int length() { return arr.length; }
}
""");
        }

        List<ParsedMethod> javaMethods = parseJavaSolutionMethods(code);
        StringBuilder metaInit = new StringBuilder();
        for (int i = 0; i < javaMethods.size(); i++) {
            ParsedMethod m = javaMethods.get(i);
            String key = m.name() + "#" + m.params().size();
            metaInit.append("        METHOD_ORDER.putIfAbsent(\"").append(escapeCppString(key)).append("\", ").append(i).append(");\n");
            metaInit.append("        METHOD_PARAM_NAMES.putIfAbsent(\"").append(escapeCppString(key)).append("\", new String[]{");
            for (int j = 0; j < m.params().size(); j++) {
                if (j > 0) metaInit.append(", ");
                metaInit.append("\"").append(escapeCppString(m.params().get(j).name())).append("\"");
            }
            metaInit.append("});\n");
        }

        String mainBlock = """

public class Main {
    private static final String SLUG = "__SLUG__";
    private static int POS_CYCLE = -1;
    private static TreeNode ROOT_TREE_REF = null;
    private static final Map<String, Integer> METHOD_ORDER = new HashMap<>();
    private static final Map<String, String[]> METHOD_PARAM_NAMES = new HashMap<>();
    private static final Set<String> HELPER_NAMES = Set.of(
            "dfs", "bfs", "backtrack", "backtracking", "helper", "dfshelper", "bfshelper",
            "solvehelper", "fun", "prec", "swap", "gcd", "canship", "sumofsquares",
            "quicksort", "mergesort", "heapify", "findparent", "union", "findroot",
            "memo", "rec", "traverse", "expandaroundcenter", "expand", "check");

    static {
__META_INIT__    }

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (!firstLine) sb.append('\\n');
            sb.append(line);
            firstLine = false;
        }
        String allInput = sb.toString();
        String hdr = "<<<DAPCODE_BATCH_V1>>>";
        if (!allInput.startsWith(hdr)) {
            runSingleCase(allInput.trim());
            return;
        }
        String body = allInput.substring(hdr.length());
        if (body.startsWith("\\r\\n")) body = body.substring(2);
        else if (body.startsWith("\\n")) body = body.substring(1);
        String[] cases = body.split("\\n<<<DAPCODE_CASE_SEP_9F3A>>>\\n", -1);
        PrintStream realOut = System.out;
        for (int ci = 0; ci < cases.length; ci++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, java.nio.charset.StandardCharsets.UTF_8));
            boolean ok = true;
            String err = "";
            long t0 = System.nanoTime();
            try {
                runSingleCase(cases[ci].trim());
            } catch (Throwable t) {
                ok = false;
                Throwable root = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
                err = root.toString();
            }
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            System.setOut(realOut);
            String payload = ok ? baos.toString(java.nio.charset.StandardCharsets.UTF_8) : err;
            while (payload.endsWith("\\n") || payload.endsWith("\\r")) {
                payload = payload.substring(0, payload.length() - 1);
            }
            realOut.printf(java.util.Locale.US, "<<<DAPCODE_RES_START:%d:%s:%.3f>>>%n%s%n<<<DAPCODE_RES_END:%d>>>%n",
                    ci, ok ? "OK" : "ERR", ms, payload, ci);
        }
    }

    private static void runSingleCase(String rawInput) throws Exception {
        POS_CYCLE = -1;
        ROOT_TREE_REF = null;
        GuessGame.PICK = 0;

        if ("serialize-and-deserialize-binary-tree".equals(SLUG)) {
            Class<?> codecClass = Class.forName("Codec");
            Object ser = codecClass.getDeclaredConstructor().newInstance();
            Object deser = codecClass.getDeclaredConstructor().newInstance();
            Method serM = codecClass.getDeclaredMethod("serialize", TreeNode.class);
            Method deserM = codecClass.getDeclaredMethod("deserialize", String.class);
            serM.setAccessible(true);
            deserM.setAccessible(true);
            Map<String, String> inputMap = extractInputMap(rawInput);
            String firstVal = inputMap.isEmpty() ? "[]" : inputMap.values().iterator().next();
            TreeNode root = (TreeNode) parseArg(firstVal, TreeNode.class, TreeNode.class);
            String data = (String) serM.invoke(ser, root);
            Object ans = deserM.invoke(deser, data);
            printResult(ans);
            return;
        }

        Class<?> solClass;
        try {
            solClass = Class.forName("Solution");
        } catch (ClassNotFoundException e) {
            solClass = Class.forName("Codec");
        }

        if ("encode-and-decode-strings".equals(SLUG)) {
            Object sol = solClass.getDeclaredConstructor().newInstance();
            Method encM = null, decM = null;
            for (Method m : solClass.getDeclaredMethods()) {
                if (m.getName().equals("encode")) encM = m;
                if (m.getName().equals("decode")) decM = m;
            }
            if (encM != null && decM != null) {
                encM.setAccessible(true);
                decM.setAccessible(true);
                Map<String, String> inputMap = extractInputMap(rawInput);
                String firstVal = inputMap.isEmpty() ? "[]" : inputMap.values().iterator().next();
                Object arg0 = parseArg(firstVal, encM.getParameterTypes()[0], encM.getGenericParameterTypes()[0]);
                Object encoded = encM.invoke(sol, arg0);
                Object decoded = decM.invoke(sol, encoded);
                printResult(decoded);
                return;
            }
        }

        Map<String, String> inputMap = extractInputMap(rawInput);
        List<String> positionalValues = new ArrayList<>(inputMap.values());

        Method targetMethod = selectBestMethod(solClass, inputMap, positionalValues);
        if (targetMethod == null) return;
        targetMethod.setAccessible(true);

        Class<?>[] paramTypes = targetMethod.getParameterTypes();
        Type[] genericParamTypes = targetMethod.getGenericParameterTypes();
        String sigKey = targetMethod.getName() + "#" + paramTypes.length;
        String[] paramNames = METHOD_PARAM_NAMES.get(sigKey);

        List<String> rawArgValues = resolveArgValues(inputMap, positionalValues, paramNames, paramTypes.length);

        ROOT_TREE_REF = null;
        Object[] invokeArgs = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            String rawVal = i < rawArgValues.size() ? rawArgValues.get(i) : "";
            invokeArgs[i] = parseArg(rawVal, paramTypes[i], genericParamTypes[i]);
        }

        Object sol = solClass.getDeclaredConstructor().newInstance();
        Object result = targetMethod.invoke(sol, invokeArgs);

        if (targetMethod.getReturnType().equals(void.class)) {
            result = invokeArgs.length > 0 ? invokeArgs[0] : null;
        }

        printResult(result);
    }

    private static Method selectBestMethod(Class<?> solClass, Map<String, String> inputMap, List<String> positionalValues) {
        List<Method> candidates = new ArrayList<>();
        for (Method m : solClass.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()
                    && !m.getName().startsWith("_") && !"main".equals(m.getName())) {
                candidates.add(m);
            }
        }
        if (candidates.isEmpty()) {
            for (Method m : solClass.getDeclaredMethods()) {
                if (!m.isSynthetic() && !m.isBridge() && !"main".equals(m.getName())) {
                    candidates.add(m);
                }
            }
        }
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        Method best = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        int bestOrder = Integer.MAX_VALUE;

        for (Method m : candidates) {
            int score = 0;
            String lower = m.getName().toLowerCase();
            int pCount = m.getParameterCount();
            String sigKey = m.getName() + "#" + pCount;
            int order = METHOD_ORDER.getOrDefault(sigKey, 999);

            if (pCount == positionalValues.size()) score += 100;
            else score -= Math.abs(pCount - positionalValues.size()) * 25;

            if (HELPER_NAMES.contains(lower)) score -= 80;
            if ("ispalindrome".equals(lower) && !SLUG.contains("valid-palindrome")) score -= 80;
            if ("isvalid".equals(lower) && !SLUG.contains("valid-parentheses") && !SLUG.contains("valid-sudoku") && !SLUG.contains("validate")) score -= 80;
            if ("merge".equals(lower) && !SLUG.startsWith("merge-")) score -= 60;
            if ("partition".equals(lower) && !SLUG.contains("partition")) score -= 60;

            String[] pNames = METHOD_PARAM_NAMES.get(sigKey);
            if (pNames != null && pNames.length == pCount && pCount > 0) {
                int matchedNames = 0;
                for (String pn : pNames) {
                    if (pn != null && !pn.isEmpty() && inputMap.containsKey(pn)) matchedNames++;
                }
                if (matchedNames == pCount) score += 50;
                else score += matchedNames * 15;
            }

            List<String> boundVals = resolveArgValues(inputMap, positionalValues, pNames, pCount);
            Class<?>[] pTypes = m.getParameterTypes();
            if (boundVals.size() == pCount) {
                boolean allCompat = true;
                for (int i = 0; i < pCount; i++) {
                    if (!isRawCompatibleWithType(boundVals.get(i), pTypes[i])) {
                        allCompat = false;
                        break;
                    }
                }
                if (allCompat) score += 40;
                else score -= 40;
            }

            if (score > bestScore || (score == bestScore && order < bestOrder)) {
                bestScore = score;
                bestOrder = order;
                best = m;
            }
        }
        return best;
    }

    private static boolean isRawCompatibleWithType(String raw, Class<?> type) {
        if (raw == null) return true;
        String s = raw.trim();
        if (s.isEmpty()) return true;
        boolean isQuoted = (s.startsWith("\\"") && s.endsWith("\\"")) || (s.startsWith("'") && s.endsWith("'"));
        boolean is2D = s.startsWith("[[");
        boolean isArr = s.startsWith("[");
        if (type == String.class) return isQuoted || !isArr;
        if (type == char.class || type == Character.class) return isQuoted;
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == double.class || type == Double.class) {
            return !isArr && !isQuoted;
        }
        if (type == boolean.class || type == Boolean.class) {
            return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false") || s.equals("0") || s.equals("1");
        }
        if (type == int[][].class || type == char[][].class) return isArr;
        if (type.isArray() || List.class.isAssignableFrom(type)
                || type.getSimpleName().equals("ListNode")
                || type.getSimpleName().equals("MountainArray")
                || type.getSimpleName().equals("Node")) {
            return isArr;
        }
        if (type.getSimpleName().equals("TreeNode")) return isArr || s.matches("-?\\\\d+") || "null".equals(s);
        return true;
    }

    private static List<String> resolveArgValues(Map<String, String> inputMap, List<String> positionalValues,
            String[] paramNames, int paramCount) {
        if (paramNames != null && paramNames.length == paramCount && paramCount > 0) {
            boolean allFound = true;
            for (String pn : paramNames) {
                if (pn == null || pn.isEmpty() || !inputMap.containsKey(pn)) {
                    allFound = false;
                    break;
                }
            }
            if (allFound) {
                List<String> ordered = new ArrayList<>(paramCount);
                for (String pn : paramNames) ordered.add(inputMap.get(pn));
                return ordered;
            }
        }
        return positionalValues;
    }

    private static Map<String, String> extractInputMap(String rawInput) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawInput == null || rawInput.isBlank()) return map;

        if (rawInput.matches("(?s).*?\\\\b[a-zA-Z_][a-zA-Z0-9_]*\\\\s*=.*")) {
            String[] parts = rawInput.split("(?:,|\\\\r?\\\\n)\\\\s*(?=[a-zA-Z_][a-zA-Z0-9_]*\\\\s*=)");
            for (String p : parts) {
                int eq = p.indexOf('=');
                if (eq != -1) {
                    String k = p.substring(0, eq).trim();
                    String v = p.substring(eq + 1).trim();
                    if ("pick".equals(k)) {
                        try { GuessGame.PICK = Integer.parseInt(v.replaceAll("[^0-9-]", "")); } catch (Exception ignored) {}
                        continue;
                    }
                    if ("pos".equals(k)) {
                        try { POS_CYCLE = Integer.parseInt(v.replaceAll("[^0-9-]", "")); } catch (Exception ignored) {}
                        continue;
                    }
                    map.put(k, v);
                }
            }
        } else {
            String[] lines = rawInput.split("\\\\r?\\\\n");
            int idx = 0;
            for (String l : lines) {
                if (!l.trim().isEmpty()) map.put("_arg" + (idx++), l.trim());
            }
        }
        return map;
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && ((s.startsWith("\\"") && s.endsWith("\\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            String inner = s.substring(1, s.length() - 1);
            StringBuilder out = new StringBuilder(inner.length());
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\\\' && i + 1 < inner.length()) {
                    char nxt = inner.charAt(++i);
                    if (nxt == 'n') out.append('\\n');
                    else if (nxt == 't') out.append('\\t');
                    else if (nxt == 'r') out.append('\\r');
                    else out.append(nxt);
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }
        return s;
    }

    private static TreeNode findTreeNode(TreeNode root, int val) {
        if (root == null) return null;
        if (root.val == val) return root;
        TreeNode left = findTreeNode(root.left, val);
        return left != null ? left : findTreeNode(root.right, val);
    }

    private static Object parseNodeArg(String s, Class<?> nodeClass) {
        try {
            if ("clone-graph".equals(SLUG)) {
                int[][] adj = (int[][]) parseArg(s, int[][].class, int[][].class);
                if (adj.length == 0) return null;
                Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
                ctor.setAccessible(true);
                Field nbField = nodeClass.getDeclaredField("neighbors");
                nbField.setAccessible(true);
                Object[] nodes = new Object[adj.length];
                for (int i = 0; i < adj.length; i++) nodes[i] = ctor.newInstance(i + 1);
                for (int i = 0; i < adj.length; i++) {
                    @SuppressWarnings("unchecked")
                    List<Object> nbs = (List<Object>) nbField.get(nodes[i]);
                    for (int nb : adj[i]) {
                        if (nb >= 1 && nb <= adj.length) nbs.add(nodes[nb - 1]);
                    }
                }
                return nodes[0];
            }
            if ("copy-list-with-random-pointer".equals(SLUG)) {
                List<String> rows = parseArrayTokens(s);
                if (rows.isEmpty()) return null;
                Constructor<?> ctor = nodeClass.getDeclaredConstructor(int.class);
                ctor.setAccessible(true);
                Field nextField = nodeClass.getDeclaredField("next");
                Field randField = nodeClass.getDeclaredField("random");
                nextField.setAccessible(true);
                randField.setAccessible(true);
                Object[] nodes = new Object[rows.size()];
                int[] rIdx = new int[rows.size()];
                for (int i = 0; i < rows.size(); i++) {
                    List<String> pair = parseArrayTokens(rows.get(i));
                    int val = pair.size() > 0 ? Integer.parseInt(pair.get(0).replaceAll("[^0-9-]", "")) : 0;
                    nodes[i] = ctor.newInstance(val);
                    rIdx[i] = -1;
                    if (pair.size() > 1 && !pair.get(1).trim().equals("null") && !pair.get(1).trim().equals("None")) {
                        rIdx[i] = Integer.parseInt(pair.get(1).replaceAll("[^0-9-]", ""));
                    }
                }
                for (int i = 0; i + 1 < nodes.length; i++) nextField.set(nodes[i], nodes[i + 1]);
                for (int i = 0; i < nodes.length; i++) {
                    if (rIdx[i] >= 0 && rIdx[i] < nodes.length) randField.set(nodes[i], nodes[rIdx[i]]);
                }
                return nodes[0];
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Object parseArg(String s, Class<?> type, Type genericType) {
        if (s == null) return null;
        s = s.trim();
        if (type == int.class || type == Integer.class) {
            String u = unquote(s);
            if (u.matches("[01]{32}")) return (int) Long.parseLong(u, 2);
            String digits = u.replaceAll("[^0-9-]", "");
            return digits.isEmpty() ? 0 : (int) Long.parseLong(digits);
        }
        if (type == long.class || type == Long.class) {
            String digits = unquote(s).replaceAll("[^0-9-]", "");
            return digits.isEmpty() ? 0L : Long.parseLong(digits);
        }
        if (type == double.class || type == Double.class) {
            String digits = unquote(s).replaceAll("[^0-9.eE+-]", "");
            return digits.isEmpty() ? 0.0 : Double.parseDouble(digits);
        }
        if (type == boolean.class || type == Boolean.class) {
            String u = unquote(s);
            return u.equalsIgnoreCase("true") || u.equals("1");
        }
        if (type == char.class || type == Character.class) {
            String u = unquote(s);
            return u.isEmpty() ? ' ' : u.charAt(0);
        }
        if (type == String.class) {
            return unquote(s);
        }
        if (type == int[].class) {
            List<String> tokens = parseArrayTokens(s);
            int[] arr = new int[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String d = tokens.get(i).replaceAll("[^0-9-]", "");
                arr[i] = d.isEmpty() ? 0 : (int) Long.parseLong(d);
            }
            return arr;
        }
        if (type == long[].class) {
            List<String> tokens = parseArrayTokens(s);
            long[] arr = new long[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String d = tokens.get(i).replaceAll("[^0-9-]", "");
                arr[i] = d.isEmpty() ? 0L : Long.parseLong(d);
            }
            return arr;
        }
        if (type == double[].class) {
            List<String> tokens = parseArrayTokens(s);
            double[] arr = new double[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String d = tokens.get(i).replaceAll("[^0-9.eE+-]", "");
                arr[i] = d.isEmpty() ? 0.0 : Double.parseDouble(d);
            }
            return arr;
        }
        if (type == boolean[].class) {
            List<String> tokens = parseArrayTokens(s);
            boolean[] arr = new boolean[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String u = unquote(tokens.get(i));
                arr[i] = u.equalsIgnoreCase("true") || u.equals("1");
            }
            return arr;
        }
        if (type == int[][].class) {
            List<String> rows = parseArrayTokens(s);
            int[][] arr = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                arr[i] = (int[]) parseArg(rows.get(i), int[].class, int[].class);
            }
            return arr;
        }
        if (type == char[].class) {
            List<String> tokens = parseArrayTokens(s);
            char[] arr = new char[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String u = unquote(tokens.get(i));
                arr[i] = u.isEmpty() ? ' ' : u.charAt(0);
            }
            return arr;
        }
        if (type == char[][].class) {
            List<String> rows = parseArrayTokens(s);
            char[][] arr = new char[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                arr[i] = (char[]) parseArg(rows.get(i), char[].class, char[].class);
            }
            return arr;
        }
        if (type == String[].class) {
            List<String> tokens = parseArrayTokens(s);
            String[] arr = new String[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                arr[i] = unquote(tokens.get(i));
            }
            return arr;
        }
        if (type.getSimpleName().equals("MountainArray")) {
            int[] arr = (int[]) parseArg(s, int[].class, int[].class);
            return new MountainArrayImpl(arr);
        }
        if (type.getSimpleName().equals("Node")) {
            return parseNodeArg(s, type);
        }
        if (type.getSimpleName().equals("ListNode[]")) {
            List<String> rows = parseArrayTokens(s);
            ListNode[] arr = new ListNode[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                arr[i] = (ListNode) parseArg(rows.get(i), ListNode.class, ListNode.class);
            }
            return arr;
        }
        if (List.class.isAssignableFrom(type)) {
            List<String> tokens = parseArrayTokens(s);
            if (!tokens.isEmpty() && tokens.get(0).trim().startsWith("[")) {
                List<Object> outer = new ArrayList<>();
                for (String row : tokens) {
                    outer.add(parseArg(row, List.class, null));
                }
                return outer;
            }
            List<Object> list = new ArrayList<>();
            for (String t : tokens) {
                t = t.trim();
                if ((t.startsWith("\\"") && t.endsWith("\\"")) || (t.startsWith("'") && t.endsWith("'"))) {
                    list.add(unquote(t));
                } else if (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false")) {
                    list.add(Boolean.parseBoolean(t));
                } else {
                    try {
                        list.add(Integer.parseInt(t));
                    } catch (Exception e) {
                        list.add(t);
                    }
                }
            }
            return list;
        }
        if (type.getSimpleName().equals("ListNode")) {
            List<String> tokens = parseArrayTokens(s);
            ListNode dummy = new ListNode(0);
            ListNode curr = dummy;
            List<ListNode> nodes = new ArrayList<>();
            for (String t : tokens) {
                String d = t.replaceAll("[^0-9-]", "");
                if (!d.isEmpty()) {
                    curr.next = new ListNode(Integer.parseInt(d));
                    curr = curr.next;
                    nodes.add(curr);
                }
            }
            if (POS_CYCLE >= 0 && POS_CYCLE < nodes.size() && !nodes.isEmpty()) {
                nodes.get(nodes.size() - 1).next = nodes.get(POS_CYCLE);
            }
            return dummy.next;
        }
        if (type.getSimpleName().equals("TreeNode")) {
            if (!s.contains("[")) {
                String d = s.replaceAll("[^0-9-]", "");
                if (d.isEmpty()) return null;
                int v = Integer.parseInt(d);
                if (ROOT_TREE_REF != null) {
                    TreeNode found = findTreeNode(ROOT_TREE_REF, v);
                    if (found != null) return found;
                }
                return new TreeNode(v);
            }
            List<String> tokens = parseArrayTokens(s);
            if (tokens.isEmpty() || tokens.get(0).trim().equals("null") || tokens.get(0).trim().equals("None")) return null;
            TreeNode root = new TreeNode(Integer.parseInt(tokens.get(0).replaceAll("[^0-9-]", "")));
            Queue<TreeNode> queue = new LinkedList<>();
            queue.offer(root);
            int idx = 1;
            while (!queue.isEmpty() && idx < tokens.size()) {
                TreeNode curr = queue.poll();
                if (idx < tokens.size()) {
                    String t = tokens.get(idx++).trim();
                    if (!t.equals("null") && !t.equals("None")) {
                        curr.left = new TreeNode(Integer.parseInt(t.replaceAll("[^0-9-]", "")));
                        queue.offer(curr.left);
                    }
                }
                if (idx < tokens.size()) {
                    String t = tokens.get(idx++).trim();
                    if (!t.equals("null") && !t.equals("None")) {
                        curr.right = new TreeNode(Integer.parseInt(t.replaceAll("[^0-9-]", "")));
                        queue.offer(curr.right);
                    }
                }
            }
            if (ROOT_TREE_REF == null) ROOT_TREE_REF = root;
            return root;
        }
        return s;
    }

    private static List<String> parseArrayTokens(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) return list;
        s = s.trim();
        int startBracket = s.indexOf('[');
        int endBracket = s.lastIndexOf(']');
        if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) return list;
        String inner = s.substring(startBracket + 1, endBracket).trim();
        if (inner.isEmpty()) return list;

        int depth = 0;
        char quote = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (quote != 0) {
                if (c == '\\\\' && i + 1 < inner.length()) {
                    current.append(c).append(inner.charAt(++i));
                    continue;
                }
                if (c == quote) quote = 0;
                current.append(c);
            } else if (c == '"' || c == '\\'') {
                quote = c;
                current.append(c);
            } else if (c == '[' || c == '{') {
                depth++;
                current.append(c);
            } else if (c == ']' || c == '}') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                if (current.length() > 0) {
                    list.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            list.add(current.toString().trim());
        }
        return list;
    }

    private static void printNodeResult(Object res) {
        try {
            if ("clone-graph".equals(SLUG)) {
                Field valF = res.getClass().getDeclaredField("val");
                Field nbF = res.getClass().getDeclaredField("neighbors");
                valF.setAccessible(true);
                nbF.setAccessible(true);
                Map<Integer, List<Integer>> visited = new TreeMap<>();
                Queue<Object> q = new LinkedList<>();
                q.offer(res);
                int mx = 0;
                while (!q.isEmpty()) {
                    Object curr = q.poll();
                    int v = (Integer) valF.get(curr);
                    if (visited.containsKey(v)) continue;
                    mx = Math.max(mx, v);
                    List<Integer> nbs = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<Object> rawNbs = (List<Object>) nbF.get(curr);
                    if (rawNbs != null) {
                        for (Object nb : rawNbs) {
                            int nv = (Integer) valF.get(nb);
                            nbs.add(nv);
                            if (!visited.containsKey(nv)) q.offer(nb);
                        }
                    }
                    visited.put(v, nbs);
                }
                List<List<Integer>> out = new ArrayList<>();
                for (int i = 1; i <= mx; i++) out.add(visited.getOrDefault(i, List.of()));
                System.out.print(out.toString());
                return;
            }
            if ("copy-list-with-random-pointer".equals(SLUG)) {
                Field valF = res.getClass().getDeclaredField("val");
                Field nextF = res.getClass().getDeclaredField("next");
                Field randF = res.getClass().getDeclaredField("random");
                valF.setAccessible(true);
                nextF.setAccessible(true);
                randF.setAccessible(true);
                List<Object> nodes = new ArrayList<>();
                IdentityHashMap<Object, Integer> idxMap = new IdentityHashMap<>();
                Object curr = res;
                while (curr != null && !idxMap.containsKey(curr)) {
                    idxMap.put(curr, nodes.size());
                    nodes.add(curr);
                    curr = nextF.get(curr);
                }
                StringBuilder out = new StringBuilder("[");
                for (int i = 0; i < nodes.size(); i++) {
                    if (i > 0) out.append(", ");
                    Object n = nodes.get(i);
                    Object r = randF.get(n);
                    out.append("[").append(valF.get(n)).append(", ")
                       .append(r != null && idxMap.containsKey(r) ? idxMap.get(r) : "null").append("]");
                }
                out.append("]");
                System.out.print(out.toString());
                return;
            }
            if ("construct-quad-tree".equals(SLUG)) {
                Field valF = res.getClass().getDeclaredField("val");
                Field leafF = res.getClass().getDeclaredField("isLeaf");
                Field tlF = res.getClass().getDeclaredField("topLeft");
                Field trF = res.getClass().getDeclaredField("topRight");
                Field blF = res.getClass().getDeclaredField("bottomLeft");
                Field brF = res.getClass().getDeclaredField("bottomRight");
                valF.setAccessible(true); leafF.setAccessible(true);
                tlF.setAccessible(true); trF.setAccessible(true);
                blF.setAccessible(true); brF.setAccessible(true);
                List<String> out = new ArrayList<>();
                Queue<Object> q = new LinkedList<>();
                q.offer(res);
                while (!q.isEmpty()) {
                    Object curr = q.poll();
                    if (curr == null) {
                        out.add("null");
                    } else {
                        boolean isLeaf = (Boolean) leafF.get(curr);
                        boolean val = (Boolean) valF.get(curr);
                        out.add("[" + (isLeaf ? 1 : 0) + ", " + (isLeaf ? (val ? 1 : 0) : 1) + "]");
                        if (!isLeaf) {
                            q.offer(tlF.get(curr)); q.offer(trF.get(curr));
                            q.offer(blF.get(curr)); q.offer(brF.get(curr));
                        } else {
                            q.offer(null); q.offer(null); q.offer(null); q.offer(null);
                        }
                    }
                }
                while (!out.isEmpty() && out.get(out.size() - 1).equals("null")) out.remove(out.size() - 1);
                System.out.print(out.toString());
                return;
            }
        } catch (Exception ignored) {}
        System.out.print("[]");
    }

    private static void printResult(Object res) {
        if (res == null) {
            if ("clone-graph".equals(SLUG) || "copy-list-with-random-pointer".equals(SLUG)
                    || "construct-quad-tree".equals(SLUG)) {
                System.out.print("[]");
            } else {
                System.out.print("null");
            }
        } else if (res instanceof Boolean) {
            System.out.print((Boolean) res ? "True" : "False");
        } else if (res instanceof int[]) {
            System.out.print(Arrays.toString((int[]) res));
        } else if (res instanceof long[]) {
            System.out.print(Arrays.toString((long[]) res));
        } else if (res instanceof double[]) {
            System.out.print(Arrays.toString((double[]) res));
        } else if (res instanceof boolean[]) {
            boolean[] arr = (boolean[]) res;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(arr[i] ? "True" : "False");
            }
            sb.append("]");
            System.out.print(sb.toString());
        } else if (res instanceof char[]) {
            char[] arr = (char[]) res;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\\"").append(arr[i]).append("\\"");
            }
            sb.append("]");
            System.out.print(sb.toString());
        } else if (res instanceof char[][]) {
            char[][] grid = (char[][]) res;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < grid.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("[");
                for (int j = 0; j < grid[i].length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append("\\"").append(grid[i][j]).append("\\"");
                }
                sb.append("]");
            }
            sb.append("]");
            System.out.print(sb.toString());
        } else if (res instanceof Object[]) {
            System.out.print(Arrays.deepToString((Object[]) res));
        } else if (res instanceof List) {
            System.out.print(res.toString());
        } else if (res.getClass().getSimpleName().equals("Node")) {
            printNodeResult(res);
        } else if (res.getClass().getSimpleName().equals("ListNode")) {
            List<Integer> list = new ArrayList<>();
            try {
                Field valField = res.getClass().getDeclaredField("val");
                Field nextField = res.getClass().getDeclaredField("next");
                valField.setAccessible(true);
                nextField.setAccessible(true);
                Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
                Object curr = res;
                while (curr != null && visited.add(curr)) {
                    list.add((Integer) valField.get(curr));
                    curr = nextField.get(curr);
                }
            } catch (Exception ignored) {}
            System.out.print(list.toString());
        } else if (res.getClass().getSimpleName().equals("TreeNode")) {
            try {
                Field valField = res.getClass().getDeclaredField("val");
                valField.setAccessible(true);
                if ("lowest-common-ancestor-of-a-binary-search-tree".equals(SLUG)) {
                    System.out.print(valField.get(res));
                    return;
                }
                Field leftField = res.getClass().getDeclaredField("left");
                Field rightField = res.getClass().getDeclaredField("right");
                leftField.setAccessible(true);
                rightField.setAccessible(true);

                List<String> list = new ArrayList<>();
                Queue<Object> q = new LinkedList<>();
                q.offer(res);
                while (!q.isEmpty()) {
                    Object curr = q.poll();
                    if (curr == null) {
                        list.add("null");
                    } else {
                        list.add(String.valueOf(valField.get(curr)));
                        q.offer(leftField.get(curr));
                        q.offer(rightField.get(curr));
                    }
                }
                while (!list.isEmpty() && list.get(list.size() - 1).equals("null")) {
                    list.remove(list.size() - 1);
                }
                System.out.print(list.toString());
            } catch (Exception ignored) {}
        } else {
            System.out.print(res.toString());
        }
    }
}
""".replace("__SLUG__", cleanSlug.replace("\"", ""))
   .replace("__META_INIT__", metaInit.toString());

        return """
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.lang.reflect.*;
""" + sj.extraImports() + "\n" + helpers + "\n" + sj.body() + mainBlock;
    }

    private List<ParsedMethod> parseJavaSolutionMethods(String code) {
        List<ParsedMethod> result = new ArrayList<>();
        String clean = stripCommentsKeepStringsClean(code);
        Matcher cm = Pattern.compile("\\bclass\\s+Solution\\b[^{]*\\{").matcher(clean);
        if (!cm.find()) {
            return result;
        }
        int pos = cm.end();
        int depth = 1;
        StringBuilder stmt = new StringBuilder();
        while (pos < clean.length() && depth > 0) {
            char c = clean.charAt(pos++);
            if (c == '{') {
                if (depth == 1) {
                    String header = stmt.toString().replaceAll("\\s+", " ").trim();
                    stmt.setLength(0);
                    ParsedMethod pm = parseJavaMethodHeader(header);
                    if (pm != null) result.add(pm);
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 1) stmt.setLength(0);
            } else if (depth == 1) {
                if (c == ';') stmt.setLength(0);
                else stmt.append(c);
            }
        }
        return result;
    }

    private ParsedMethod parseJavaMethodHeader(String header) {
        if (header.isEmpty() || header.contains("class ") || header.contains("interface ") || header.contains("enum ")) {
            return null;
        }
        int openParen = header.indexOf('(');
        if (openParen == -1) return null;
        int closeParen = header.lastIndexOf(')');
        if (closeParen == -1 || closeParen < openParen) return null;
        String beforeParen = header.substring(0, openParen).trim();
        String insideParen = header.substring(openParen + 1, closeParen).trim();
        beforeParen = beforeParen.replaceAll("^(?:(?:public|private|protected|static|final|synchronized|abstract)\\s+)+", "").trim();
        if (beforeParen.equals("Solution")) return null;
        Matcher nm = Pattern.compile("^(.*?)\\b([A-Za-z_][A-Za-z0-9_]*)$").matcher(beforeParen);
        if (!nm.find()) return null;
        String retType = nm.group(1).trim();
        String name = nm.group(2).trim();
        if (retType.isEmpty()) return null;
        List<ParsedParam> params = new ArrayList<>();
        if (!insideParen.isBlank()) {
            for (String raw : splitAngleSafe(insideParen)) {
                String p = raw.replaceAll("\\bfinal\\s+", "").trim();
                if (p.isEmpty()) continue;
                Matcher pm = Pattern.compile("^(.*?)\\b([A-Za-z_][A-Za-z0-9_]*)$").matcher(p);
                if (pm.find()) {
                    params.add(new ParsedParam(pm.group(1).trim(), pm.group(2).trim()));
                } else {
                    params.add(new ParsedParam(p, ""));
                }
            }
        }
        return new ParsedMethod(retType, name, params, false);
    }
}
