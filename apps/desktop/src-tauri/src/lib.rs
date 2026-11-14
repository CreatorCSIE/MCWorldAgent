// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
mod channel;

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            greet,
            channel::chan_post,
            channel::chan_read_lines,
            channel::chan_list_dir,
            channel::chan_read_file,
            channel::chan_append
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
