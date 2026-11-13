#!/usr/bin/env node
/*
 * MCWorldAgent Phase 0 PoC harness — 路线 A：
 * 直接 require Attic 的 @attic/runtime 构建产物（CJS dist），
 * dryRunLaunch 拿 LaunchConfig，extraJvmArgs 注入 -javaagent，自行 spawn。
 * Attic 仓库零改动；不启动 sidecar/IPC；不依赖 Attic GUI。
 *
 * [2026-11-13] 默认启动通路已升级为 attic-cli（--jvm-arg 尾注入，方案见案卷 §10）；
 * 本 harness 保留为备用/实验路径（--java / --agent-first / --no-agent 等变量隔离实验）。
 *
 * 用法：
 *   node integrations/attic/launch-poc.cjs [--dry-run] [--timeout <s>] [--no-legacyfix] [--no-agent]
 */
'use strict';

const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { spawn } = require('child_process');

// ---------- 常量（勘察已核实，见 spec §2） ----------
const ATTIC_RUNTIME_DIST =
  'e:/4k/github-projects/Attic-Minecraft-Launcher/packages/runtime/dist/index.js';
const VERSION_ID = 'inf-20100327';
const JAR_SHA1 = '47b1b32430a211520993552ba0a5e00c1af44724'; // betacraft 基地原始件（三态准入=原始）
const JAVA8 = 'C:\\Program Files\\Java\\jdk1.8.0_202\\bin\\java.exe';

const REPO = path.resolve(__dirname, '..', '..');
const AGENT_JAR = path.join(REPO, 'java', 'agent', 'out', 'mcworldagent-agent.jar');
const ASM_JAR = path.join(REPO, 'java', 'agent', 'libs', 'asm-9.7.jar');
const OUT_DIR = path.join(__dirname, 'out');

const ROOT = path.join(process.env.APPDATA, '.attic'); // AtticRuntime 缺省数据根（同式）
const BETACRAFT_JAR = path.join(process.env.APPDATA, '.betacraft', 'versions', VERSION_ID + '.jar');
const STAGED_JAR = path.join(ROOT, 'versions', VERSION_ID + '.jar');

// ---------- 工具 ----------
function sha1(file) {
  return crypto.createHash('sha1').update(fs.readFileSync(file)).digest('hex');
}

function die(msg) {
  console.error('[launch-poc] FATAL: ' + msg);
  process.exit(2);
}

// ---------- 入位闸门（三态准入纪律：SHA1 对表，原始件才收） ----------
function ensureJarStaged(withAgent) {
  if (withAgent) {
    if (!fs.existsSync(AGENT_JAR)) die('agent jar missing, run java/agent/build.ps1 first: ' + AGENT_JAR);
    if (!fs.existsSync(ASM_JAR)) die('asm jar missing: ' + ASM_JAR);
  }
  if (fs.existsSync(STAGED_JAR)) {
    const h = sha1(STAGED_JAR);
    if (h !== JAR_SHA1) die('staged jar SHA1 mismatch: ' + h + ' != ' + JAR_SHA1);
    console.log('[gate] staged jar OK (SHA1 match, reused): ' + STAGED_JAR);
    return;
  }
  if (!fs.existsSync(BETACRAFT_JAR)) die('betacraft base jar missing: ' + BETACRAFT_JAR);
  const srcHash = sha1(BETACRAFT_JAR);
  if (srcHash !== JAR_SHA1) die('source jar SHA1 mismatch: ' + srcHash + ' != ' + JAR_SHA1);
  fs.mkdirSync(path.dirname(STAGED_JAR), { recursive: true });
  fs.copyFileSync(BETACRAFT_JAR, STAGED_JAR);
  const dstHash = sha1(STAGED_JAR);
  if (dstHash !== JAR_SHA1) die('copied jar SHA1 mismatch: ' + dstHash);
  console.log('[gate] staged jar copied + verified: ' + STAGED_JAR);
}

