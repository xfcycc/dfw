import { spawn } from 'node:child_process';

// 输入继承的本地配置，启动 Java；信号转发保证房间正常保存后退出。
const server = spawn('java', ['-jar', 'backend/target/party-monopoly.jar'], { stdio: 'inherit', env: process.env });
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.kill(signal));
server.on('error', error => { console.error('Java 服务启动失败：', error.message); process.exitCode = 1; });
server.on('exit', code => { process.exitCode = code ?? 0; });
