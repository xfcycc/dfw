import mysql, { type Connection, type RowDataPacket } from 'mysql2/promise';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/** 本地工具所用 MySQL 配置，与 Java 的 DB_URL / DB_USERNAME / DB_PASSWORD 对齐。 */
export const databaseConfig = (() => {
  const url = new URL((process.env.DB_URL ?? 'jdbc:mysql://127.0.0.1:3306/dfw').replace(/^jdbc:/, ''));
  return { host: url.hostname, port: Number(url.port || 3306), database: decodeURIComponent(url.pathname.slice(1)), user: process.env.DB_USERNAME ?? 'root', password: process.env.DB_PASSWORD ?? '', charset: 'utf8mb4', multipleStatements: true };
})();

/** 输入数据库名称，返回经过标识符转义的名称，不用于值参数。 */
export function identifier(name: string): string { return '`' + name.replaceAll('`', '``') + '`'; }

/** 输入可选库名，连接 MySQL；空名称用于创建新库。 */
export async function connectDatabase(name: string | null = databaseConfig.database): Promise<Connection> {
  return await mysql.createConnection({ ...databaseConfig, database: name ?? undefined });
}

/** 输入管理员连接和目标库名，只创建缺失库及缺失表，返回目标连接。 */
export async function initializeDatabase(admin: Connection, name: string): Promise<Connection> {
  await admin.query(`CREATE DATABASE IF NOT EXISTS ${identifier(name)} CHARACTER SET utf8mb4 COLLATE utf8mb4_bin`);
  const connection = await connectDatabase(name);
  try { await connection.query(readFileSync(resolve('backend/src/main/resources/schema.sql'), 'utf8')); return connection; }
  catch (error) { await connection.end(); throw error; }
}

/** 输入目标连接，批量返回三个业务表的当前条数。 */
export async function rowCounts(connection: Connection): Promise<{ rooms: number; sessions: number; receipts: number }> {
  const [rows] = await connection.query<RowDataPacket[]>('SELECT (SELECT COUNT(*) FROM rooms) AS rooms, (SELECT COUNT(*) FROM sessions) AS sessions, (SELECT COUNT(*) FROM receipts) AS receipts');
  return { rooms: Number(rows[0].rooms), sessions: Number(rows[0].sessions), receipts: Number(rows[0].receipts) };
}
