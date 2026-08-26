package com.sql.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Text-to-SQL 智能生成助手
 * 
 * @author Your Name
 * @date 2024-08-25
 */
@SpringBootApplication
public class TextToSQLApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TextToSQLApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("🤖 Text-to-SQL 智能生成助手启动成功！");
        System.out.println("📝 访问地址: http://localhost:8080");
        System.out.println("========================================\n");
    }
}