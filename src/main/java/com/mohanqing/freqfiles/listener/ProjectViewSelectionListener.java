package com.mohanqing.freqfiles.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.mohanqing.freqfiles.manager.FreqFilesManager;
import com.mohanqing.freqfiles.ui.FreqFilesFloatingPanel;
import org.jetbrains.annotations.NotNull;

/**
 * 项目资源管理器选择监听器
 * 监听用户在项目资源管理器（Project View）中点击文件的事件
 * 通过监听选择变化来捕获文件选择
 */
public class ProjectViewSelectionListener {
    
    public static final Key<ProjectViewSelectionListener> KEY = Key.create("ProjectViewSelectionListener");
    
    private final Project project;
    private final FreqFilesManager manager;
    private MessageBusConnection connection;

    public ProjectViewSelectionListener(@NotNull Project project) {
        this.project = project;
        this.manager = FreqFilesManager.getInstance(project);
    }

    public void install() {
        // 注意：ProjectViewSelectionListener 原本是为了监听项目视图中的文件选择
        // 但实际上 FileEditorListener 已经监听了所有文件编辑器事件（包括标签栏切换和项目视图选择）
        // 为了避免重复记录，这里不再监听 FileEditorManagerListener
        // 如果需要单独监听项目视图的选择，需要使用其他方式（如 ProjectView 的 SelectionProvider）
        // 目前暂时不安装监听器，避免与 FileEditorListener 重复
        connection = project.getMessageBus().connect();
        // 暂时不订阅任何事件，避免重复记录
    }
    
    /**
     * 记录文件访问
     */
    private void recordFileAccess(@NotNull VirtualFile file) {
        // 记录访问
        manager.recordFileAccess(file);
        
        // 刷新悬浮窗
        ApplicationManager.getApplication().invokeLater(() -> {
            FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
            if (floatingPanel != null && floatingPanel.isVisible()) {
                floatingPanel.refreshLists();
            }
        });
    }
    
    public void dispose() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
}