// ---------- 主流程 ----------
async function main() {
  const argv = process.argv.slice(2);
  const dryRun = argv.includes('--dry-run');
  const noLegacyfix = argv.includes('--no-legacyfix');
  const noAgent = argv.includes('--no-agent'); // 基线对照：不挂我们的 agent
  const tIdx = argv.indexOf('--timeout');
  const timeoutS = tIdx >= 0 ? parseInt(argv[tIdx + 1], 10) : 180;
  // 变量隔离实验开关（run5 ticks=0 定位）：--java 换 JVM、--agent-first 把我们的
  // javaagent 前置到 legacyfix 之前（复刻 CLI 的 launchArgs 顺序）、--out 分目录落日志。
  const jIdx = argv.indexOf('--java');
  const javaExe = jIdx >= 0 ? argv[jIdx + 1] : JAVA8;
  const agentFirst = argv.includes('--agent-first');
  const oIdx = argv.indexOf('--out');
  const outDir = oIdx >= 0 ? path.resolve(argv[oIdx + 1]) : OUT_DIR;

  ensureJarStaged(!noAgent);
  fs.mkdirSync(outDir, { recursive: true });

  const transformLog = path.join(outDir, 'agent-transform.log');
  const hooksLog = path.join(outDir, 'agent-hooks.log');

  const { AtticRuntime } = require(ATTIC_RUNTIME_DIST);
  const runtime = new AtticRuntime({ version: '0.1.0' });

  const agentArg = noAgent ? null :
    // MCA_AGENT_EXTRA: 调试开关（如 disable=world-ctor;dump=<dir>），追加进 agent 参数串
    '-javaagent:' + AGENT_JAR + '=log=' + transformLog + ';hooks=' + hooksLog
      + (process.env.MCA_AGENT_EXTRA ? ';' + process.env.MCA_AGENT_EXTRA : '');

  const config = await runtime.dryRunLaunch({
    versionId: VERSION_ID,
    javaPath: javaExe,
    extraJvmArgs: agentArg && !agentFirst ? [agentArg] : [],
  });

  let jvmArgs = config.jvmArgs;
  if (noLegacyfix) {
    jvmArgs = jvmArgs.filter((a) => !(a.startsWith('-javaagent:') && a.includes('legacyfix')));
    console.log('[poc] legacyfix javaagent removed (--no-legacyfix)');
  }
  if (agentArg && agentFirst) {
    jvmArgs = [agentArg, ...jvmArgs];
    console.log('[poc] our javaagent prepended before legacyfix (--agent-first)');
  }

  // agent 依赖经 classpath 追加（0327 单系统类加载器；ASM 由 premain 类加载器解析）
  const classpath = noAgent ? [...config.classpath] : [...config.classpath, AGENT_JAR, ASM_JAR];

  if (dryRun) {
    console.log(JSON.stringify({ ...config, jvmArgs, classpath, gameDir: config.gameDir }, null, 2));
    return;
  }

  const cmd = [
    ...jvmArgs,
    '-cp', classpath.join(path.delimiter),
    config.mainClass,
    ...config.gameArgs,
  ];
  console.log('[poc] spawn: ' + config.javaPath);
  console.log('[poc] args: ' + JSON.stringify(cmd));
  console.log('[poc] cwd : ' + config.gameDir);

  const child = spawn(config.javaPath, cmd, {
    cwd: config.gameDir,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  console.log('[poc] pid  : ' + child.pid);
  const out = fs.createWriteStream(path.join(outDir, 'game-stdout.log'));
  const err = fs.createWriteStream(path.join(outDir, 'game-stderr.log'));
  child.stdout.pipe(out);
  child.stderr.pipe(err);

  const killer = setTimeout(() => {
    console.log('[poc] timeout ' + timeoutS + 's -> kill');
    child.kill();
  }, timeoutS * 1000);

  child.on('error', (e) => {
    clearTimeout(killer);
    die('spawn failed: ' + e.message);
  });
  child.on('exit', (code, signal) => {
    clearTimeout(killer);
    console.log('[poc] game exit code=' + code + ' signal=' + signal);
    console.log('[poc] logs: ' + outDir);
  });
}

main().catch((e) => die(e && e.stack ? e.stack : String(e)));
