package com.dapcode.backend_core.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.dapcode.backend_core.entity.Problem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProblemDAOIntegrationTest {

    private ProblemDAO problemDAO;

    @BeforeEach
    void setUp() {
        problemDAO = new ProblemDAOImpl();
    }

    @Test
    @Order(1)
    @DisplayName("Kịch bản 1: Luồng tích hợp CRUD bài tập (Create -> Read -> Update -> Delete) trên DB thật qua HikariCP")
    void test1_CRUD_Flow() {
        log.info("=== [Kịch bản 1] Bắt đầu kiểm thử luồng CRUD bài tập qua HikariCP ===");
        String uniqueSlug = "integration-crud-" + System.currentTimeMillis();
        Problem newProblem = Problem.builder()
                .title("Bài tập Kiểm thử Tích hợp CRUD")
                .slug(uniqueSlug)
                .difficulty("MEDIUM")
                .category("Arrays")
                .descriptionMarkdown("Mô tả bài tập kiểm thử CRUD trên HikariCP")
                .timeLimitMs(2000)
                .memoryLimitKb(262144)
                .isPublished(true)
                .build();

        log.info("[CREATE] Đang thêm bài tập: title='{}', slug='{}', difficulty='{}', category='{}'",
                newProblem.getTitle(), newProblem.getSlug(), newProblem.getDifficulty(), newProblem.getCategory());
        Problem inserted = problemDAO.insert(newProblem);
        assertNotNull(inserted, "Thêm bài tập qua HikariCP phải thành công");
        assertNotNull(inserted.getId(), "ID phải được tạo tự động");
        log.info("[CREATE] Thêm thành công -> id={}, slug='{}'", inserted.getId(), inserted.getSlug());

        Optional<Problem> foundById = problemDAO.findById(inserted.getId());
        assertTrue(foundById.isPresent(), "Phải tìm thấy bài tập theo ID");
        assertEquals("Bài tập Kiểm thử Tích hợp CRUD", foundById.get().getTitle());
        log.info("[READ - findById] Tìm thấy bài tập: id={}, title='{}'",
                foundById.get().getId(), foundById.get().getTitle());

        Optional<Problem> foundBySlug = problemDAO.findBySlug(uniqueSlug);
        assertTrue(foundBySlug.isPresent(), "Phải tìm thấy bài tập theo Slug");
        log.info("[READ - findBySlug] Tìm thấy bài tập theo slug='{}' -> id={}",
                uniqueSlug, foundBySlug.get().getId());

        inserted.setTitle("Bài tập Kiểm thử CRUD (Đã sửa)");
        inserted.setDifficulty("HARD");
        Problem updated = problemDAO.update(inserted);
        assertNotNull(updated, "Cập nhật bài tập phải thành công");
        assertEquals("Bài tập Kiểm thử CRUD (Đã sửa)", updated.getTitle());
        assertEquals("HARD", updated.getDifficulty());
        log.info("[UPDATE] Cập nhật thành công id={} -> title='{}', difficulty='{}'",
                updated.getId(), updated.getTitle(), updated.getDifficulty());

        boolean deleted = problemDAO.delete(inserted.getId());
        assertTrue(deleted, "Xóa bài tập phải thành công");
        log.info("[DELETE] Xóa bài tập id={} -> kết quả={}", inserted.getId(), deleted);

        Optional<Problem> checkAfterDelete = problemDAO.findById(inserted.getId());
        assertTrue(checkAfterDelete.isEmpty(), "Bài tập phải không còn tồn tại sau khi xóa");
        log.info("[VERIFY DELETE] Kiểm tra lại findById({}) -> tồn tại={}",
                inserted.getId(), checkAfterDelete.isPresent());
        log.info("=== [Kịch bản 1] Hoàn thành CRUD thành công ===");
    }

    @Test
    @Order(2)
    @DisplayName("Kịch bản 2: Kiểm thử tích hợp Tìm kiếm bài tập theo từ khóa (Search)")
    void test2_Search_ByKeyword() {
        log.info("=== [Kịch bản 2] Bắt đầu kiểm thử Tìm kiếm bài tập theo từ khóa (Search) ===");
        String keyword = "SearchTestKey" + System.currentTimeMillis();
        Problem p = Problem.builder()
                .title("Thuật toán " + keyword)
                .slug("slug-search-" + System.currentTimeMillis())
                .difficulty("EASY")
                .category("Search")
                .descriptionMarkdown("Mô tả tìm kiếm")
                .timeLimitMs(1000)
                .memoryLimitKb(131072)
                .isPublished(true)
                .build();
        Problem saved = problemDAO.insert(p);
        assertNotNull(saved);
        log.info("[SETUP] Đã tạo bài tập mẫu: id={}, title='{}'", saved.getId(), saved.getTitle());

        List<Problem> results = problemDAO.findWithFilters(null, null, keyword, 0, 10);
        log.info("[SEARCH] Tìm kiếm với keyword='{}' -> trả về {} kết quả", keyword, results.size());
        assertFalse(results.isEmpty(), "Kết quả tìm kiếm không được rỗng");
        assertTrue(results.get(0).getTitle().contains(keyword), "Tiêu đề kết quả phải chứa từ khóa tìm kiếm");
        log.info("[SEARCH] Kết quả đầu tiên: id={}, title='{}'", results.get(0).getId(), results.get(0).getTitle());

        boolean cleaned = problemDAO.delete(saved.getId());
        log.info("[CLEANUP] Đã xóa bài tập mẫu id={} -> {}", saved.getId(), cleaned);
        log.info("=== [Kịch bản 2] Hoàn thành Search thành công ===");
    }

    @Test
    @Order(3)
    @DisplayName("Kịch bản 3: Kiểm thử tích hợp Lọc bài tập theo Chủ đề và Độ khó (Filter)")
    void test3_Filter_ByCategoryAndDifficulty() {
        log.info("=== [Kịch bản 3] Bắt đầu kiểm thử Lọc bài tập theo Chủ đề & Độ khó (Filter) ===");
        String testCategory = "DynamicProgrammingTest";
        Problem p = Problem.builder()
                .title("Bài toán Quy hoạch động")
                .slug("slug-filter-" + System.currentTimeMillis())
                .difficulty("HARD")
                .category(testCategory)
                .descriptionMarkdown("Mô tả lọc dữ liệu")
                .timeLimitMs(1500)
                .memoryLimitKb(131072)
                .isPublished(true)
                .build();
        Problem saved = problemDAO.insert(p);
        assertNotNull(saved);
        log.info("[SETUP] Đã tạo bài tập mẫu: id={}, category='{}', difficulty='{}'",
                saved.getId(), saved.getCategory(), saved.getDifficulty());

        List<Problem> results = problemDAO.findWithFilters(testCategory, "HARD", null, 0, 10);
        log.info("[FILTER] Lọc theo category='{}', difficulty='HARD' -> trả về {} kết quả",
                testCategory, results.size());
        assertFalse(results.isEmpty(), "Phải có kết quả thỏa mãn bộ lọc");
        assertEquals(testCategory, results.get(0).getCategory());
        assertEquals("HARD", results.get(0).getDifficulty());
        log.info("[FILTER] Kết quả đầu tiên: id={}, title='{}', category='{}', difficulty='{}'",
                results.get(0).getId(), results.get(0).getTitle(),
                results.get(0).getCategory(), results.get(0).getDifficulty());

        boolean cleaned = problemDAO.delete(saved.getId());
        log.info("[CLEANUP] Đã xóa bài tập mẫu id={} -> {}", saved.getId(), cleaned);
        log.info("=== [Kịch bản 3] Hoàn thành Filter thành công ===");
    }

    @Test
    @Order(4)
    @DisplayName("Kịch bản 4: Kiểm thử tích hợp Phân trang danh sách bài tập (Pagination)")
    void test4_Pagination() {
        log.info("=== [Kịch bản 4] Bắt đầu kiểm thử Phân trang danh sách bài tập (Pagination) ===");
        Problem p1 = problemDAO.insert(Problem.builder()
                .title("Paging 1 " + System.currentTimeMillis())
                .slug("paging-1-" + System.currentTimeMillis())
                .difficulty("EASY")
                .category("Paging")
                .descriptionMarkdown("Desc")
                .build());

        Problem p2 = problemDAO.insert(Problem.builder()
                .title("Paging 2 " + System.currentTimeMillis())
                .slug("paging-2-" + System.currentTimeMillis())
                .difficulty("EASY")
                .category("Paging")
                .descriptionMarkdown("Desc")
                .build());
        log.info("[SETUP] Đã tạo 2 bài tập mẫu: p1.id={}, p2.id={}",
                p1 != null ? p1.getId() : null, p2 != null ? p2.getId() : null);

        List<Problem> page0 = problemDAO.findWithFilters(null, null, null, 0, 2);
        assertNotNull(page0);
        assertTrue(page0.size() <= 2, "Số lượng bài tập ở mỗi trang tối đa là 2");
        log.info("[PAGINATION] Truy vấn page=0, size=2 -> trả về {} bản ghi", page0.size());

        long totalCount = problemDAO.countWithFilters(null, null, null);
        assertTrue(totalCount >= 2, "Tổng số bài tập trong DB phải ít nhất là 2");
        log.info("[PAGINATION] Tổng số bài tập trong DB (countWithFilters) = {}", totalCount);

        boolean del1 = p1 != null && problemDAO.delete(p1.getId());
        boolean del2 = p2 != null && problemDAO.delete(p2.getId());
        log.info("[CLEANUP] Đã xóa bài tập mẫu -> p1={}, p2={}", del1, del2);
        log.info("=== [Kịch bản 4] Hoàn thành Pagination thành công ===");
    }

    @Test
    @Order(5)
    @DisplayName("Kịch bản 5: Kiểm thử tích hợp Bắt lỗi Validation dữ liệu đầu vào (Validation Error Handling)")
    void test5_Validation_ErrorHandling() {
        log.info("=== [Kịch bản 5] Bắt đầu kiểm thử Bắt lỗi Validation dữ liệu đầu vào ===");
        Problem invalidTitle = Problem.builder()
                .title("") // Rỗng -> vi phạm
                .difficulty("EASY")
                .descriptionMarkdown("Mô tả")
                .build();

        IllegalArgumentException exTitle = assertThrows(IllegalArgumentException.class, () -> {
            problemDAO.insert(invalidTitle);
        });
        assertEquals("Tiêu đề bài tập không được để trống!", exTitle.getMessage());
        log.info("[VALIDATION 1] Title rỗng -> bắt được {}: '{}'",
                exTitle.getClass().getSimpleName(), exTitle.getMessage());

        Problem invalidDifficulty = Problem.builder()
                .title("Bài tập hợp lệ")
                .difficulty("") // Rỗng -> vi phạm
                .descriptionMarkdown("Mô tả")
                .build();

        IllegalArgumentException exDiff = assertThrows(IllegalArgumentException.class, () -> {
            problemDAO.insert(invalidDifficulty);
        });
        assertEquals("Độ khó không được để trống!", exDiff.getMessage());
        log.info("[VALIDATION 2] Difficulty rỗng -> bắt được {}: '{}'",
                exDiff.getClass().getSimpleName(), exDiff.getMessage());

        Problem invalidDesc = Problem.builder()
                .title("Bài tập hợp lệ")
                .difficulty("EASY")
                .descriptionMarkdown("") // Rỗng -> vi phạm
                .build();

        IllegalArgumentException exDesc = assertThrows(IllegalArgumentException.class, () -> {
            problemDAO.insert(invalidDesc);
        });
        assertEquals("Mô tả đề bài không được để trống!", exDesc.getMessage());
        log.info("[VALIDATION 3] Description rỗng -> bắt được {}: '{}'",
                exDesc.getClass().getSimpleName(), exDesc.getMessage());
        log.info("=== [Kịch bản 5] Hoàn thành Validation Error Handling thành công ===");
    }
}
