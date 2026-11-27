package com.dapcode.backend_core.judge0;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class Judge0SubmissionRequestTest {

    @Test
    @DisplayName("Judge0SubmissionRequest chỉ serialize snake_case, không lặp camelCase và bỏ qua field null")
    void shouldSerializeOnlySnakeCaseWithoutDuplicatesOrNulls() {
        Judge0SubmissionRequest request = Judge0SubmissionRequest.builder()
                .source_code("print(1)")
                .language_id(71)
                .stdin("x = 1")
                .expected_output("1")
                .cpu_time_limit(2.0)
                .memory_limit(262144)
                .build();

        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"source_code\":\"print(1)\""), json);
        assertTrue(json.contains("\"language_id\":71"), json);
        assertTrue(json.contains("\"stdin\":\"x = 1\""), json);
        assertTrue(json.contains("\"expected_output\":\"1\""), json);
        assertTrue(json.contains("\"cpu_time_limit\":2.0"), json);
        assertTrue(json.contains("\"memory_limit\":262144"), json);

        assertFalse(json.contains("\"sourceCode\""), "Không được serialize trùng sourceCode: " + json);
        assertFalse(json.contains("\"languageId\""), "Không được serialize trùng languageId: " + json);
        assertFalse(json.contains("\"expectedOutput\""), "Không được serialize trùng expectedOutput: " + json);
        assertFalse(json.contains("\"cpuTimeLimit\""), "Không được serialize trùng cpuTimeLimit: " + json);
        assertFalse(json.contains("\"memoryLimit\""), "Không được serialize trùng memoryLimit: " + json);
        assertFalse(json.contains("enable_per_process_and_thread_memory_limit"),
                "Không được serialize field null: " + json);
        assertFalse(json.contains("compiler_options"),
                "Không được serialize compiler_options khi null: " + json);

        Judge0SubmissionRequest cppReq = Judge0SubmissionRequest.builder()
                .source_code("int main(){}")
                .language_id(54)
                .compiler_options("-std=c++17")
                .build();
        String cppJson = mapper.writeValueAsString(cppReq);
        assertTrue(cppJson.contains("\"compiler_options\":\"-std=c++17\""), cppJson);
    }
}
