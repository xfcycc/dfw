import { resolve } from 'node:path';
import { connectDatabase, databaseConfig, initializeDatabase } from './database.js';
import { importLegacy, readLegacy } from './sqlite-import.js';

// 输入可选 SQLite 路径，在空目标 MySQL 库内原子导入，不覆盖任何已有对局。
const rows = await readLegacy(resolve(process.argv[2] ?? 'data/game.sqlite'));
const admin = await connectDatabase(null);
try {
  const connection = await initializeDatabase(admin, databaseConfig.database);
  try { console.log(JSON.stringify({ database: databaseConfig.database, imported: await importLegacy(connection, rows), sourceChanged: false })); }
  finally { await connection.end(); }
} finally { await admin.end(); }
