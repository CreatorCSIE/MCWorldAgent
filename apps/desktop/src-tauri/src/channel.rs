// T1 命令台文件原语（shell-t1-command-panel spec §2）。
// 纯 std::fs：投递=tmp→rename 原子（与 CLI/agent 同协议）、增量 tail、目录清单、
// 单文件读、证据行 append。轮询节奏全在前端（250ms，案卷 §3 判决：Windows
// rename 语义对 fs event 不稳）。零依赖新增。

use serde::Serialize;
use std::fs;
use std::io::{Read, Seek, SeekFrom, Write};
use std::path::Path;

fn e2s(e: std::io::Error) -> String {
    e.to_string()
}

/// tmp→rename 原子投递进 inbox。
#[tauri::command]
pub fn chan_post(dir: String, tmp_name: String, content: String) -> Result<(), String> {
    let d = Path::new(&dir);
    fs::create_dir_all(d.join("tmp")).map_err(e2s)?;
    fs::create_dir_all(d.join("inbox")).map_err(e2s)?;
    let tmp = d.join("tmp").join(format!("~{}", tmp_name));
    fs::write(&tmp, content).map_err(e2s)?;
    fs::rename(&tmp, d.join("inbox").join(&tmp_name)).map_err(e2s)
}

#[derive(Serialize)]
pub struct LinesRead {
    pub lines: Vec<String>,
    pub offset: u64,
    /// true=文件不存在（通道未就绪），前端不记错误。
    pub missing: bool,
}

/// 从 offset 增量读完整行（\\n 结尾才算完整；半行不消费、offset 不越过）。
/// 文件比 offset 小（轮转/截断）→ 自动回 0 重读。
#[tauri::command]
pub fn chan_read_lines(path: String, offset: u64) -> Result<LinesRead, String> {
    let p = Path::new(&path);
    if !p.is_file() {
        return Ok(LinesRead { lines: vec![], offset: 0, missing: true });
    }
    let mut f = fs::File::open(p).map_err(e2s)?;
    let len = f.metadata().map_err(e2s)?.len();
    let start = if offset > len { 0 } else { offset };
    f.seek(SeekFrom::Start(start)).map_err(e2s)?;
    let mut buf = Vec::new();
    f.read_to_end(&mut buf).map_err(e2s)?;
    let mut lines: Vec<String> = Vec::new();
    let mut consumed: u64 = 0;
    let mut cur = String::new();
    for byte in buf {
        consumed += 1;
        if byte == b'\n' {
            let s = cur.clone();
            cur.clear();
            lines.push(s.trim_end_matches('\r').to_string());
        } else {
            cur.push(byte as char); // UTF-8 多字节经 lossy 重建（日志行均 ASCII 面）
        }
    }
    Ok(LinesRead { lines, offset: start + consumed, missing: false })
}

/// 目录清单（outbox 轮询用；目录缺失=Err("ENOENT")，前端映射 not-ready）。
#[tauri::command]
pub fn chan_list_dir(path: String) -> Result<Vec<String>, String> {
    let rd = match fs::read_dir(Path::new(&path)) {
        Ok(rd) => rd,
        Err(e) => {
            if e.kind() == std::io::ErrorKind::NotFound {
                return Err("ENOENT".to_string());
            }
            return Err(e2s(e));
        }
    };
    let mut v = Vec::new();
    for entry in rd {
        if let Ok(name) = entry.map(|e| e.file_name().to_string_lossy().to_string()) {
            v.push(name);
        }
    }
    v.sort();
    Ok(v)
}

#[tauri::command]
pub fn chan_read_file(path: String) -> Result<String, String> {
    let bytes = fs::read(Path::new(&path)).map_err(e2s)?;
    Ok(String::from_utf8_lossy(&bytes).to_string())
}

/// 证据行 append（cmds-shell.txt，与 CLI 归档同等待遇）。
#[tauri::command]
pub fn chan_append(path: String, line: String) -> Result<(), String> {
    if let Some(parent) = Path::new(&path).parent() {
        fs::create_dir_all(parent).map_err(e2s)?;
    }
    let mut f = fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(&path)
        .map_err(e2s)?;
    writeln!(f, "{}", line).map_err(e2s)
}
