# Text-to-SQL 智能助手 🤖

<div align="center">

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-8+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.14-brightgreen.svg)
![AI](https://img.shields.io/badge/AI-Qwen--Plus-red.svg)

**基于阿里云通义千问的自然语言转 SQL 工具**

---

## 📊 项目成果

在 [牛客网 125 道大厂 SQL 真题](https://www.nowcoder.com/exam/oj?page=1&tab=SQL%E7%AF%87&topicId=375) 上的实测数据：

| 结果 | 数量 | 占比 |
|------|------|------|
| ✅ 一次通过 | 82 题 | **65.6%** |
| 🔧 纠正一次后通过 | 27 题 | **21.6%** |
| ❌ 失败 | 16 题 | 12.8% |

**综合通过率：87.2%**



---

## ✨ 功能特性

- 🤖 **自然语言转 SQL**：输入中文需求，自动生成标准 SQL
- 🎯 **高准确率**：基于精心设计的 Prompt，一次通过率 65.6%
- 🔧 **智能纠错**：支持错误反馈，AI 自动修正
- 📋 **SQL 格式化**：自动美化 SQL，提升可读性
- 🗃️ **多数据库支持**：MySQL、PostgreSQL、ClickHouse
- 🎨 **友好界面**：简洁的 Web UI，支持一键复制
- ⚡ **快速响应**：平均 2 秒生成结果

---

## 🎬 效果演示

### 输入

```
题目：查询所有北京大学的学生信息，按年龄升序排列

表结构：
user_profile(id, device_id, gender, age, university)
```

### 输出

```sql
SELECT device_id, gender, age, university
FROM user_profile
WHERE university = '北京大学'
ORDER BY age ASC;
```

---

## 🚀 快速开始

### 前置要求

- Java 8+
- Maven 3.6+
- 阿里云通义千问 API Key（[申请地址](https://dashscope.aliyun.com/)）

### 1. 克隆项目

```bash
git clone https://github.com/your-username/text-to-sql-assistant.git
cd text-to-sql-assistant
```

### 2. 配置 API Key

**方式一：环境变量（推荐）**

```bash
# Linux/Mac
export DASHSCOPE_API_KEY=sk-your-api-key-here

# Windows
set DASHSCOPE_API_KEY=sk-your-api-key-here
```

**方式二：配置文件**

编辑 `src/main/resources/application.properties`：

```properties
dashscope.api.key=sk-your-api-key-here
```

### 3. 启动项目

```bash
# 编译
mvn clean package

# 运行
java -jar target/text-to-sql-assistant-1.0-SNAPSHOT.jar

# 或直接运行
mvn spring-boot:run
```

### 4. 访问应用

打开浏览器访问：**http://localhost:8080**

---

## 📁 项目结构

```
text-to-sql-assistant/
├── src/main/java/com/sql/assistant/
│   ├── controller/
│   │   └── SQLController.java          # REST API 控制器
│   ├── service/
│   │   ├── TextToSQLService.java       # SQL 生成核心逻辑
│   │   └── SQLExecutorService.java     # SQL 执行引擎
│   ├── model/
│   │   ├── SQLRequest.java             # 请求模型
│   │   ├── SQLResponse.java            # 响应模型
│   │   └── TableSchema.java            # 表结构模型
│   └── TextToSQLApplication.java       # 启动类
├── src/main/resources/
│   ├── static/
│   │   └── index.html                  # Web 前端界面
│   └── application.properties          # 配置文件
├── docs/                               # 文档
├── pom.xml                             # Maven 配置
└── README.md
```

---

## 🎯 核心技术

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.14 | Web 框架 |
| 阿里云通义千问 | qwen-plus | LLM 引擎 |
| OkHttp | 4.12.0 | HTTP 客户端 |
| JSQLParser | 4.6 | SQL 解析与验证 |
| MySQL | 8.0.33 | 数据库驱动 |
| Jackson | - | JSON 处理 |

### Prompt Engineering 核心

#### 11 条强制规则

```
1. ⚠️ 输出列名/别名：必须与题目要求的输出格式完全一致
2. ⚠️ 排序(ORDER BY)：题目说"按xx升序"，必须加ORDER BY xx ASC
3. ⚠️ 去重(DISTINCT)：题目说"去重"或"不重复"，必须用DISTINCT
4. ⚠️ 限制数量(LIMIT)：题目说"前10条"、"前N条"，必须加LIMIT N
5. ⚠️ 字段名大小写：严格使用表结构中的字段名（MySQL在Linux下区分大小写）
6. ⚠️ 聚合函数：COUNT(*)统计行数，COUNT(field)统计非NULL值
7. ⚠️ 日期/月份提取：MONTH(date)返回1-12，DATE_FORMAT(date,'%m')返回01-12
8. ⚠️ 窗口函数：ROW_NUMBER()连续排序，RANK()跳跃排序，DENSE_RANK()密集排序
9. ⚠️ 条件筛选：字符串用单引号，NULL值用IS NOT NULL
10. ⚠️ 时间差计算：需要小数精度时用TIMESTAMPDIFF(MINUTE)/60，不要用TIMESTAMPDIFF(HOUR)
11. ⚠️ 输出格式：只输出SQL语句，不要有任何解释或注释
```

#### CTE 强制使用场景

```
1. 多表 JOIN + 复杂聚合
2. 时间窗口逻辑
3. 需要判断'之前/之后'的状态
4. 涉及多个计算步骤的业务逻辑
5. 题目超过 3 个判断条件
```

---

## 📖 使用指南

### 基础用法

1. **输入题目需求**：在文本框中输入自然语言描述
2. **定义表结构**：填写表名、字段名、字段类型
3. **指定输出格式**：告诉 AI 需要哪些列
4. **生成 SQL**：点击"生成 SQL"按钮
5. **复制使用**：点击"复制"按钮一键复制

### 高级功能

#### 错误反馈与纠正

如果生成的 SQL 有问题：

1. 在"错误反馈"框中描述问题（如：字段名大小写错误）
2. 点击"重新生成"
3. AI 会基于反馈修正 SQL

#### 支持的 SQL 方言

- MySQL（默认）
- PostgreSQL
- ClickHouse

在界面下拉框中选择对应方言即可。

---

## 🎓 适用场景

### 1. SQL 学习者

- 快速生成 SQL 模板
- 学习标准写法
- 对比自己的实现

### 2. 数据分析师

- 提升查询效率
- 减少语法错误
- 专注业务逻辑

### 3. 开发人员

- 快速原型开发
- SQL 代码生成
- 减少重复劳动

---

## 💡 AI 能力边界

### ✅ 擅长场景（通过率 90%+）

- 单表查询 + WHERE 条件
- 简单 JOIN（2-3 表）
- 基础聚合（SUM、AVG、COUNT）
- 常见窗口函数（ROW_NUMBER、RANK）

### ⚠️ 需要辅助场景（通过率 60%-80%）

- 多表 JOIN（4+ 表）
- 复杂子查询
- 漏斗分析
- 时间窗口计算

**建议：** 提供更详细的上下文，分解为多个 CTE 步骤

### ❌ 力不从心场景（通过率 < 40%）

- 递归 CTE
- 复杂数学建模
- 图论问题
- 高度依赖业务经验的隐含条件

**建议：** 人工介入或分解问题

---

## 🔧 配置说明

### application.properties

```properties
# 服务器端口
server.port=8080

# 通义千问 API Key（可通过环境变量覆盖）
dashscope.api.key=${DASHSCOPE_API_KEY:your-default-key}

# 数据库配置（可选，仅用于 SQL 执行）
spring.datasource.url=jdbc:mysql://localhost:3306/test
spring.datasource.username=root
spring.datasource.password=your-password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 连接池配置
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```



## 📝 更新日志

### v1.0.0 (2024-01-15)

- ✨ 支持自然语言转 SQL
- ✨ 11 条 Prompt 强制规则
- ✨ CTE 强制使用优化
- ✨ SQL 格式化功能
- ✨ 错误反馈机制
- ✨ Web UI 界面
- 🐛 修复时间精度丢失问题
- 🐛 修复窗口函数选择错误

---

## 🔗 相关链接

- [技术博客 - 用 AI 刷 SQL 题]([用 AI 刷 SQL 题？我做了个 Text-to-SQL 助手把牛客网 125 道大厂 SQL 真题喂给 AI，看看它 - 掘金](https://juejin.cn/spost/7678269666679619611))
- [牛客网 SQL 真题库](https://www.nowcoder.com/exam/oj?page=1&tab=SQL%E7%AF%87&topicId=375)
- [阿里云通义千问](https://dashscope.aliyun.com/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。
