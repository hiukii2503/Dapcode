DROP TABLE IF EXISTS submissions CASCADE;
DROP TABLE IF EXISTS test_cases CASCADE;
DROP TABLE IF EXISTS theories CASCADE;
DROP TABLE IF EXISTS problems CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    banned_reason VARCHAR(255),
    google_id VARCHAR(100),
    avatar_url TEXT,
    xp INT NOT NULL DEFAULT 0,
    streak_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    description_markdown TEXT NOT NULL,
    time_limit_ms INT NOT NULL DEFAULT 2000,
    memory_limit_kb INT NOT NULL DEFAULT 262144,
    starter_code TEXT,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE theories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    category VARCHAR(100) NOT NULL,
    summary TEXT,
    content_markdown TEXT NOT NULL,
    order_index INT DEFAULT 0,
    is_published BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    source_code TEXT NOT NULL,
    language_id INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_runtime_ms DOUBLE PRECISION,
    max_memory_kb INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_problems_slug ON problems(slug);
CREATE INDEX idx_problems_category_difficulty ON problems(category, difficulty);
CREATE INDEX idx_test_cases_problem_id ON test_cases(problem_id);
CREATE INDEX idx_theories_category ON theories(category);
CREATE INDEX idx_theories_slug ON theories(slug);
CREATE INDEX idx_submissions_user ON submissions(user_id);
CREATE INDEX idx_submissions_problem ON submissions(problem_id);
CREATE INDEX idx_submissions_created_at ON submissions(created_at DESC);

INSERT INTO users (id, username, email, password_hash, role, is_active, xp, streak_count, avatar_url)
VALUES
(
    '0529d450-19bc-4762-931b-28c7daf3d6ec',
    'admin',
    'admin@dapcode.io',
    '$2a$10$Mreo5nJGsNmktHMClkEM8uGdWPSICUzt7Fzd9keMX5AEdF1.qBq0W',
    'ROLE_ADMIN',
    TRUE,
    9999,
    30,
    'https://api.dicebear.com/7.x/bottts/svg?seed=dapcode_admin'
),
(
    'a0000000-0000-0000-0000-000000000002',
    'student01',
    'student01@dapcode.io',
    '$2a$10$Mreo5nJGsNmktHMClkEM8uGdWPSICUzt7Fzd9keMX5AEdF1.qBq0W',
    'ROLE_USER',
    TRUE,
    120,
    5,
    'https://api.dicebear.com/7.x/avataaars/svg?seed=student01'
);

INSERT INTO problems (id, slug, title, difficulty, category, description_markdown, time_limit_ms, memory_limit_kb, starter_code, is_published)
VALUES
(
    'b0000000-0000-0000-0000-000000000001',
    'two-sum',
    'Two Sum',
    'Easy',
    'Arrays & Hashing',
    'Cho một mảng số nguyên `nums` và một số nguyên `target`, hãy trả về chỉ số của hai số sao cho tổng của chúng bằng `target`.',
    2000,
    262144,
    '{"python":"class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        pass","java":"class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{};\n    }\n}","cpp":"class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {};\n    }\n};"}',
    TRUE
),
(
    'b0000000-0000-0000-0000-000000000002',
    'valid-anagram',
    'Valid Anagram',
    'Easy',
    'Arrays & Hashing',
    'Cho hai chuỗi `s` và `t`, trả về `true` nếu `t` là một chuỗi đảo chữ của `s`, và `false` nếu ngược lại.',
    2000,
    262144,
    '{"python":"class Solution:\n    def isAnagram(self, s: str, t: str) -> bool:\n        pass","java":"class Solution {\n    public boolean isAnagram(String s, String t) {\n        return false;\n    }\n}","cpp":"class Solution {\npublic:\n    bool isAnagram(string s, string t) {\n        return false;\n    }\n};"}',
    TRUE
),
(
    'b0000000-0000-0000-0000-000000000003',
    'maximum-subarray',
    'Maximum Subarray',
    'Medium',
    'Dynamic Programming',
    'Cho mảng số nguyên `nums`, hãy tìm mảng con liên tiếp (chứa ít nhất 1 phần tử) có tổng lớn nhất và trả về tổng đó.',
    2000,
    262144,
    '{"python":"class Solution:\n    def maxSubArray(self, nums: list[int]) -> int:\n        pass","java":"class Solution {\n    public int maxSubArray(int[] nums) {\n        return 0;\n    }\n}","cpp":"class Solution {\npublic:\n    int maxSubArray(vector<int>& nums) {\n        return 0;\n    }\n};"}',
    TRUE
);

