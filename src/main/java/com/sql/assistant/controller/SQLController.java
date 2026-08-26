package com.sql.assistant.controller;

import com.sql.assistant.model.SQLRequest;
import com.sql.assistant.model.SQLResponse;
import com.sql.assistant.service.SQLExecutorService;
import com.sql.assistant.service.TextToSQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL 生成 REST API
 * 
 * @author Your Name
 * @date 2024-08-25
 */
@RestController
@RequestMapping("/api/sql")
@CrossOrigin(origins = "*") // 允许跨域
public class SQLController {
    
    private static final Logger LOG = LoggerFactory.getLogger(SQLController.class);
    
    @Autowired
    private TextToSQLService textToSQLService;
    
    @Autowired
    private SQLExecutorService sqlExecutorService;
    
    /**
     * 生成 SQL
     * 
     * POST /api/sql/generate
     * {
     *   "naturalLanguageQuery": "查询北京地区销售额前10的商品",
     *   "sqlDialect": "mysql",
     *   "executeSQL": false
     * }
     */
    @PostMapping("/generate")
    public SQLResponse generateSQL(@RequestBody SQLRequest request) {
        LOG.info("收到SQL生成请求 - 查询: {}, 方言: {}", 
                request.getNaturalLanguageQuery(), 
                request.getSqlDialect());
        
        try {
            // 参数校验
            if (request.getNaturalLanguageQuery() == null || 
                request.getNaturalLanguageQuery().trim().isEmpty()) {
                return SQLResponse.error("查询内容不能为空");
            }
            
            if (request.getSqlDialect() == null || 
                request.getSqlDialect().trim().isEmpty()) {
                request.setSqlDialect("mysql"); // 默认MySQL
            }
            
            // 生成 SQL
            SQLResponse response = textToSQLService.generateSQL(request);
            
            // 如果需要执行SQL
            if (request.isExecuteSQL() && response.isSuccess()) {
                String sql = response.getGeneratedSQL();
                
                // 安全检查
                if (!textToSQLService.isSQLSafe(sql)) {
                    response.setErrorMessage("SQL不安全，拒绝执行");
                    return response;
                }
                
                // 执行SQL
                SQLResponse execResponse = sqlExecutorService.executeSQL(request, sql);
                
                // 合并结果
                response.setQueryResult(execResponse.getQueryResult());
                response.setResultCount(execResponse.getResultCount());
            }
            
            return response;
            
        } catch (Exception e) {
            LOG.error("SQL生成失败", e);
            return SQLResponse.error("服务器错误：" + e.getMessage());
        }
    }
    
    /**
     * 验证 SQL 语法
     * 
     * POST /api/sql/validate
     * {
     *   "generatedSQL": "SELECT * FROM orders WHERE city = '北京'",
     *   "sqlDialect": "mysql"
     * }
     */
    @PostMapping("/validate")
    public Map<String, Object> validateSQL(@RequestBody Map<String, String> request) {
        String sql = request.get("generatedSQL");
        String dialect = request.getOrDefault("sqlDialect", "mysql");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);
            result.put("valid", true);
            result.put("message", "语法检查通过");
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "语法错误：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "OK");
        result.put("service", "Text-to-SQL Assistant");
        return result;
    }
}