package com.sql.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 生成请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SQLRequest {
    
    /** 题目需求描述 */
    private String questionDescription;
    
    /** 输出格式要求（列名、排序等） */
    private String outputFormat;
    
    /** 错误反馈信息（用于二次分析） */
    private String errorFeedback;
    
    /** 上次生成的SQL（用于增量修改） */
    private String previousSQL;
    
    /** SQL 方言：mysql, postgresql, clickhouse */
    private String sqlDialect;
    
    /** 是否执行SQL */
    private boolean executeSQL;
    
    /** 表结构信息（牛客题目场景，用户直接粘贴） */
    private String tableSchema;
    
    /** 数据库连接信息（执行时需要） */
    private String databaseUrl;
    private String username;
    private String password;
    
    /** 兼容旧字段 */
    @Deprecated
    public String getNaturalLanguageQuery() {
        return questionDescription;
    }
    
    @Deprecated
    public void setNaturalLanguageQuery(String naturalLanguageQuery) {
        this.questionDescription = naturalLanguageQuery;
    }
}