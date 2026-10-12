-- 只创建缺失表，不清理或覆盖已有对局；所有业务表使用事务引擎。
CREATE TABLE IF NOT EXISTS rooms (
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    state LONGTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS sessions (
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    player_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS receipts (
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    command_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result LONGTEXT NOT NULL,
    PRIMARY KEY (code, command_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
