-- RAG ChatStore 初始化脚本（容器首次启动时自动执行）
-- 1) 启用 pgvector 扩展（Spring AI PgVectorStore / 向量检索依赖）
CREATE EXTENSION IF NOT EXISTS vector;
