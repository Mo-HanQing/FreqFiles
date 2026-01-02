package com.mohanqing.freqfiles.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.mohanqing.freqfiles.manager.FreqFilesManager;
import com.mohanqing.freqfiles.service.FreqFilesService;
import com.mohanqing.freqfiles.state.FrequentFilesState;
import com.mohanqing.freqfiles.ui.FreqFilesFloatingPanel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件编辑器监听器，监听文件打开事件并统计使用频率
 * 包括：文件在编辑器中打开、标签栏选择、项目资源管理器选择
 * 同时记录文件停留时间
 */
public class FileEditorListener implements FileEditorManagerListener {
    
    public static final Key<FileEditorListener> KEY = Key.create("FileEditorListener");
    
    private final Project project;
    private final FreqFilesManager manager;
    private MessageBusConnection connection;
    
    // 记录每个文件打开的时间（文件路径 -> 打开时间戳）
    private final Map<String, Long> fileOpenTime = new HashMap<>();
    
    // 记录当前选中的文件（用于处理标签切换）
    private String currentSelectedFile = null;
    
    // 定时器：定期累计当前文件的停留时间并刷新列表
    private Timer stayTimeTimer = null;
    
    // 定时器间隔（毫秒）：每30秒累计一次停留时间
    private static final int TIMER_INTERVAL_MS = 30000;

    public FileEditorListener(@NotNull Project project) {
        this.project = project;
        this.manager = FreqFilesManager.getInstance(project);
    }

    public void install() {
        // 通过消息总线订阅文件编辑器事件，保存连接以确保监听器持续有效
        connection = project.getMessageBus().connect();
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
        
        // 启动定时器，定期累计当前文件的停留时间
        startStayTimeTimer();
    }
    
