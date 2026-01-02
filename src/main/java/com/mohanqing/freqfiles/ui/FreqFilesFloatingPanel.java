package com.mohanqing.freqfiles.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.mohanqing.freqfiles.util.FreqFilesBundle;
import com.mohanqing.freqfiles.service.FreqFilesService;
import com.mohanqing.freqfiles.state.FavoriteFilesState;
import com.mohanqing.freqfiles.state.FrequentFilesState;
import com.mohanqing.freqfiles.state.FreqFilesSettingsState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮面板 - 可拖动的快速文件选择面板
 */
public class FreqFilesFloatingPanel {
    public static final Key<FreqFilesFloatingPanel> KEY = Key.create("FreqFilesFloatingPanel");
    
    private JPanel panel;
    private JBList<String> favoriteList;
    private JBList<String> frequentList;
    private Project project;
    private JBPopup popup;
    private JLabel hintLabel; // 保存提示标签的引用，以便动态更新
    private javax.swing.Timer hintUpdateTimer; // 定时器，用于定期更新提示文本
    
    public FreqFilesFloatingPanel(@NotNull Project project) {
        this.project = project;
        createPanel();
    }
    
    private void createPanel() {
        panel = new JPanel(new BorderLayout());
        // 调整默认大小（根据图片中的实际显示）
        panel.setPreferredSize(new Dimension(600, 270));
        // 移除 panel 的所有边距
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // 创建左右分栏面板 - 使用自定义布局实现固定均分
        JPanel leftPanel = createFavoritePanel();
        leftPanel.setBorder(BorderFactory.createTitledBorder(FreqFilesBundle.message("freqfiles.favorite.files")));
        
        JPanel rightPanel = createFrequentPanel();
        rightPanel.setBorder(BorderFactory.createTitledBorder(FreqFilesBundle.message("freqfiles.frequent.files")));
        
        // 使用 JSplitPane 实现可调节的分隔，默认右边占更大比例（30% 左边，70% 右边）
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(0.3); // 设置分隔线位置为30%（左边30%，右边70%）
        splitPane.setDividerSize(3); // 设置分隔线宽度为3像素（足够拖动）
        splitPane.setResizeWeight(0.3); // 设置调整权重，保持左边30%的比例
        splitPane.setBorder(BorderFactory.createEmptyBorder()); // 移除边框
        splitPane.setContinuousLayout(true); // 启用连续布局，拖动时实时更新
        splitPane.setOneTouchExpandable(false); // 禁用一键展开按钮
        
        // 设置分隔线为完全透明
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        // 不绘制任何内容，保持完全透明
                        // 但分隔区域仍然可以拖动
                    }
                };
            }
        });
        
        JPanel container = new JPanel(new BorderLayout());
        container.add(splitPane, BorderLayout.CENTER);
        
        panel.add(container, BorderLayout.CENTER);
        
        // 添加底部栏：提示和设置按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // 左侧提示
        hintLabel = new JLabel();
        updateHintLabel(); // 初始化提示文本
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, hintLabel.getFont().getSize() - 1));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        hintLabel.setHorizontalAlignment(SwingConstants.LEFT);
        bottomPanel.add(hintLabel, BorderLayout.WEST);
        
        // 右侧设置按钮（使用图标，放在最右边）
        JButton settingsButton = new JButton(AllIcons.General.Settings);
        settingsButton.setToolTipText(FreqFilesBundle.message("freqfiles.settings"));
        settingsButton.setBorderPainted(false);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setMargin(new Insets(0, 0, 0, 0));
        // 设置按钮的精确大小，只保留必要的内边距
        settingsButton.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        settingsButton.setPreferredSize(new Dimension(settingsButton.getIcon().getIconWidth() + 10, 
                                                      settingsButton.getIcon().getIconHeight() + 6));
        settingsButton.setMaximumSize(settingsButton.getPreferredSize());
        settingsButton.setMinimumSize(settingsButton.getPreferredSize());
        settingsButton.addActionListener(e -> openSettings());
        // 使用容器包装，确保点击区域精确
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonContainer.setOpaque(false);
        buttonContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        buttonContainer.add(settingsButton);
        bottomPanel.add(buttonContainer, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        // 刷新列表
        refreshLists();
        
        // 启动定时器，定期更新提示文本（每5秒更新一次）
        startHintUpdateTimer();
    }
    
    /**
     * 启动提示更新定时器
     */
    private void startHintUpdateTimer() {
        stopHintUpdateTimer(); // 先停止之前的定时器
        
        hintUpdateTimer = new javax.swing.Timer(5000, e -> {
            if (isVisible() && hintLabel != null) {
                updateHintLabel();
            }
        });
        hintUpdateTimer.setRepeats(true);
        hintUpdateTimer.start();
    }
    
    /**
     * 停止提示更新定时器
     */
    private void stopHintUpdateTimer() {
        if (hintUpdateTimer != null) {
            hintUpdateTimer.stop();
            hintUpdateTimer = null;
        }
    }
    
    /**
     * 更新提示标签的文本
     */
    private void updateHintLabel() {
        if (hintLabel != null) {
            String toggleShortcut = getExitShortcutText();
            hintLabel.setText(FreqFilesBundle.message("freqfiles.hint.exit", toggleShortcut));
        }
    }
    
    /**
     * 获取切换悬浮面板的快捷键文本
     * 动态获取 ShowFloatingPanelAction 的快捷键
     */
    private String getExitShortcutText() {
        try {
            // 获取 ActionManager
            ActionManager actionManager = ActionManager.getInstance();
            if (actionManager == null) {
                return "Shift+Alt+F";
            }
            
            // 获取 KeymapManager
            KeymapManager keymapManager = KeymapManager.getInstance();
            if (keymapManager == null) {
                return "Shift+Alt+F";
            }
            
            // 获取当前活动的键盘映射
            Keymap keymap = keymapManager.getActiveKeymap();
            if (keymap == null) {
                return "Shift+Alt+F";
            }
            
            // 获取 Action 的快捷键
            com.intellij.openapi.actionSystem.Shortcut[] shortcuts = keymap.getShortcuts("FreqFiles.ShowFloatingPanel");
            if (shortcuts != null && shortcuts.length > 0) {
                // 使用 KeymapUtil 格式化快捷键文本
                String shortcutText = KeymapUtil.getShortcutsText(shortcuts);
                if (shortcutText != null && !shortcutText.isEmpty()) {
                    return shortcutText;
                }
            }
        } catch (Exception e) {
            // 如果获取失败，返回默认值
        }
        
        // 默认返回 Shift+Alt+F
        return "Shift+Alt+F";
    }
    
    /**
     * 格式化快捷键文本
     */
    private String formatShortcut(javax.swing.KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "Shift+Alt+F";
        }
        
        StringBuilder sb = new StringBuilder();
        
        int modifiers = keyStroke.getModifiers();
        if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
            sb.append("Meta+");
        }
        
        // 处理特殊按键
        int keyCode = keyStroke.getKeyCode();
        if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
            sb.append("ESC");
        } else if (keyCode >= java.awt.event.KeyEvent.VK_F1 && keyCode <= java.awt.event.KeyEvent.VK_F12) {
            sb.append("F").append(keyCode - java.awt.event.KeyEvent.VK_F1 + 1);
        } else {
            // 使用按键名称
            String keyName = java.awt.event.KeyEvent.getKeyText(keyCode);
            sb.append(keyName);
        }
        
        return sb.toString();
    }
    
    private JPanel createFavoritePanel() {
        favoriteList = new JBList<>();
        favoriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoriteList.setCellRenderer(new FileListCellRenderer());
        new ListSpeedSearch<>(favoriteList);
        
        // 设置工具提示显示相对路径
        setListTooltip(favoriteList);
        
        // 双击打开文件（保持悬浮窗常驻）
        favoriteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedFile(favoriteList);
                    // 移除关闭逻辑，保持悬浮窗常驻
                }
            }
        });
        
        // 使用ToolbarDecorator添加工具栏按钮
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(favoriteList)
                .setAddAction(button -> addFavoriteFile())
                .setRemoveAction(button -> removeFavoriteFile())
                .setMoveUpAction(button -> moveFavoriteFileUp())
                .setMoveDownAction(button -> moveFavoriteFileDown())
                .addExtraAction(new AnAction("Clear All", "Clear all favorite files", AllIcons.General.Delete) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        clearAllFavoriteFiles();
                    }
                });
        
        JPanel panel = decorator.createPanel();
        // 调整滚动条宽度
        adjustScrollBarWidth(panel);
        return panel;
    }
    
    private JPanel createFrequentPanel() {
        frequentList = new JBList<>();
        frequentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frequentList.setCellRenderer(new FrequentFileListCellRenderer());
        new ListSpeedSearch<>(frequentList);
        
        // 设置工具提示显示相对路径
        setListTooltip(frequentList);
        
        // 双击打开文件（保持悬浮窗常驻）
        frequentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedFile(frequentList);
                    // 移除关闭逻辑，保持悬浮窗常驻
                }
            }
        });
        
        // 使用ToolbarDecorator添加删除按钮
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(frequentList)
                .disableAddAction()
                .setRemoveAction(button -> removeFrequentFile())
                .disableUpAction()
                .disableDownAction()
                .addExtraAction(new AnAction("Clear All", "Clear all frequent files statistics", AllIcons.General.Delete) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        clearAllFrequentFiles();
                    }
                });
        
        JPanel panel = decorator.createPanel();
        // 调整滚动条宽度
        adjustScrollBarWidth(panel);
        return panel;
    }
    
    /**
     * 调整滚动条宽度，使滚动条更细
     */
    private void adjustScrollBarWidth(JPanel panel) {
        // 递归查找 JScrollPane
        findAndAdjustScrollPane(panel);
    }
    
    private void findAndAdjustScrollPane(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                // 设置垂直滚动条的首选宽度（更细，4像素）
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                if (verticalScrollBar != null) {
                    verticalScrollBar.setPreferredSize(new Dimension(4, verticalScrollBar.getPreferredSize().height));
                    verticalScrollBar.setMinimumSize(new Dimension(4, verticalScrollBar.getMinimumSize().height));
                    verticalScrollBar.setMaximumSize(new Dimension(4, verticalScrollBar.getMaximumSize().height));
                }
                // 设置水平滚动条的首选高度（更细，4像素）
                JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                if (horizontalScrollBar != null) {
                    horizontalScrollBar.setPreferredSize(new Dimension(horizontalScrollBar.getPreferredSize().width, 4));
                    horizontalScrollBar.setMinimumSize(new Dimension(horizontalScrollBar.getMinimumSize().width, 4));
                    horizontalScrollBar.setMaximumSize(new Dimension(horizontalScrollBar.getMaximumSize().width, 4));
                }
            } else if (comp instanceof Container) {
                findAndAdjustScrollPane((Container) comp);
            }
        }
    }
    
    private void openSelectedFile(JBList<String> list) {
        String selected = list.getSelectedValue();
        if (selected != null) {
            VirtualFile file = null;
            
            // 尝试作为相对路径处理
            String basePath = project.getBasePath();
            if (basePath != null) {
                try {
                    Path base = Paths.get(basePath);
                    Path filePath = base.resolve(selected);
                    file = VirtualFileManager.getInstance().findFileByNioPath(filePath);
                } catch (Exception e) {
                    // 忽略异常，继续尝试其他方式
                }
            }
            
            // 如果相对路径找不到，尝试作为绝对路径
            if (file == null) {
                file = VirtualFileManager.getInstance().findFileByNioPath(new File(selected).toPath());
            }
            
            // 如果还是找不到，尝试通过URL查找
            if (file == null) {
                String url = VirtualFileManager.constructUrl("file", selected);
                file = VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
            }
            
            if (file != null && file.exists()) {
                OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                // 注意：文件打开时会触发 FileEditorListener.fileOpened()，在那里会自动从排除列表移除
            }
        }
    }
    
    private void addFavoriteFile() {
        // 使用文件选择器选择文件（允许选择项目外的文件）
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        descriptor.setTitle(FreqFilesBundle.message("freqfiles.file.chooser.title"));
        descriptor.setDescription(FreqFilesBundle.message("freqfiles.file.chooser.description"));
        
        // 获取项目根目录（如果存在），否则使用用户主目录
        VirtualFile baseDir = null;
        String basePath = project.getBasePath();
        if (basePath != null) {
            baseDir = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(basePath));
        }
        // 如果项目根目录不存在，使用用户主目录
        if (baseDir == null) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                baseDir = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(userHome));
            }
        }
        
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(descriptor, project, baseDir);
        if (selectedFiles.length == 0) {
            return;
        }
        
        VirtualFile selectedFile = selectedFiles[0];
        if (selectedFile.isDirectory()) {
            JOptionPane.showMessageDialog(panel, FreqFilesBundle.message("freqfiles.error.only.files"), 
                    FreqFilesBundle.message("freqfiles.settings"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 获取应该存储的路径（项目内用相对路径，项目外用绝对路径）
        String pathToStore = getPathToStore(selectedFile.getPath());
        if (pathToStore == null) {
            // 如果无法获取路径，使用绝对路径
            pathToStore = selectedFile.getPath();
        }
        
        FreqFilesService service = FreqFilesService.getInstance(project);
        FavoriteFilesState favoriteState = service.getFavoriteState();
        
        // 检查是否已经是常用文件（同时检查相对路径和绝对路径）
        if (favoriteState.favoriteFiles.contains(pathToStore) || 
            favoriteState.favoriteFiles.contains(selectedFile.getPath())) {
            JOptionPane.showMessageDialog(panel, FreqFilesBundle.message("freqfiles.error.already.in.list"), 
                    FreqFilesBundle.message("freqfiles.settings"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        favoriteState.addFavoriteFile(pathToStore);
        refreshLists();
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
    
    private void removeFavoriteFile() {
        String selected = favoriteList.getSelectedValue();
        if (selected != null) {
            FreqFilesService service = FreqFilesService.getInstance(project);
            FavoriteFilesState favoriteState = service.getFavoriteState();
            favoriteState.removeFavoriteFile(selected);
            refreshLists();
        }
    }
    
    /**
     * 显示带英文按钮的是/否确认对话框
     */
    private int showYesNoDialog(Component parentComponent, String message, String title) {
        // 临时保存原始的按钮文本
        Object yesText = UIManager.get("OptionPane.yesButtonText");
        Object noText = UIManager.get("OptionPane.noButtonText");
        
        try {
            // 设置英文按钮文本
            UIManager.put("OptionPane.yesButtonText", "Yes");
            UIManager.put("OptionPane.noButtonText", "No");
            
            // 显示对话框
            return JOptionPane.showConfirmDialog(
                parentComponent,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
        } finally {
            // 恢复原始的按钮文本
            if (yesText != null) {
                UIManager.put("OptionPane.yesButtonText", yesText);
            }
            if (noText != null) {
                UIManager.put("OptionPane.noButtonText", noText);
            }
        }
    }
    
    /**
     * 删除所有常用文件
     */
    private void clearAllFavoriteFiles() {
        FreqFilesService service = FreqFilesService.getInstance(project);
        FavoriteFilesState favoriteState = service.getFavoriteState();
        
        if (favoriteState.favoriteFiles.isEmpty()) {
            return;
        }
        
        // 显示确认对话框（使用英文按钮）
        int result = showYesNoDialog(
            panel,
            "Are you sure you want to clear all favorite files?",
            "Clear All Favorite Files"
        );
        
        if (result == JOptionPane.YES_OPTION) {
            favoriteState.clearAll();
            refreshLists();
        }
    }
    
    private void removeFrequentFile() {
        String selected = frequentList.getSelectedValue();
        if (selected != null) {
            FreqFilesService service = FreqFilesService.getInstance(project);
            FrequentFilesState frequentState = service.getFrequentState();
            // 清除该文件的所有统计信息（点击次数、停留时间、最后访问时间）
            // 同时从排除列表中移除（如果存在），这样文件重新打开时可以从零开始统计
            frequentState.removeFileFrequency(selected);
            frequentState.includeInFrequent(selected); // 确保从排除列表中移除
            refreshLists();
        }
    }
    
    /**
     * 删除所有高频文件统计
     */
    private void clearAllFrequentFiles() {
        FreqFilesService service = FreqFilesService.getInstance(project);
        FrequentFilesState frequentState = service.getFrequentState();
        
        if (frequentState.fileFrequency.isEmpty() && frequentState.excludedFromFrequent.isEmpty()) {
            return;
        }
        
        // 显示确认对话框（使用英文按钮）
        int result = showYesNoDialog(
            panel,
            "Are you sure you want to clear all frequent files statistics?",
            "Clear All Frequent Files"
        );
        
        if (result == JOptionPane.YES_OPTION) {
            frequentState.clearAll();
            refreshLists();
        }
    }
    
    private void moveFavoriteFileUp() {
        int selectedIndex = favoriteList.getSelectedIndex();
        if (selectedIndex > 0) {
            FreqFilesService service = FreqFilesService.getInstance(project);
            FavoriteFilesState favoriteState = service.getFavoriteState();
            List<String> favoriteFiles = new ArrayList<>(favoriteState.favoriteFiles);
            String temp = favoriteFiles.get(selectedIndex);
            favoriteFiles.set(selectedIndex, favoriteFiles.get(selectedIndex - 1));
            favoriteFiles.set(selectedIndex - 1, temp);
            favoriteState.favoriteFiles.clear();
            favoriteState.favoriteFiles.addAll(favoriteFiles);
            refreshLists();
            favoriteList.setSelectedIndex(selectedIndex - 1);
        }
    }
    
    private void moveFavoriteFileDown() {
        int selectedIndex = favoriteList.getSelectedIndex();
        FreqFilesService service = FreqFilesService.getInstance(project);
        FavoriteFilesState favoriteState = service.getFavoriteState();
        List<String> favoriteFiles = new ArrayList<>(favoriteState.favoriteFiles);
        if (selectedIndex >= 0 && selectedIndex < favoriteFiles.size() - 1) {
            String temp = favoriteFiles.get(selectedIndex);
            favoriteFiles.set(selectedIndex, favoriteFiles.get(selectedIndex + 1));
            favoriteFiles.set(selectedIndex + 1, temp);
            favoriteState.favoriteFiles.clear();
            favoriteState.favoriteFiles.addAll(favoriteFiles);
            refreshLists();
            favoriteList.setSelectedIndex(selectedIndex + 1);
        }
    }
    
    public void refreshLists() {
        // 更新提示文本
        updateHintLabel();
        if (project == null || project.isDisposed()) {
            return;
        }
        
        // 立即刷新列表，确保实时更新
        // 使用 ApplicationManager.invokeLater 确保在正确的线程上执行
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            
            try {
                FreqFilesService service = FreqFilesService.getInstance(project);
                FavoriteFilesState favoriteState = service.getFavoriteState();
                FrequentFilesState frequentState = service.getFrequentState();
                
                // 刷新常用文件列表
                if (favoriteList != null) {
                    List<String> favoriteFiles = new ArrayList<>(favoriteState.favoriteFiles);
                    favoriteList.setListData(favoriteFiles.toArray(new String[0]));
                }
                
                // 刷新高频文件列表（显示前50个），每次都会重新排序
                if (frequentList != null) {
                    // 从设置中获取权重
                    FreqFilesSettingsState settings = service.getSettingsState();
                    double freqWeight = settings.getFrequencyWeightRatio();
                    double stayWeight = settings.getStayTimeWeightRatio();
                    
                    List<String> frequentFiles = frequentState.getTopFrequentFiles(
                        50, favoriteState.favoriteFiles, freqWeight, stayWeight);
                    frequentList.setListData(frequentFiles.toArray(new String[0]));
                }
                
                // 强制刷新整个面板
                if (panel != null) {
                    panel.revalidate();
                    panel.repaint();
                }
            } catch (Exception e) {
                // 忽略异常，避免影响其他功能
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 显示悬浮面板
     */
    public void show(Component component) {
        // 如果已经显示，先关闭
        if (popup != null && popup.isVisible()) {
            popup.closeOk(null);
            popup = null;
        }
        
        refreshLists();
        
        // 创建一个包装面板，移除所有内边距
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        wrapperPanel.add(panel, BorderLayout.CENTER);
        
        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(wrapperPanel, null)
                .setTitle("FreqFiles: Quick Select File")
                .setResizable(true)
                .setMovable(true)
                .setFocusable(true)
                .setRequestFocus(false) // 不强制获取焦点，避免影响编辑器
                .setCancelKeyEnabled(true)
                .setCancelOnClickOutside(false) // 不允许点击外部关闭，保持常驻
                .setCancelOnWindowDeactivation(false)
                .createPopup();
        
        // 在显示后设置popup的初始大小
        ApplicationManager.getApplication().invokeLater(() -> {
            if (popup != null && popup.isVisible()) {
                Component content = popup.getContent();
                if (content != null) {
                    Window window = SwingUtilities.getWindowAncestor(content);
                    if (window != null) {
                        window.setSize(new Dimension(600, 270));
                        window.validate();
                    }
                }
            }
        });
        
        if (component != null) {
            popup.showInCenterOf(component);
        } else {
            // 如果没有指定组件，显示在IDE窗口中心
            WindowManager windowManager = WindowManager.getInstance();
            if (windowManager != null && project != null) {
                Window ideFrame = windowManager.getFrame(project);
                if (ideFrame != null) {
                    popup.showInCenterOf(ideFrame);
                }
            }
        }
    }
    
    public void hide() {
        // 停止定时器
        stopHintUpdateTimer();
        
        if (popup != null && popup.isVisible()) {
            popup.closeOk(null);
        }
    }
    
    /**
     * 检查悬浮窗是否可见
     */
    public boolean isVisible() {
        return popup != null && popup.isVisible();
    }
    
    /**
     * 打开设置界面
     */
    private void openSettings() {
        if (project != null && !project.isDisposed()) {
            // 直接创建并显示设置对话框
            FreqFilesSettingsConfigurable configurable = new FreqFilesSettingsConfigurable(project);
            com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                .editConfigurable(project, configurable);
            // 设置应用后，刷新列表
            refreshLists();
        }
    }
    
    /**
     * 设置列表的工具提示，显示文件的相对路径
     */
    private void setListTooltip(JBList<String> list) {
        list.setToolTipText(null); // 先清除默认tooltip
        
        list.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && index < list.getModel().getSize()) {
                    String filePath = list.getModel().getElementAt(index);
                    if (filePath != null) {
                        String displayPath = getDisplayPath(filePath);
                        list.setToolTipText(displayPath);
                    }
                } else {
                    list.setToolTipText(null);
                }
            }
        });
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
            // 如果文件不在项目内，返回null
            return null;
        } catch (Exception e) {
            // 如果出错，返回null
            return null;
        }
    }
    
    /**
     * 获取文件的显示路径（用于tooltip）
     * 如果文件在项目内，返回相对路径；否则返回绝对路径
     */
    private String getDisplayPath(String filePath) {
        String relativePath = getRelativePath(filePath);
        if (relativePath != null) {
            return relativePath;
        } else {
            return filePath;
        }
    }

    /**
     * 自定义列表渲染器，只显示文件名（用于常用文件列表）
     */
    private static class FileListCellRenderer extends ColoredListCellRenderer<String> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends String> list, String value, int index,
                                            boolean selected, boolean hasFocus) {
            if (value == null) {
                return;
            }
            
            File file = new File(value);
            append(file.getName());
        }
    }
    
    /**
     * 高频文件列表渲染器，显示文件名、点击次数和停留时间
     */
    private class FrequentFileListCellRenderer extends ColoredListCellRenderer<String> {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends String> list, String value, int index,
                                            boolean selected, boolean hasFocus) {
            if (value == null) {
                return;
            }
            
            File file = new File(value);
            append(file.getName());
            
            // 获取点击次数和停留时间
            FreqFilesService service = FreqFilesService.getInstance(project);
            FrequentFilesState frequentState = service.getFrequentState();
            
            int frequency = frequentState.fileFrequency.getOrDefault(value, 0);
            long stayTimeMs = frequentState.fileTotalStayTime.getOrDefault(value, 0L);
            
            // 格式化停留时间
            String stayTimeText = formatStayTime(stayTimeMs);
            
            // 只显示统计信息部分（文件名已经在上面的 append 中添加了）
            String statsText = " | Clicks: " + frequency + " | Stay: " + stayTimeText;
            append(statsText, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        
        /**
         * 格式化停留时间（英文格式）
         */
        private String formatStayTime(long stayTimeMs) {
            if (stayTimeMs < 1000) {
                return stayTimeMs + "ms";
            } else if (stayTimeMs < 60 * 1000) {
                return (stayTimeMs / 1000) + "s";
            } else if (stayTimeMs < 60 * 60 * 1000) {
                long minutes = stayTimeMs / (60 * 1000);
                long seconds = (stayTimeMs % (60 * 1000)) / 1000;
                return minutes + "m " + seconds + "s";
            } else {
                long hours = stayTimeMs / (60 * 60 * 1000);
                long minutes = (stayTimeMs % (60 * 60 * 1000)) / (60 * 1000);
                return hours + "h " + minutes + "m";
            }
        }
    }
}
