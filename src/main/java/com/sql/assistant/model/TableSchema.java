package com.sql.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据库表结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableSchema {
    
    /** 表名 */
    private String tableName;
    
    /** 表说明 */
    private String description;
    
    /** 字段列表 */
    private List<ColumnInfo> columns;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        /** 字段名 */
        private String columnName;
        
        /** 字段类型 */
        private String dataType;
        
        /** 是否可空 */
        private boolean nullable;
        
        /** 字段说明 */
        private String comment;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("表名: ").append(tableName).append("\n");
        if (description != null && !description.isEmpty()) {
            sb.append("说明: ").append(description).append("\n");
        }
        sb.append("字段:\n");
        for (ColumnInfo col : columns) {
            sb.append("  - ").append(col.columnName)
              .append(" (").append(col.dataType).append(")");
            if (col.comment != null && !col.comment.isEmpty()) {
                sb.append(" // ").append(col.comment);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}