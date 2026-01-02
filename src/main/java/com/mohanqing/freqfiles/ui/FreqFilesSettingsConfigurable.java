package com.mohanqing.freqfiles.ui;

import com.mohanqing.freqfiles.util.FreqFilesBundle;
import com.mohanqing.freqfiles.state.FreqFilesSettingsState;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 插件设置界面
 */
public class FreqFilesSettingsConfigurable implements Configurable {
    
    private final Project project;
    private FreqFilesSettingsPanel settingsPanel;
    
    public FreqFilesSettingsConfigurable(@Nullable Project project) {
        this.project = project;
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return FreqFilesBundle.message("freqfiles.settings.configurable.display.name");
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        if (project == null) {
            return new JLabel("No project is open");
        }
        settingsPanel = new FreqFilesSettingsPanel(project);
        return settingsPanel.getPanel();
    }
    
    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }
        FreqFilesSettingsState settings = project.getService(FreqFilesSettingsState.class);
        return settingsPanel.getFrequencyWeight() != settings.frequencyWeight ||
               settingsPanel.getStayTimeWeight() != settings.stayTimeWeight;
    }
    
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel != null && project != null) {
            FreqFilesSettingsState settings = project.getService(FreqFilesSettingsState.class);
            settings.frequencyWeight = settingsPanel.getFrequencyWeight();
            settings.stayTimeWeight = settingsPanel.getStayTimeWeight();
            
            // 刷新悬浮窗列表
            FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
            if (floatingPanel != null) {
                floatingPanel.refreshLists();
            }
        }
    }
    
    @Override
    public void reset() {
        if (settingsPanel != null && project != null) {
            FreqFilesSettingsState settings = project.getService(FreqFilesSettingsState.class);
            settingsPanel.setFrequencyWeight(settings.frequencyWeight);
            settingsPanel.setStayTimeWeight(settings.stayTimeWeight);
        }
    }
}

