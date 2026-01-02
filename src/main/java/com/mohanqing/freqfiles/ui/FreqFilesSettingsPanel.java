package com.mohanqing.freqfiles.ui;

import com.mohanqing.freqfiles.util.FreqFilesBundle;
import com.mohanqing.freqfiles.state.FreqFilesSettingsState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * 设置面板UI - 使用单个滑动条，两端分别显示两个权重
 */
public class FreqFilesSettingsPanel {
    
    private final Project project;
    private JPanel panel;
    private JSlider weightSlider;
    private JLabel frequencyValueLabel;
    private JLabel stayTimeValueLabel;
    
    public FreqFilesSettingsPanel(@NotNull Project project) {
        this.project = project;
        createPanel();
        loadSettings();
    }
    
    private void createPanel() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        
        // 标题
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JBLabel titleLabel = new JBLabel(FreqFilesBundle.message("freqfiles.settings.title"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize() + 2));
        panel.add(titleLabel, gbc);
        
        // 滑动条容器
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 左侧标签和值（点击次数权重）
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.add(new JBLabel(FreqFilesBundle.message("freqfiles.settings.click.weight")));
        frequencyValueLabel = new JBLabel("40%");
        frequencyValueLabel.setFont(frequencyValueLabel.getFont().deriveFont(Font.BOLD));
        frequencyValueLabel.setForeground(new Color(70, 130, 180)); // 蓝色
        leftPanel.add(frequencyValueLabel);
        sliderPanel.add(leftPanel, BorderLayout.WEST);
        
        // 中间滑动条
        weightSlider = new JSlider(0, 100, 40);
        weightSlider.setMajorTickSpacing(20);
        weightSlider.setMinorTickSpacing(5);
        weightSlider.setPaintTicks(true);
        weightSlider.setPaintLabels(true);
        weightSlider.addChangeListener(e -> {
            int frequencyWeight = weightSlider.getValue();
            int stayTimeWeight = 100 - frequencyWeight;
            frequencyValueLabel.setText(frequencyWeight + "%");
            stayTimeValueLabel.setText(stayTimeWeight + "%");
        });
        sliderPanel.add(weightSlider, BorderLayout.CENTER);
        
        // 右侧标签和值（停留时间权重）
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        stayTimeValueLabel = new JBLabel("60%");
        stayTimeValueLabel.setFont(stayTimeValueLabel.getFont().deriveFont(Font.BOLD));
        stayTimeValueLabel.setForeground(new Color(220, 20, 60)); // 红色
        rightPanel.add(stayTimeValueLabel);
        rightPanel.add(new JBLabel(":" + FreqFilesBundle.message("freqfiles.settings.stay.weight")));
        sliderPanel.add(rightPanel, BorderLayout.EAST);
        
        panel.add(sliderPanel, gbc);
    }
    
    private void loadSettings() {
        FreqFilesSettingsState settings = project.getService(FreqFilesSettingsState.class);
        // 从设置中读取点击次数权重（0-100）
        int frequencyWeight = settings.frequencyWeight;
        weightSlider.setValue(frequencyWeight);
        int stayTimeWeight = 100 - frequencyWeight;
        frequencyValueLabel.setText(frequencyWeight + "%");
        stayTimeValueLabel.setText(stayTimeWeight + "%");
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    public int getFrequencyWeight() {
        return weightSlider.getValue();
    }
    
    public int getStayTimeWeight() {
        return 100 - weightSlider.getValue();
    }
    
    public void setFrequencyWeight(int weight) {
        weightSlider.setValue(weight);
        int stayTimeWeight = 100 - weight;
        frequencyValueLabel.setText(weight + "%");
        stayTimeValueLabel.setText(stayTimeWeight + "%");
    }
    
    public void setStayTimeWeight(int weight) {
        int frequencyWeight = 100 - weight;
        weightSlider.setValue(frequencyWeight);
        frequencyValueLabel.setText(frequencyWeight + "%");
        stayTimeValueLabel.setText(weight + "%");
    }
}

