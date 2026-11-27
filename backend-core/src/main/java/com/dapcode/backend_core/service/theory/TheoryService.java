package com.dapcode.backend_core.service.theory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.dapcode.backend_core.dto.admin.CreateTheoryRequest;
import com.dapcode.backend_core.dto.admin.UpdateTheoryRequest;
import com.dapcode.backend_core.dto.common.PageResponse;
import com.dapcode.backend_core.dto.theory.TheoryDetailResponse;
import com.dapcode.backend_core.dto.theory.TheorySummaryResponse;
import com.dapcode.backend_core.entity.Theories;

public interface TheoryService {
    List<TheorySummaryResponse> getTheories(String category);

    PageResponse<TheorySummaryResponse> getTheoriesPaged(String category, String search, Pageable pageable);

    TheoryDetailResponse getTheoryBySlug(String slug);

    List<String> getCategories();

    List<Theories> getAdminTheories(String category);

    Theories getAdminTheoryById(UUID id);

    Theories createTheory(CreateTheoryRequest req);

    Theories updateTheory(UUID id, UpdateTheoryRequest req);

    void deleteTheory(UUID id);
}
