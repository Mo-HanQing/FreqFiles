package com.mohanqing.freqfiles.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 设置界面提供者
 */
public class FreqFilesSettingsConfigurableProvider extends ConfigurableProvider {
    
    @Nullable
    @Override
    public Configurable createConfigurable() {
        // 获取当前打开的项目
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return new FreqFilesSettingsConfigurable(openProjects[0]);
        }
        // 如果没有打开的项目，返回一个默认的 Configurable（虽然可能无法正常工作）
        // 但这样可以避免返回 null 导致的错误
        return new FreqFilesSettingsConfigurable(null);
    }
    
    @Override
    public boolean canCreateConfigurable() {
        // 确保至少有一个打开的项目
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        return openProjects.length > 0;
    }
}

