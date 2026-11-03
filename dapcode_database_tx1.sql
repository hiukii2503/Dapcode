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

CREATE INDEX idx_theories_category ON theories(category);
CREATE INDEX idx_theories_slug ON theories(slug);
CREATE INDEX idx_test_cases_problem_id ON test_cases(problem_id);

INSERT INTO users (id, username, email, password_hash, role, is_active, xp, streak_count, avatar_url)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    'admin@dapcode.io',
    'admin123',
    'ROLE_ADMIN',
    TRUE,
    9999,
    30,
    'https://api.dicebear.com/7.x/bottts/svg?seed=admin'
);

INSERT INTO problems (id, slug, title, difficulty, category, description_markdown, time_limit_ms, memory_limit_kb, is_published)
VALUES 
(
    'b0000000-0000-0000-0000-000000000001',
    'two-sum',
    'Two Sum',
    'EASY',
    'Arrays & Hashing',
    'Cho một mảng số nguyên nums và một số nguyên target, hãy trả về chỉ số của hai số sao cho tổng của chúng bằng target.',
    1000,
    131072,
    TRUE
),
(
    'b0000000-0000-0000-0000-000000000002',
    'valid-anagram',
    'Valid Anagram',
    'EASY',
    'Arrays & Hashing',
    'Cho hai chuỗi s và t, trả về true nếu t là một chuỗi đảo chữ của s, và false nếu ngược lại.',
    1000,
    131072,
    TRUE
);

INSERT INTO test_cases (id, problem_id, input_data, expected_output, is_hidden)
VALUES 
(
    'c0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    '[2,7,11,15]\n9',
    '[0,1]',
    FALSE
),
(
    'c0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000001',
    '[3,2,4]\n6',
    '[1,2]',
    FALSE
),
(
    'c0000000-0000-0000-0000-000000000003',
    'b0000000-0000-0000-0000-000000000001',
    '[3,3]\n6',
    '[0,1]',
    TRUE
);

INSERT INTO theories (id, title, slug, category, summary, content_markdown, order_index, is_published)
VALUES 
(
    'd0000000-0000-0000-0000-000000000001',
    'Tổng quan Mảng và Bảng Băm',
    'tong-quan-mang-va-bang-bam',
    'Arrays & Hashing',
    'Tổng quan về cấu trúc mảng và bảng băm',
    '# Mảng & Bảng Băm\n\n## 1. Mảng (Array)\nMảng là tập hợp các phần tử liên tiếp trong bộ nhớ. Truy cập theo chỉ mục có độ phức tạp O(1).\n\n## 2. Bảng Băm (Hash Table)\nSử dụng hàm băm (Hash Function) để ánh xạ khóa thành chỉ mục, giúp tìm kiếm và chèn trong thời gian trung bình O(1).',
    1,
    TRUE
),
(
    'd0000000-0000-0000-0000-000000000002',
    'Kỹ thuật Hai Con Trỏ',
    'ky-thuat-hai-con-tro',
    'Two Pointers',
    'Cách sử dụng hai con trỏ duyệt mảng để tối ưu độ phức tạp',
    '# Kỹ thuật Hai Con Trỏ\n\n## Nguyên lý hoạt động\nThường áp dụng trên mảng đã sắp xếp: một con trỏ ở đầu (Left), một con trỏ ở cuối (Right) di chuyển dần vào giữa.',
    2,
    TRUE
),
(
    'd0000000-0000-0000-0000-000000000003',
    'Cây Nhị Phân Tìm Kiếm',
    'cay-nhi-phan-tim-kiem',
    'Trees',
    'Cấu trúc cây nhị phân tìm kiếm cơ bản',
    '# Binary Search Tree\n\n## Tính chất\n- Mọi nút con bên trái đều nhỏ hơn nút cha.\n- Mọi nút con bên phải đều lớn hơn nút cha.\n- Độ phức tạp tìm kiếm trung bình: O(log N).',
    3,
    TRUE
);