INSERT INTO test_cases (id, problem_id, input_data, expected_output, is_hidden)
VALUES
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', E'[2,7,11,15]\n9', '[0,1]', FALSE),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', E'[3,2,4]\n6', '[1,2]', FALSE),
('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', E'[3,3]\n6', '[0,1]', TRUE),
('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', E'"anagram"\n"nagaram"', 'true', FALSE),
('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000002', E'"rat"\n"car"', 'false', TRUE),
('c0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000003', '[-2,1,-3,4,-1,2,1,-5,4]', '6', FALSE);

INSERT INTO theories (id, title, slug, category, summary, content_markdown, order_index, is_published)
VALUES
(
    'd0000000-0000-0000-0000-000000000001',
    'Tổng quan Mảng và Bảng Băm',
    'tong-quan-mang-va-bang-bam',
    'Arrays & Hashing',
    'Tổng quan về cấu trúc mảng và bảng băm (Hash Table)',
    E'# Mảng & Bảng Băm\n\n## 1. Mảng (Array)\nMảng là tập hợp các phần tử liên tiếp trong bộ nhớ. Truy cập theo chỉ mục có độ phức tạp O(1).\n\n## 2. Bảng Băm (Hash Table)\nSử dụng hàm băm (Hash Function) để ánh xạ khóa thành chỉ mục, giúp tìm kiếm và chèn trong thời gian trung bình O(1).',
    1,
    TRUE
),
(
    'd0000000-0000-0000-0000-000000000002',
    'Kỹ thuật Hai Con Trỏ',
    'ky-thuat-hai-con-tro',
    'Two Pointers',
    'Cách sử dụng hai con trỏ duyệt mảng để tối ưu độ phức tạp xuống O(N)',
    E'# Kỹ thuật Hai Con Trỏ\n\n## Nguyên lý hoạt động\nThường áp dụng trên mảng đã sắp xếp: một con trỏ ở đầu (Left), một con trỏ ở cuối (Right) di chuyển dần vào giữa.',
    2,
    TRUE
),
(
    'd0000000-0000-0000-0000-000000000003',
    'Cây Nhị Phân Tìm Kiếm',
    'cay-nhi-phan-tim-kiem',
    'Trees',
    'Cấu trúc cây nhị phân tìm kiếm cơ bản (Binary Search Tree)',
    E'# Binary Search Tree\n\n## Tính chất\n- Mọi nút con bên trái đều nhỏ hơn nút cha.\n- Mọi nút con bên phải đều lớn hơn nút cha.\n- Độ phức tạp tìm kiếm trung bình: O(log N).',
    3,
    TRUE
);

INSERT INTO submissions (id, user_id, problem_id, source_code, language_id, status, total_runtime_ms, max_memory_kb)
VALUES
(
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000001',
    E'class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        seen = {}\n        for i, n in enumerate(nums):\n            if target - n in seen:\n                return [seen[target - n], i]\n            seen[n] = i',
    71,
    'ACCEPTED',
    18.5,
    14200
),
(
    'e0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000002',
    E'class Solution:\n    def isAnagram(self, s: str, t: str) -> bool:\n        return len(s) == len(t)',
    71,
    'WRONG_ANSWER',
    12.0,
    13800
);
