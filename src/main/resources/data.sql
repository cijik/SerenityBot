DROP TABLE IF EXISTS bot_params;

CREATE TABLE bot_params (
    param_name VARCHAR(50) PRIMARY KEY,
    param_value VARCHAR(50) DEFAULT NULL
);