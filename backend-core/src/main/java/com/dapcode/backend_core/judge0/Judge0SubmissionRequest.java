package com.dapcode.backend_core.judge0;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Judge0SubmissionRequest {
    private String source_code;
    private int language_id;
    private String compiler_options;
    private String stdin;
    private String expected_output;
    private Double cpu_time_limit;
    private Integer memory_limit;
    private Boolean enable_per_process_and_thread_memory_limit;
}
