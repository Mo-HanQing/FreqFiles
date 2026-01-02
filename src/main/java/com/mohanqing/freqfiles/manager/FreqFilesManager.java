package com.mohanqing.freqfiles.manager;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.mohanqing.freqfiles.service.FreqFilesService;
import com.mohanqing.freqfiles.state.FavoriteFilesState;
import com.mohanqing.freqfiles.state.FrequentFilesState;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件频率管理器，提供文件操作的便捷方法
 */
public class FreqFilesManager {
    
    private final Project project;

    public FreqFilesManager(Project project) {
        this.project = project;
    }

    public static FreqFilesManager getInstance(Project project) {
        return new FreqFilesManager(project);
    }

    /**
     * 记录文件访问（只统计项目内的文件，使用相对路径）
     */
    public void recordFileAccess(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return;
        }
        
        // 只统计项目内的文件
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        
        String filePath = file.getPath();
        String relativePath = getRelativePath(filePath);
        if (relativePath == null) {
            // 文件不在项目内，不统计
            return;
        }
        
        FreqFilesService service = FreqFilesService.getInstance(project);
        FrequentFilesState frequentState = service.getFrequentState();
        
        // 增加文件频率（无论文件是否在排除列表中，都会增加频率）
        frequentState.incrementFileFrequency(relativePath);
        
        // 如果文件之前被从高频列表中排除，现在重新打开时，自动从排除列表中移除
        // 这样文件就可以重新出现在高频列表中（如果频率足够高）
        frequentState.includeInFrequent(relativePath);
    }

    /**
     * 添加常用文件
     * 如果文件在项目内，使用相对路径；否则使用绝对路径
     */
    public void addFavoriteFile(VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return;
        }
        
        String filePath = file.getPath();
        String pathToStore = getPathToStore(filePath);
        if (pathToStore == null) {
            // 如果无法获取路径，使用绝对路径
            pathToStore = filePath;
        }
        
        FreqFilesService service = FreqFilesService.getInstance(project);
        service.getFavoriteState().addFavoriteFile(pathToStore);
    }
    
    /**
     * 获取应该存储的路径
     * 如果文件在项目内，返回相对路径；否则返回绝对路径
     */
    private String getPathToStore(String absolutePath) {
        String relativePath = getRelativePath(absolutePath);
        if (relativePath != null) {
            // 文件在项目内，使用相对路径
            return relativePath;
        } else {
            // 文件不在项目内，使用绝对路径
            return absolutePath;
        }
    }
    
    /**
     * 获取文件相对于项目根目录的相对路径
     * 如果文件不在项目内，返回null
     */
    private String getRelativePath(String absolutePath) {
        try {
            String basePath = project.getBasePath();
            if (basePath != null) {
                Path base = Paths.get(basePath);
                Path file = Paths.get(absolutePath);
                if (file.startsWith(base)) {
                    return base.relativize(file).toString().replace('\\', '/');
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 移除常用文件
     */
    public void removeFavoriteFile(VirtualFile file) {
        if (file == null) {
            return;
        }
        
        String filePath = file.getPath();
        FreqFilesService service = FreqFilesService.getInstance(project);
        FavoriteFilesState favoriteState = service.getFavoriteState();
        
        // 尝试移除相对路径
        String relativePath = getRelativePath(filePath);
        if (relativePath != null) {
            favoriteState.removeFavoriteFile(relativePath);
        }
        
        // 也尝试移除绝对路径（以防存储的是绝对路径）
        favoriteState.removeFavoriteFile(filePath);
    }

    /**
     * 检查文件是否在常用文件列表中
     */
    public boolean isFavoriteFile(VirtualFile file) {
        if (file == null) {
            return false;
        }
        
        String filePath = file.getPath();
        FreqFilesService service = FreqFilesService.getInstance(project);
        FavoriteFilesState favoriteState = service.getFavoriteState();
        
        // 检查相对路径
        String relativePath = getRelativePath(filePath);
        if (relativePath != null && favoriteState.favoriteFiles.contains(relativePath)) {
            return true;
        }
        
        // 检查绝对路径（以防存储的是绝对路径）
        return favoriteState.favoriteFiles.contains(filePath);
    }
}

