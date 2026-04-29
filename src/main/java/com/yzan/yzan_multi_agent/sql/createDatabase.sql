CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE requirement_record (
                                    id BIGSERIAL PRIMARY KEY,
                                    request_id VARCHAR(64) NOT NULL,
                                    session_id VARCHAR(64) NOT NULL,
                                    parent_request_id VARCHAR(64),
                                    user_id VARCHAR(64),
                                    user_requirement_json TEXT NOT NULL,
                                    structured_requirement_json TEXT NOT NULL,
                                    created_at TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP
);

CREATE INDEX idx_requirement_record_request_id ON requirement_record(request_id);
CREATE INDEX idx_requirement_record_session_id ON requirement_record(session_id);
CREATE INDEX idx_requirement_record_parent_request_id ON requirement_record(parent_request_id);

CREATE TABLE agent_execution_record (
                                        id BIGSERIAL PRIMARY KEY,
                                        request_id VARCHAR(64) NOT NULL,
                                        session_id VARCHAR(64) NOT NULL,
                                        parent_request_id VARCHAR(64),
                                        user_id VARCHAR(64),
                                        structured_requirement_json TEXT NOT NULL,
                                        agent_result_json TEXT NOT NULL,
                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP
);

CREATE INDEX idx_agent_execution_record_request_id ON agent_execution_record(request_id);
CREATE INDEX idx_agent_execution_record_session_id ON agent_execution_record(session_id);
CREATE INDEX idx_agent_execution_record_parent_request_id ON agent_execution_record(parent_request_id);

CREATE TABLE plan_record (
                             id BIGSERIAL PRIMARY KEY,
                             request_id VARCHAR(64) NOT NULL,
                             session_id VARCHAR(64) NOT NULL,
                             parent_request_id VARCHAR(64),
                             user_id VARCHAR(64),
                             structured_requirement_json TEXT NOT NULL,
                             agent_results_json TEXT NOT NULL,
                             decoration_plan_json TEXT NOT NULL,
                             created_at TIMESTAMP NOT NULL,
                             updated_at TIMESTAMP
);

CREATE INDEX idx_plan_record_request_id ON plan_record(request_id);
CREATE INDEX idx_plan_record_session_id ON plan_record(session_id);
CREATE INDEX idx_plan_record_parent_request_id ON plan_record(parent_request_id);

CREATE TABLE requirement_state_record (
                                          id BIGSERIAL PRIMARY KEY,
                                          session_id VARCHAR(64) NOT NULL UNIQUE,
                                          structured_requirement_json TEXT NOT NULL,
                                          version BIGINT NOT NULL,
                                          created_at TIMESTAMP NOT NULL,
                                          updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_requirement_state_record_session_id ON requirement_state_record(session_id);

CREATE TABLE conversation_turn_record (
                                          id BIGSERIAL PRIMARY KEY,
                                          session_id VARCHAR(64) NOT NULL,
                                          raw_input TEXT NOT NULL,
                                          assistant_output TEXT,
                                          intent_type VARCHAR(64),
                                          created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_conversation_turn_record_session_id ON conversation_turn_record(session_id);

CREATE TABLE conversation_summary_record (
                                              id BIGSERIAL PRIMARY KEY,
                                              session_id VARCHAR(64) NOT NULL,
                                              summary_text TEXT NOT NULL,
                                              start_turn INTEGER NOT NULL,
                                              end_turn INTEGER NOT NULL,
                                              created_at TIMESTAMP NOT NULL,
                                              updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_conversation_summary_record_session_id ON conversation_summary_record(session_id);
CREATE UNIQUE INDEX uk_conversation_summary_record_session_turn_range
    ON conversation_summary_record(session_id, start_turn, end_turn);

CREATE TABLE knowledge_chunk_record (
                                        id BIGSERIAL PRIMARY KEY,
                                        agent_domain VARCHAR(32) NOT NULL,
                                        source_name VARCHAR(255) NOT NULL,
                                        content TEXT NOT NULL,
                                        content_hash VARCHAR(64) NOT NULL,
                                        embedding_json TEXT NOT NULL,
                                        embedding_vector VECTOR(1024),
                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP
);

CREATE UNIQUE INDEX uk_knowledge_chunk_record_content_hash ON knowledge_chunk_record(content_hash);
CREATE INDEX idx_knowledge_chunk_record_agent_domain ON knowledge_chunk_record(agent_domain);
CREATE INDEX idx_knowledge_chunk_record_source_name ON knowledge_chunk_record(source_name);
CREATE INDEX idx_knowledge_chunk_record_embedding_vector
    ON knowledge_chunk_record
    USING hnsw (embedding_vector vector_cosine_ops);

CREATE TABLE IF NOT EXISTS knowledge_chunk_record (
                                                      id BIGSERIAL PRIMARY KEY,
                                                      agent_domain VARCHAR(32) NOT NULL,
                                                      source_name VARCHAR(255) NOT NULL,
                                                      content TEXT NOT NULL,
                                                      content_hash VARCHAR(64) NOT NULL,
                                                      embedding_json TEXT NOT NULL,
                                                      embedding_vector VECTOR(1024),
                                                      created_at TIMESTAMP NOT NULL,
                                                      updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_chunk_record_content_hash
    ON knowledge_chunk_record(content_hash);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_agent_domain
    ON knowledge_chunk_record(agent_domain);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_source_name
    ON knowledge_chunk_record(source_name);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_embedding_vector
    ON knowledge_chunk_record
    USING hnsw (embedding_vector vector_cosine_ops);
