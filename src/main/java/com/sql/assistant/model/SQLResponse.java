package com.sql.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SQL 生成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SQLResponse {
    
    /** 生成的SQL */
    private String generatedSQL;
    
    /** 是否生成成功 */
    private boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** SQL语法是否有效 */
    private boolean syntaxValid;
    
    /** 语法验证信息 */
    private String syntaxMessage;
    
    /** 执行结果（如果执行了SQL） */
    private List<Map<String, Object>> queryResult;
    
    /** 执行耗时（毫秒） */
    private long executionTimeMs;
    
    /** 结果行数 */
    private int resultCount;
    
    /** LLM 响应的完整文本 */
    private String llmResponse;
    
    public static SQLResponse success(String sql) {
        SQLResponse response = new SQLResponse();
        response.setSuccess(true);
        response.setGeneratedSQL(sql);
        return response;
    }
    
    public static SQLResponse error(String errorMessage) {
        SQLResponse response = new SQLResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}