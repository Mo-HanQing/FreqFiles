package com.mohanqing.freqfiles.activity;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.mohanqing.freqfiles.listener.FileEditorListener;
import com.mohanqing.freqfiles.listener.ProjectViewSelectionListener;
import org.jetbrains.annotations.NotNull;

/**
 * 启动活动，在项目打开时初始化监听器
 */
public class FreqFilesStartupActivity implements StartupActivity.DumbAware {
    
    @Override
    public void runActivity(@NotNull Project project) {
        // 初始化文件编辑器监听器（监听文件打开和标签栏选择）
        FileEditorListener editorListener = new FileEditorListener(project);
        editorListener.install();
        
        // 初始化项目视图选择监听器（监听项目资源管理器中的文件选择）
        ProjectViewSelectionListener projectViewListener = new ProjectViewSelectionListener(project);
        projectViewListener.install();
        
        // 将监听器保存到项目中，以便后续清理
        project.putUserData(FileEditorListener.KEY, editorListener);
        project.putUserData(ProjectViewSelectionListener.KEY, projectViewListener);
    }
}
