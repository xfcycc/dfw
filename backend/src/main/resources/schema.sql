-- 只创建缺失表，不清理或覆盖已有对局；所有业务表使用事务引擎。
CREATE TABLE IF NOT EXISTS rooms (
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    UNIQUE KEY uq_rooms_id (room_id),
    state LONGTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS sessions (
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    KEY idx_sessions_room_player (room_id, player_id),
    player_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- 固定档案与恢复来源；六位号暂不回收，历史关联始终使用 room_id。
CREATE TABLE IF NOT EXISTS room_archives (
    room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    archive_code CHAR(26) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    source_snapshot_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    restore_request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    restore_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    restored_player_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    UNIQUE KEY uq_archive_code (archive_code),
    UNIQUE KEY uq_archive_room_code (code),
    UNIQUE KEY uq_archive_restore (source_room_id, restore_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS room_snapshots (
    snapshot_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    version BIGINT NOT NULL,
    turn_number INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    at BIGINT NOT NULL,
    format_version INT NOT NULL,
    rules_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    state LONGTEXT NOT NULL,
    public_state LONGTEXT NOT NULL,
    manual BOOLEAN NOT NULL DEFAULT FALSE,
    label TEXT NULL,
    UNIQUE KEY uq_snapshot_version (room_id, version),
    KEY idx_snapshot_game (room_id, game_id, version),
    KEY idx_snapshot_manual (room_id, manual, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS room_history (
    sequence BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    snapshot_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    game_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    version BIGINT NOT NULL,
    turn_number INT NULL,
    phase VARCHAR(32) NULL,
    actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    actor_name TEXT NULL,
    type VARCHAR(40) NOT NULL,
    text TEXT NOT NULL,
    at BIGINT NOT NULL,
    details LONGTEXT NULL,
    KEY idx_history_room (room_id, sequence),
    KEY idx_history_game (room_id, game_id, sequence),
    KEY idx_history_type (room_id, type, sequence),
    KEY idx_history_turn (room_id, game_id, turn_number, sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS receipts (
    code CHAR(6) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    command_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result LONGTEXT NOT NULL,
    PRIMARY KEY (code, command_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
