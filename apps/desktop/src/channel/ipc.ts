/** Tauri channel command 薄封装（shell-t1 spec §2 面）。浏览器 dev 环境自动识别，上层回退 mock。 */
import { invoke } from '@tauri-apps/api/core'

export function inTauri(): boolean {
  return typeof (window as unknown as Record<string, unknown>).__TAURI_INTERNALS__ !== 'undefined'
}

export interface LinesRead {
  lines: string[]
  offset: number
  missing: boolean
}

export const chan = {
  post: (dir: string, tmpName: string, content: string) =>
    invoke<void>('chan_post', { dir, tmpName, content }),
  readLines: (path: string, offset: number) =>
    invoke<LinesRead>('chan_read_lines', { path, offset }),
  listDir: (path: string) => invoke<string[]>('chan_list_dir', { path }),
  readFile: (path: string) => invoke<string>('chan_read_file', { path }),
  append: (path: string, line: string) => invoke<void>('chan_append', { path, line }),
}
