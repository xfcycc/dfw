import type { Connection } from 'mysql2/promise';
import { DatabaseSync } from 'node:sqlite';
import { backup } from 'node:sqlite';
import { mkdtempSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { rowCounts } from './database.js';

/** 旧快照行，导入时保留 JSON 文本、身份摘要和已提交回执。 */
interface LegacyRows {
  rooms: { code: string; state: string }[];
  sessions: { token_hash: string; code: string; player_id: string | null }[];
  receipts: { code: string; command_id: string; actor_id: string; result: string }[];
}

/** 输入 SQLite 路径，以只读连接取得一致性备份并批量读取三张表，不改写源文件。 */
export async function readLegacy(path: string): Promise<LegacyRows> {
  const temporary = mkdtempSync(join(tmpdir(), 'dfw-sqlite-import-'));
  let source: DatabaseSync | undefined;
  let snapshot: DatabaseSync | undefined;
  try {
    source = new DatabaseSync(path, { readOnly: true });
    const destination = join(temporary, 'snapshot.sqlite'); await backup(source, destination);
    snapshot = new DatabaseSync(destination, { readOnly: true });
    return {
      rooms: snapshot.prepare('SELECT code,state FROM rooms').all() as unknown as LegacyRows['rooms'],
      sessions: snapshot.prepare('SELECT token_hash,code,player_id FROM sessions').all() as unknown as LegacyRows['sessions'],
      receipts: snapshot.prepare('SELECT code,command_id,actor_id,result FROM receipts').all() as unknown as LegacyRows['receipts'],
    };
  } finally { snapshot?.close(); source?.close(); rmSync(temporary, { recursive: true, force: true }); }
}

/** 输入 MySQL 连接和旧快照，目标必须为空；分批插入并整体事务提交，失败整体回滚。 */
export async function importLegacy(connection: Connection, rows: LegacyRows): Promise<{ rooms: number; sessions: number; receipts: number }> {
  const [locks] = await connection.query<import('mysql2/promise').RowDataPacket[]>("SELECT GET_LOCK(CONCAT('dfw_import:', LEFT(SHA2(DATABASE(), 256), 40)), 0) AS acquired");
  if (locks[0].acquired !== 1) throw new Error('另一个旧数据导入正在执行');
  try {
    await connection.beginTransaction();
    const counts = await rowCounts(connection);
    if (Object.values(counts).some(count => count > 0)) throw new Error('目标库已有对局或身份，拒绝覆盖；请停服后选择空目标库');
    // 只分批写入，不在循环中逐条查询。限制单批体积，避免快照超过 max_allowed_packet。
    const writes = [
      { sql: 'INSERT INTO rooms(code,state) VALUES ', values: rows.rooms.map(row => [row.code, row.state]) },
      { sql: 'INSERT INTO sessions(token_hash,code,player_id) VALUES ', values: rows.sessions.map(row => [row.token_hash, row.code, row.player_id]) },
      { sql: 'INSERT INTO receipts(code,command_id,actor_id,result) VALUES ', values: rows.receipts.map(row => [row.code, row.command_id, row.actor_id, row.result]) },
    ];
    for (const write of writes) {
      let batch: (string | null)[][] = []; let bytes = 0;
      for (const row of write.values) {
        const size = Buffer.byteLength(JSON.stringify(row)) * 2;
        if (batch.length && (batch.length >= 250 || bytes + size > 512 * 1024)) { await insertBatch(connection, write.sql, batch); batch = []; bytes = 0; }
        batch.push(row); bytes += size;
      }
      if (batch.length) await insertBatch(connection, write.sql, batch);
    }
    const imported = await rowCounts(connection);
    if (imported.rooms !== rows.rooms.length || imported.sessions !== rows.sessions.length || imported.receipts !== rows.receipts.length) throw new Error('导入数量不一致，已回滚');
    await connection.commit(); return imported;
  } catch (error) { await connection.rollback(); throw error; }
  finally { await connection.query("SELECT RELEASE_LOCK(CONCAT('dfw_import:', LEFT(SHA2(DATABASE(), 256), 40)))"); }
}

/** 输入可信 INSERT 前缀与一批行，构造占位符并服务端绑定所有值，不拼接快照文本。 */
async function insertBatch(connection: Connection, prefix: string, rows: (string | null)[][]): Promise<void> {
  const placeholders = rows.map(row => '(' + row.map(() => '?').join(',') + ')').join(',');
  await connection.execute(prefix + placeholders, rows.flat());
}
