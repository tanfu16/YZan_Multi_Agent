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
