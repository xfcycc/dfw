import { connectDatabase, databaseConfig, initializeDatabase, rowCounts } from './database.js';

const admin = await connectDatabase(null);
try {
  const connection = await initializeDatabase(admin, databaseConfig.database);
  try { console.log(JSON.stringify({ database: databaseConfig.database, counts: await rowCounts(connection), message: '数据库与 InnoDB 表已就绪，未清理现有数据' })); }
  finally { await connection.end(); }
} finally { await admin.end(); }
