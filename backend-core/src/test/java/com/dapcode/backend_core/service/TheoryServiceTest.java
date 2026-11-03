package com.dapcode.backend_core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.dto.admin.CreateTheoryRequest;
import com.dapcode.backend_core.dto.admin.UpdateTheoryRequest;
import com.dapcode.backend_core.dto.theory.TheoryDetailResponse;
import com.dapcode.backend_core.entity.Theories;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.repository.TheoryRepository;
import com.dapcode.backend_core.service.theory.TheoryServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TheoryServiceTest {

    @Mock
    private TheoryRepository theoryRepository;

    @InjectMocks
    private TheoryServiceImpl theoryService;

    private Theories sampleTheory;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleTheory = Theories.builder()
                .id(sampleId)
                .title("Mảng và Bảng Băm")
                .slug("mang-va-bang-bam")
                .category("Arrays & Hashing")
                .summary("Tổng quan về cấu trúc mảng và hash table")
                .contentMarkdown("# Lý thuyết Mảng\nMảng là cấu trúc dữ liệu liên tục...")
                .orderIndex(1)
                .isPublished(true)
                .build();
    }

    @Test
    void testCreateTheory_Success() {
        CreateTheoryRequest req = CreateTheoryRequest.builder()
                .title("Hai Con Trỏ")
                .slug("two-pointers")
                .category("Two Pointers")
                .summary("Kỹ thuật hai con trỏ tối ưu O(N)")
                .contentMarkdown("# Kỹ thuật Two Pointers...")
                .orderIndex(2)
                .isPublished(true)
                .build();

        when(theoryRepository.existsBySlug("two-pointers")).thenReturn(false);
        when(theoryRepository.save(any(Theories.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Theories created = theoryService.createTheory(req);

        assertNotNull(created);
        assertEquals("Hai Con Trỏ", created.getTitle());
        assertEquals("two-pointers", created.getSlug());
        assertEquals("Two Pointers", created.getCategory());
        verify(theoryRepository).save(any(Theories.class));
    }

    @Test
    void testGetTheoryBySlug_Success() {
        when(theoryRepository.findBySlugAndIsPublishedTrue("mang-va-bang-bam"))
                .thenReturn(Optional.of(sampleTheory));

        TheoryDetailResponse res = theoryService.getTheoryBySlug("mang-va-bang-bam");

        assertNotNull(res);
        assertEquals(sampleId, res.getId());
        assertEquals("Mảng và Bảng Băm", res.getTitle());
        assertEquals("Arrays & Hashing", res.getCategory());
    }

    @Test
    void testUpdateTheory_Success() {
        UpdateTheoryRequest req = UpdateTheoryRequest.builder()
                .title("Mảng và Bảng Băm (Đã cập nhật)")
                .category("Arrays & Hashing")
                .summary("Bản tóm tắt mới")
                .contentMarkdown("Nội dung bài giảng mới...")
                .orderIndex(5)
                .isPublished(true)
                .build();

        when(theoryRepository.findById(sampleId)).thenReturn(Optional.of(sampleTheory));
        when(theoryRepository.save(any(Theories.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Theories updated = theoryService.updateTheory(sampleId, req);

        assertNotNull(updated);
        assertEquals("Mảng và Bảng Băm (Đã cập nhật)", updated.getTitle());
        assertEquals(5, updated.getOrderIndex());
        verify(theoryRepository).save(sampleTheory);
    }

    @Test
    void testDeleteTheory_Success() {
        when(theoryRepository.findById(sampleId)).thenReturn(Optional.of(sampleTheory));

        theoryService.deleteTheory(sampleId);

        verify(theoryRepository).delete(sampleTheory);
    }

    @Test
    void testGetTheoryBySlug_NotFound() {
        when(theoryRepository.findBySlugAndIsPublishedTrue("slug-khong-ton-tai"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> theoryService.getTheoryBySlug("slug-khong-ton-tai"));
        assertEquals(ErrorCode.THEORY_NOT_FOUND, ex.getErrorCode());
    }
}

