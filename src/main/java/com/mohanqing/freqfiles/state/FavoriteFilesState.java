package com.mohanqing.freqfiles.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 常用文件状态管理，负责持久化常用文件列表
 */
@State(
    name = "FavoriteFilesState",
    storages = @Storage("FavoriteFiles.xml")
)
@Service(Service.Level.PROJECT)
public final class FavoriteFilesState implements PersistentStateComponent<FavoriteFilesState> {
    
    public FavoriteFilesState() {
    }
    
    // 常用文件列表（用户手动选择的文件路径）
    public Set<String> favoriteFiles = new LinkedHashSet<>();

    @Override
    public @Nullable FavoriteFilesState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FavoriteFilesState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 添加常用文件
     */
    public void addFavoriteFile(String filePath) {
        favoriteFiles.add(filePath);
    }

    /**
     * 移除常用文件
     */
    public void removeFavoriteFile(String filePath) {
        favoriteFiles.remove(filePath);
    }

    /**
     * 清除所有常用文件
     */
    public void clearAll() {
        favoriteFiles.clear();
    }
}

