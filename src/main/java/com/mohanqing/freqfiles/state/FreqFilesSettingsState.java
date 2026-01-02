package com.mohanqing.freqfiles.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 插件设置状态管理，保存用户自定义的权重配置
 */
@State(
    name = "FreqFilesSettingsState",
    storages = @Storage("FreqFilesSettings.xml")
)
@Service(Service.Level.PROJECT)
public final class FreqFilesSettingsState implements PersistentStateComponent<FreqFilesSettingsState> {
    
    public FreqFilesSettingsState() {
        // 默认值：点击次数权重 40%，停留时间权重 60%
        frequencyWeight = 40;
        stayTimeWeight = 60;
    }
    
    // 点击次数权重（0-100）
    public int frequencyWeight = 40;
    
    // 停留时间权重（0-100）
    public int stayTimeWeight = 60;

    @Override
    public @Nullable FreqFilesSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FreqFilesSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * 获取点击次数的权重比例（0.0-1.0）
     */
    public double getFrequencyWeightRatio() {
        int total = frequencyWeight + stayTimeWeight;
        return total > 0 ? (double) frequencyWeight / total : 0.4;
    }
    
    /**
     * 获取停留时间的权重比例（0.0-1.0）
     */
    public double getStayTimeWeightRatio() {
        int total = frequencyWeight + stayTimeWeight;
        return total > 0 ? (double) stayTimeWeight / total : 0.6;
    }
}

