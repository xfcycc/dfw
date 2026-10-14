package com.dfw.infrastructure;

import java.util.Set;
import java.util.HashSet;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 启动时只增加缺失的身份关联列，不清理、覆盖已有数据。 */
@Component
@DependsOnDatabaseInitialization
public class ArchiveSchema {
    /** 输入数据源访问器，一次读取列元数据后增加缺失列和索引。 */
    public ArchiveSchema(JdbcTemplate jdbc) {
        Set<String> required = new HashSet<>();
        Set<String> columns = new HashSet<>(jdbc.query("SELECT CONCAT(TABLE_NAME,'.',COLUMN_NAME),IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('rooms','sessions','room_history')", (rs, row) -> { if ("NO".equals(rs.getString(2))) required.add(rs.getString(1)); return rs.getString(1); }));
        if (!columns.contains("rooms.room_id")) jdbc.execute("ALTER TABLE rooms ADD COLUMN room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL, ADD UNIQUE KEY uq_rooms_id(room_id)");
        if (!columns.contains("sessions.room_id")) jdbc.execute("ALTER TABLE sessions ADD COLUMN room_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL, ADD KEY idx_sessions_room_player(room_id,player_id)");
        if (required.contains("room_history.turn_number")) jdbc.execute("ALTER TABLE room_history MODIFY COLUMN turn_number INT NULL");
    }
}
