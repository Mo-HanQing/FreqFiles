package com.mohanqing.freqfiles.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.mohanqing.freqfiles.ui.FreqFilesFloatingPanel;
import org.jetbrains.annotations.NotNull;

/**
 * 显示/隐藏悬浮面板的Action（切换模式）
 * 使用同一快捷键可以打开或关闭悬浮面板
 */
public class ShowFloatingPanelAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 获取或创建悬浮面板
        FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
        if (floatingPanel == null) {
            floatingPanel = new FreqFilesFloatingPanel(project);
            project.putUserData(FreqFilesFloatingPanel.KEY, floatingPanel);
        }
        
        // 切换模式：如果已显示则关闭，否则显示
        if (floatingPanel.isVisible()) {
            floatingPanel.hide();
        } else {
            floatingPanel.show(null);
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}

