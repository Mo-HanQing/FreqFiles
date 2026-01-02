package com.mohanqing.freqfiles.state;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 高频文件状态管理，负责持久化文件使用频率统计
 */
@State(
    name = "FrequentFilesState",
    storages = @Storage("FrequentFiles.xml")
)
@Service(Service.Level.PROJECT)
public final class FrequentFilesState implements PersistentStateComponent<FrequentFilesState> {
    
    public FrequentFilesState() {
    }
    
    // 文件使用频率统计（文件路径 -> 点击次数）
    public Map<String, Integer> fileFrequency = new HashMap<>();
    
    // 文件最后访问时间（用于排序）
    public Map<String, Long> fileLastAccessTime = new HashMap<>();
    
    // 文件累计停留时间（文件路径 -> 累计停留时间（毫秒））
    public Map<String, Long> fileTotalStayTime = new HashMap<>();
    
    // 被用户手动从高频列表中移除的文件（黑名单）
    public Set<String> excludedFromFrequent = new HashSet<>();

    @Override
    public @Nullable FrequentFilesState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FrequentFilesState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 增加文件使用频率
     */
    public void incrementFileFrequency(String filePath) {
        fileFrequency.put(filePath, fileFrequency.getOrDefault(filePath, 0) + 1);
        fileLastAccessTime.put(filePath, System.currentTimeMillis());
    }

    /**
     * 增加文件累计停留时间（毫秒）
     */
    public void addFileStayTime(String filePath, long stayTimeMs) {
        long currentTotal = fileTotalStayTime.getOrDefault(filePath, 0L);
        long newTotal = currentTotal + stayTimeMs;
        fileTotalStayTime.put(filePath, newTotal);
    }
    
    /**
     * 获取高频文件列表（综合考虑点击次数和停留时间）
     * 排序规则：
     * 1. 计算综合得分 = 点击次数 * 0.4 + 停留时间得分 * 0.6
     * 2. 停留时间得分 = 停留时间（分钟）的归一化值（0-1）
     * 3. 如果综合得分相同，按最近访问时间排序
     */
    public List<String> getTopFrequentFiles(int limit, Set<String> favoriteFiles, 
                                             double frequencyWeight, double stayTimeWeight) {
        List<String> files = new ArrayList<>(fileFrequency.keySet());
        files.removeAll(favoriteFiles); // 排除常用文件列表中的文件
        files.removeAll(excludedFromFrequent); // 排除被用户手动移除的文件
        
        if (files.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 计算最大点击次数和最大停留时间（用于归一化）
        int maxFrequency = files.stream()
            .mapToInt(f -> fileFrequency.getOrDefault(f, 0))
            .max()
            .orElse(1);
        
        long maxStayTime = files.stream()
            .mapToLong(f -> fileTotalStayTime.getOrDefault(f, 0L))
            .max()
            .orElse(1L);
        
        // 如果最大值都为0，则使用默认值避免除零
        if (maxFrequency == 0) maxFrequency = 1;
        if (maxStayTime == 0) maxStayTime = 1;
        
        final int finalMaxFrequency = maxFrequency;
        final long finalMaxStayTime = maxStayTime;
        
        files.sort((f1, f2) -> {
            // 获取点击次数和停留时间
            int freq1 = fileFrequency.getOrDefault(f1, 0);
            int freq2 = fileFrequency.getOrDefault(f2, 0);
            long stayTime1 = fileTotalStayTime.getOrDefault(f1, 0L);
            long stayTime2 = fileTotalStayTime.getOrDefault(f2, 0L);
            
            // 归一化：将点击次数和停留时间转换为0-1之间的值
            double normalizedFreq1 = (double) freq1 / finalMaxFrequency;
            double normalizedFreq2 = (double) freq2 / finalMaxFrequency;
            double normalizedStay1 = (double) stayTime1 / finalMaxStayTime;
            double normalizedStay2 = (double) stayTime2 / finalMaxStayTime;
            
            // 综合得分：使用传入的权重参数
            double score1 = normalizedFreq1 * frequencyWeight + normalizedStay1 * stayTimeWeight;
            double score2 = normalizedFreq2 * frequencyWeight + normalizedStay2 * stayTimeWeight;
            
            
            // 首先按综合得分排序（降序）
            int scoreCompare = Double.compare(score2, score1);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            
            // 综合得分相同时，按最近访问时间排序（降序，最近访问的在前）
            long time1 = fileLastAccessTime.getOrDefault(f1, 0L);
            long time2 = fileLastAccessTime.getOrDefault(f2, 0L);
            return Long.compare(time2, time1);
        });
        
        int size = Math.min(limit, files.size());
        return files.subList(0, size);
    }
    
    /**
     * 从高频列表中排除文件（但不删除统计）
     */
    public void excludeFromFrequent(String filePath) {
        excludedFromFrequent.add(filePath);
    }
    
    /**
     * 重新包含文件到高频列表（如果之前被排除）
     */
    public void includeInFrequent(String filePath) {
        excludedFromFrequent.remove(filePath);
    }
    
    /**
     * 从频率统计中移除文件
     */
    public void removeFileFrequency(String filePath) {
        fileFrequency.remove(filePath);
        fileLastAccessTime.remove(filePath);
        fileTotalStayTime.remove(filePath);
    }

    /**
     * 清除所有高频文件统计
     */
    public void clearAll() {
        fileFrequency.clear();
        fileLastAccessTime.clear();
        fileTotalStayTime.clear();
        excludedFromFrequent.clear();
    }
}

