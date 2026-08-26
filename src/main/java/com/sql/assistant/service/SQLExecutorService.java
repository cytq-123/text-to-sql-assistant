package com.sql.assistant.service;

import com.sql.assistant.model.SQLRequest;
import com.sql.assistant.model.SQLResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行服务
 * 
 * 功能：
 * 1. 执行 SQL 查询
 * 2. 返回查询结果
 * 3. 安全控制
 * 
 * @author Your Name
 * @date 2024-08-25
 */
@Service
public class SQLExecutorService {
    
    private static final Logger LOG = LoggerFactory.getLogger(SQLExecutorService.class);
    
    private static final int MAX_RESULT_SIZE = 1000; // 最多返回1000行
    
    /**
     * 执行 SQL 查询
     */
    public SQLResponse executeSQL(SQLRequest request, String sql) {
        long startTime = System.currentTimeMillis();
        
        // 安全检查
        if (!isSQLSafe(sql)) {
            return SQLResponse.error("SQL 不安全：只允许 SELECT 查询");
        }
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            // 建立数据库连接
            conn = DriverManager.getConnection(
                    request.getDatabaseUrl(),
                    request.getUsername(),
                    request.getPassword()
            );
            
            // 执行查询
            stmt = conn.createStatement();
            stmt.setMaxRows(MAX_RESULT_SIZE); // 限制返回行数
            
            rs = stmt.executeQuery(sql);
            
            // 解析结果
            List<Map<String, Object>> resultList = parseResultSet(rs);
            
            // 构建响应
            SQLResponse response = SQLResponse.success(sql);
            response.setQueryResult(resultList);
            response.setResultCount(resultList.size());
            response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            
            LOG.info("SQL执行成功 - 返回{}行, 耗时: {}ms", resultList.size(), response.getExecutionTimeMs());
            
            return response;
            
        } catch (SQLException e) {
            LOG.error("SQL执行失败", e);
            return SQLResponse.error("执行失败：" + e.getMessage());
            
        } finally {
            // 关闭资源
            closeResources(rs, stmt, conn);
        }
    }
    
    /**
     * 解析查询结果
     */
    private List<Map<String, Object>> parseResultSet(ResultSet rs) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            
            resultList.add(row);
        }
        
        return resultList;
    }
    
    /**
     * 安全检查
     */
    private boolean isSQLSafe(String sql) {
        String upperSQL = sql.toUpperCase().trim();
        
        // 只允许 SELECT 查询
        if (!upperSQL.startsWith("SELECT")) {
            return false;
        }
        
        // 检测危险关键字
        String[] dangerousKeywords = {
                "DROP", "DELETE", "TRUNCATE", "ALTER", 
                "GRANT", "REVOKE", "EXEC", "EXECUTE",
                "INSERT", "UPDATE", "CREATE"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSQL.contains(" " + keyword + " ") || 
                upperSQL.contains(" " + keyword + ";")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 关闭资源
     */
    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            LOG.error("关闭资源失败", e);
        }
    }
}