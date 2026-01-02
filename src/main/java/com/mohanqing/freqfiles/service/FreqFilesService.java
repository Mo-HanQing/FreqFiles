package com.mohanqing.freqfiles.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.mohanqing.freqfiles.state.FavoriteFilesState;
import com.mohanqing.freqfiles.state.FrequentFilesState;
import com.mohanqing.freqfiles.state.FreqFilesSettingsState;
import org.jetbrains.annotations.NotNull;

/**
 * 插件服务类，提供对状态管理的访问
 */
@Service(Service.Level.PROJECT)
public final class FreqFilesService {
    
    private final Project project;

    public FreqFilesService(@NotNull Project project) {
        this.project = project;
    }

    public static FreqFilesService getInstance(@NotNull Project project) {
        return project.getService(FreqFilesService.class);
    }

    public FavoriteFilesState getFavoriteState() {
        return project.getService(FavoriteFilesState.class);
    }

    public FrequentFilesState getFrequentState() {
        return project.getService(FrequentFilesState.class);
    }

    public FreqFilesSettingsState getSettingsState() {
        return project.getService(FreqFilesSettingsState.class);
    }

    public Project getProject() {
        return project;
    }
}