    public void dispose() {
        // 停止定时器
        stopStayTimeTimer();
        
        // 断开连接
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
    
    /**
     * 启动定时器，定期累计当前文件的停留时间
     */
    private void startStayTimeTimer() {
        stopStayTimeTimer(); // 先停止之前的定时器
        
        stayTimeTimer = new Timer(TIMER_INTERVAL_MS, e -> {
            // 定期累计当前选中文件的停留时间
            if (currentSelectedFile != null && fileOpenTime.containsKey(currentSelectedFile)) {
                Long openTime = fileOpenTime.get(currentSelectedFile);
                if (openTime != null) {
                    long currentTime = System.currentTimeMillis();
                    long stayTime = currentTime - openTime;
                    
                    // 只记录有效的停留时间（大于0且小于24小时，避免异常数据）
                    if (stayTime > 0 && stayTime < 24 * 60 * 60 * 1000L) {
                        FreqFilesService service = FreqFilesService.getInstance(project);
                        FrequentFilesState frequentState = service.getFrequentState();
                        
                        // 累计停留时间（在原有时间上继续累计）
                        frequentState.addFileStayTime(currentSelectedFile, stayTime);
                        
                        // 更新打开时间为当前时间，继续计时
                        fileOpenTime.put(currentSelectedFile, currentTime);
                        
                        // 刷新悬浮窗列表
                        ApplicationManager.getApplication().invokeLater(() -> {
                            FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
                            if (floatingPanel != null && floatingPanel.isVisible()) {
                                floatingPanel.refreshLists();
                            }
                        });
                    }
                }
            }
        });
        stayTimeTimer.setRepeats(true);
        stayTimeTimer.start();
    }
    
    /**
     * 停止定时器
     */
    private void stopStayTimeTimer() {
        if (stayTimeTimer != null) {
            stayTimeTimer.stop();
            stayTimeTimer = null;
        }
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 当文件被打开时，不在这里记录访问，避免重复记录
        // 访问记录在 selectionChanged 中统一处理
        
        // 如果这是当前选中的文件，记录打开时间
        String relativePath = getRelativePath(file);
        if (relativePath != null && !file.isDirectory()) {
            // 如果还没有记录打开时间，则记录（可能是新打开的文件）
            if (!fileOpenTime.containsKey(relativePath)) {
                recordFileOpenTime(file);
            }
            // 更新当前选中的文件
            currentSelectedFile = relativePath;
        }
    }

    @Override
    public void selectionChanged(@NotNull com.intellij.openapi.fileEditor.FileEditorManagerEvent event) {
        // 当切换文件标签时，需要：
        // 1. 如果之前有选中的文件，停止计时并累计停留时间
        // 2. 记录新选中文件的打开时间（如果之前打开过，继续累计；如果是新打开，重新开始计时）
        VirtualFile newFile = event.getNewFile();
        if (newFile != null && !newFile.isDirectory()) {
            String newFileRelativePath = getRelativePath(newFile);
            
            // 如果之前有选中的文件，且不是同一个文件，则停止计时并累计停留时间
            if (currentSelectedFile != null && 
                !currentSelectedFile.equals(newFileRelativePath) &&
                fileOpenTime.containsKey(currentSelectedFile)) {
                // 停止之前文件的计时，累计停留时间（但不移除打开时间，因为文件还在打开状态）
                recordFileStayTime(currentSelectedFile);
            }
            
            // 记录新文件的访问
            recordFileAccess(newFile);
            
            // 如果新文件之前没有记录打开时间，则记录（开始计时）
            // 如果之前有记录，说明是切换回来的，继续计时（不重新记录打开时间）
            if (!fileOpenTime.containsKey(newFileRelativePath)) {
                recordFileOpenTime(newFile);
            }
            
            // 更新当前选中的文件
            currentSelectedFile = newFileRelativePath;
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 文件关闭时，记录停留时间
        if (file != null && !file.isDirectory()) {
            String relativePath = getRelativePath(file);
            if (relativePath != null) {
                recordFileCloseTime(relativePath);
                fileOpenTime.remove(relativePath);
                
                // 如果关闭的是当前选中的文件，清除选中状态
                if (currentSelectedFile != null && currentSelectedFile.equals(relativePath)) {
                    currentSelectedFile = null;
                }
            }
        }
    }
    
    /**
     * 记录文件访问的统一处理
     */
    private void recordFileAccess(@NotNull VirtualFile file) {
        // 记录访问
        manager.recordFileAccess(file);
        
        // 立即刷新悬浮窗，确保列表实时更新
        // 使用 invokeLater 确保在 EDT 线程上执行
        ApplicationManager.getApplication().invokeLater(() -> {
            FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
            if (floatingPanel != null && floatingPanel.isVisible()) {
                floatingPanel.refreshLists();
            }
        });
    }
    
    /**
     * 记录文件打开时间
     */
    private void recordFileOpenTime(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
            return;
        }
        
        String relativePath = getRelativePath(file);
        if (relativePath == null) {
            return;
        }
        
        // 记录打开时间（如果已经记录过，不覆盖，避免重复计算）
        if (!fileOpenTime.containsKey(relativePath)) {
            fileOpenTime.put(relativePath, System.currentTimeMillis());
        }
    }
    
    /**
     * 停止文件计时并累计停留时间（用于文件切换，不是关闭）
     * 累计停留时间，但保留打开时间记录，这样下次切换回来时可以继续计时
     */
    private void recordFileStayTime(String relativePath) {
        if (relativePath == null) {
            return;
        }
        
        Long openTime = fileOpenTime.get(relativePath);
        if (openTime != null) {
            long currentTime = System.currentTimeMillis();
            long stayTime = currentTime - openTime;
            
            // 只记录有效的停留时间（大于0且小于24小时，避免异常数据）
            if (stayTime > 0 && stayTime < 24 * 60 * 60 * 1000L) {
                FreqFilesService service = FreqFilesService.getInstance(project);
                FrequentFilesState frequentState = service.getFrequentState();
                
                // 累计停留时间（在原有时间上继续累计）
                frequentState.addFileStayTime(relativePath, stayTime);
                
                // 更新打开时间为当前时间，这样下次切换回来时继续计时
                // 这样累计的时间就是：从上次切换到这次切换之间的时间
                fileOpenTime.put(relativePath, currentTime);
            }
        }
    }
    
    /**
     * 记录文件关闭时间并计算停留时间（用于文件关闭）
     * 累计停留时间，并移除打开时间记录
     */
    private void recordFileCloseTime(String relativePath) {
        if (relativePath == null) {
            return;
        }
        
        Long openTime = fileOpenTime.get(relativePath);
        if (openTime != null) {
            long closeTime = System.currentTimeMillis();
            long stayTime = closeTime - openTime;
            
            // 只记录有效的停留时间（大于0且小于24小时，避免异常数据）
            if (stayTime > 0 && stayTime < 24 * 60 * 60 * 1000L) {
                FreqFilesService service = FreqFilesService.getInstance(project);
                FrequentFilesState frequentState = service.getFrequentState();
                // 累计停留时间（在原有时间上继续累计）
                frequentState.addFileStayTime(relativePath, stayTime);
            }
            
            // 文件关闭时，移除打开时间记录
            fileOpenTime.remove(relativePath);
        }
    }
    
    /**
     * 获取文件相对于项目根目录的相对路径
     */
    private String getRelativePath(VirtualFile file) {
        if (file == null) {
            return null;
        }
        
        try {
            String basePath = project.getBasePath();
            if (basePath == null) {
                return null;
            }
            
            String filePath = file.getPath();
            Path base = Paths.get(basePath);
            Path filePathObj = Paths.get(filePath);
            if (filePathObj.startsWith(base)) {
                return base.relativize(filePathObj).toString().replace('\\', '/');
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

