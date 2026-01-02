# FreqFiles - IntelliJ IDEA File Frequency Statistics Plugin

A powerful file management plugin for IntelliJ IDEA that helps you quickly access your favorite and frequently used files. Boost your productivity by organizing and accessing your most important files with ease.

## Features

### 🎯 Favorite Files
- Manually select and save your frequently used files
- Add files from both within and outside your project
- Organize files with drag-and-drop reordering (up/down buttons)
- Quick access with double-click to open files
- Clear all favorite files with one click

### 📊 Frequent Files
- Automatically tracks file usage statistics
- Records both click count and stay time for each file
- Intelligently ranks files based on usage patterns
- Displays top 50 most frequently accessed files
- Shows statistics: file name, click count, and stay time (in English format: ms, s, m, h)
- Remove files from frequent list (statistics remain for future tracking)
- Clear all statistics with one click

### 🎨 Floating Panel
- Resizable floating panel for quick file access
- Toggle with `Shift+Alt+F` keyboard shortcut
- Stays visible while you work (doesn't close when clicking outside)
- Split view: Favorite Files (left, 30%) and Frequent Files (right, 70%)
- Draggable divider to adjust panel proportions
- Scrollbars with thin design for better UI
- Dynamic exit hint showing current shortcut

### ⚙️ Customizable Settings
- Fine-tune the ranking algorithm with weight ratio settings
- Adjust the balance between click count weight and stay time weight
- Real-time percentage display while adjusting
- Sum of weights always equals 100%
- Access via: **File → Settings → Tools → FreqFiles**

### 🔗 Context Menu Integration
- Right-click any file in:
  - Project view
  - Editor tabs
  - Editor content
- Quick option to add file to favorites
- Integrated seamlessly into IDE menus

### 💾 Persistent Storage
- All favorite files and statistics are automatically saved
- Data persists across IDE restarts
- Stored in project configuration files

## Usage

### Opening the Floating Panel

1. Press `Shift+Alt+F` to toggle the floating panel
   - Or use: **Tools → Toggle Quick File Panel**

### Managing Favorite Files

1. **Adding files**:
   - Click the **+** button in the Favorite Files panel
   - Select files from file chooser (supports files outside project)
   - Or right-click any file → **Add to Favorite Files**

2. **Removing files**:
   - Select a file and click **-** button to remove
   - Click the trash icon to clear all favorite files

3. **Reordering files**:
   - Select a file and use **↑** (up) or **↓** (down) buttons

4. **Opening files**:
   - Double-click any file in the list

### Viewing Frequent Files

1. The Frequent Files panel automatically displays your most-used files
2. Statistics shown for each file:
   - Click count
   - Stay time (formatted as: ms, s, m, h)
3. **Removing from list**:
   - Select a file and click **-** button (statistics remain)
   - Click the trash icon to clear all statistics
4. **Opening files**:
   - Double-click any file in the list

### Configuring Weight Settings

1. Go to **File → Settings → Tools → FreqFiles**
2. Adjust the slider to set the weight ratio:
   - **Click Count Weight**: How much click count affects ranking
   - **Stay Time Weight**: How much stay time affects ranking
3. The sum always equals 100%
4. Default: 40% click count, 60% stay time
5. Changes apply immediately to the ranking algorithm
