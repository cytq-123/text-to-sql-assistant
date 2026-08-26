package com.sql.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sql.assistant.model.SQLRequest;
import com.sql.assistant.model.SQLResponse;
import com.sql.assistant.model.TableSchema;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TextToSQLService {
    
    private static final Logger LOG = LoggerFactory.getLogger(TextToSQLService.class);
    
    @Value("${dashscope.api.key}")
    private String apiKey;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public TextToSQLService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public SQLResponse generateSQL(SQLRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String prompt = buildPrompt(request);
            String llmResponse = callLLM(prompt);
            String sql = extractSQL(llmResponse);
            boolean syntaxValid = validateSQL(sql, request.getSqlDialect());
            
            SQLResponse response = SQLResponse.success(sql);
            response.setSyntaxValid(syntaxValid);
            response.setSyntaxMessage(syntaxValid ? "语法检查通过" : "语法可能有误，请人工检查");
            response.setLlmResponse(llmResponse);
            response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            LOG.info("SQL生成成功 - 耗时: {}ms", response.getExecutionTimeMs());
            
            return response;
            
        } catch (Exception e) {
            LOG.error("SQL生成失败", e);
            return SQLResponse.error("生成失败：" + e.getMessage());
        }
    }
    
    private String buildPrompt(SQLRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个SQL专家，擅长解决牛客网SQL笔试题目。\n\n");
        
        prompt.append("⚠️ 【核心原则 - 必须遵守】\n");
        prompt.append("对于以下情况，必须使用 WITH (CTE) 分步处理，禁止写单一复杂查询：\n");
        prompt.append("1. 多表 JOIN + 复杂聚合\n");
        prompt.append("2. 包含时间窗口判断（如'10分钟内'）\n");
        prompt.append("3. 需要判断'之前/之后'的状态\n");
        prompt.append("4. 涉及多个计算步骤的业务逻辑\n");
        prompt.append("5. 题目超过 3 个判断条件\n\n");
        
        prompt.append("标准 CTE 写法示例：\n");
        prompt.append("WITH step1 AS (筛选基础数据),\n");
        prompt.append("     step2 AS (关联表+标记事件),\n");
        prompt.append("     step3 AS (窗口函数/条件判断),\n");
        prompt.append("     step4 AS (聚合统计)\n");
        prompt.append("SELECT * FROM step4;\n\n");
        
        prompt.append("【数据库类型】\n");
        prompt.append(request.getSqlDialect()).append("\n\n");
        
        prompt.append("【表结构信息】\n");
        if (request.getTableSchema() != null && !request.getTableSchema().trim().isEmpty()) {
            prompt.append(request.getTableSchema());
            prompt.append("\n\n注意：\n");
            prompt.append("- 如果只给了表格样本数据，请根据样本推断字段类型\n");
            prompt.append("- 数字推断为INT，文本推断为VARCHAR，日期推断为DATE/DATETIME\n");
            prompt.append("- id字段通常是INT，device_id也是INT\n");
            prompt.append("- 姓名、学校、省份等文本字段用VARCHAR\n");
        } else {
            prompt.append(getExampleSchema());
        }
        prompt.append("\n\n");
        
        prompt.append("【题目需求】\n");
        String questionDesc = request.getQuestionDescription();
        if (questionDesc == null || questionDesc.trim().isEmpty()) {
            questionDesc = request.getNaturalLanguageQuery();
        }
        prompt.append(questionDesc).append("\n\n");
        
        if (request.getOutputFormat() != null && !request.getOutputFormat().trim().isEmpty()) {
            prompt.append("【输出格式要求】\n");
            prompt.append(request.getOutputFormat()).append("\n\n");
        }
        
        if (request.getErrorFeedback() != null && !request.getErrorFeedback().trim().isEmpty()) {
            prompt.append("【上次生成的SQL】\n");
            if (request.getPreviousSQL() != null && !request.getPreviousSQL().trim().isEmpty()) {
                prompt.append(request.getPreviousSQL()).append("\n\n");
                prompt.append("【需要修改的问题】\n");
                prompt.append(request.getErrorFeedback()).append("\n\n");
                prompt.append("⚠️ 关键要求：这是增量修改，不要改变SQL的核心逻辑！\n\n");
            } else {
                prompt.append("【存在以下问题】\n");
                prompt.append(request.getErrorFeedback()).append("\n");
                prompt.append("请重新生成正确的SQL。\n\n");
            }
        }
        
        prompt.append("【关键要求 - 必须严格遵守】\n");
        prompt.append("1. ⚠️ 输出列名/别名：必须与题目要求的输出格式完全一致\n");
        prompt.append("2. ⚠️ 排序(ORDER BY)：题目说\"按xx升序\"，必须加ORDER BY xx ASC\n");
        prompt.append("3. ⚠️ 去重(DISTINCT)：题目说\"去重\"或\"不重复\"，必须用DISTINCT\n");
        prompt.append("4. ⚠️ 限制数量(LIMIT)：题目说\"前10条\"、\"前N条\"，必须加LIMIT N\n");
        prompt.append("5. ⚠️ 字段名大小写：严格使用表结构中的字段名（MySQL在Linux下区分大小写）\n");
        prompt.append("6. ⚠️ 聚合函数：COUNT(*)统计行数，COUNT(field)统计非NULL值\n");
        prompt.append("7. ⚠️ 日期/月份提取：MONTH(date)返回1-12，DATE_FORMAT(date,'%m')返回01-12\n");
        prompt.append("8. ⚠️ 窗口函数：ROW_NUMBER()连续排序，RANK()跳跃排序，DENSE_RANK()密集排序\n");
        prompt.append("9. ⚠️ 条件筛选：字符串用单引号，NULL值用IS NOT NULL\n");
        prompt.append("10. ⚠️ 时间差计算：需要小数精度时用TIMESTAMPDIFF(MINUTE)/60或TIMESTAMPDIFF(SECOND)/3600，不要用TIMESTAMPDIFF(HOUR)（会丢失分钟秒）\n");
        prompt.append("11. ⚠️ 输出格式：只输出SQL语句，不要有任何解释或注释，不要用```sql包裹\n\n");
        
        return prompt.toString();
    }
    
    private String getExampleSchema() {
        return "表名: orders\n字段:\n" +
               "  - order_id INT PRIMARY KEY\n" +
               "  - user_id INT\n" +
               "  - product_name VARCHAR(100)\n" +
               "  - category VARCHAR(50)\n" +
               "  - amount DECIMAL(10,2)\n" +
               "  - order_date DATE\n";
    }
    
    private String callLLM(String prompt) throws Exception {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        requestBody.put("input", input);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "text");
        requestBody.put("parameters", parameters);
        requestBody.put("model", "qwen-plus");
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("LLM API 调用失败: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode output = root.path("output");
            return output.path("text").asText();
        }
    }
    
    private String extractSQL(String llmResponse) {
        String sql = llmResponse.trim();
        
        if (sql.startsWith("```sql")) {
            sql = sql.substring(6);
        } else if (sql.startsWith("```")) {
            sql = sql.substring(3);
        }
        
        if (sql.endsWith("```")) {
            sql = sql.substring(0, sql.length() - 3);
        }
        
        return formatSQL(sql.trim());
    }
    
    private String formatSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        sql = sql.replaceAll("\\s+", " ").trim();
        sql = sql.replaceAll("(?i)\\s+(FROM)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(WHERE)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(LEFT\\s+JOIN|RIGHT\\s+JOIN|INNER\\s+JOIN|OUTER\\s+JOIN|JOIN)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(GROUP\\s+BY)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(HAVING)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(ORDER\\s+BY)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(LIMIT)\\s+", "\n$1 ");
        sql = sql.replaceAll("(?i)\\s+(AND)\\s+", "\n  $1 ");
        sql = sql.replaceAll("(?i)\\s+(OR)\\s+", "\n  $1 ");
        
        return sql;
    }
    
    private boolean validateSQL(String sql, String dialect) {
        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (Exception e) {
            LOG.warn("SQL 语法验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isSQLSafe(String sql) {
        String upperSQL = sql.toUpperCase().trim();
        
        if (!upperSQL.startsWith("SELECT")) {
            return false;
        }
        
        String[] dangerousKeywords = {"DROP", "DELETE", "TRUNCATE", "ALTER", "GRANT", "REVOKE", "EXEC", "EXECUTE"};
        for (String keyword : dangerousKeywords) {
            if (upperSQL.contains(keyword)) {
                return false;
            }
        }
        
        return true;
    }
}