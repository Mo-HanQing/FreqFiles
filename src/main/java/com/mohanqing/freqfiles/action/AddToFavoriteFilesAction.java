package com.mohanqing.freqfiles.action;

import com.intellij.icons.AllIcons;
import com.mohanqing.freqfiles.util.FreqFilesBundle;
import com.mohanqing.freqfiles.manager.FreqFilesManager;
import com.mohanqing.freqfiles.ui.FreqFilesFloatingPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 右键菜单Action：添加到常用文件
 */
public class AddToFavoriteFilesAction extends AnAction {
    
    private static final Logger LOG = Logger.getInstance(AddToFavoriteFilesAction.class);
    
    public AddToFavoriteFilesAction() {
        super(FreqFilesBundle.message("freqfiles.add.to.favorite"), 
              FreqFilesBundle.message("freqfiles.add.to.favorite.description"), 
              AllIcons.Nodes.Favorite);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // 使用与 update() 相同的逻辑获取文件
        VirtualFile file = getFileFromEvent(e);
        
        if (project == null || file == null) {
            Messages.showInfoMessage(project, FreqFilesBundle.message("freqfiles.error.cannot.get.file"), 
                    FreqFilesBundle.message("freqfiles.settings"));
            return;
        }
        
        if (file.isDirectory()) {
            Messages.showInfoMessage(project, FreqFilesBundle.message("freqfiles.error.only.files"), 
                    FreqFilesBundle.message("freqfiles.settings"));
            return;
        }
        
        FreqFilesManager manager = FreqFilesManager.getInstance(project);
        
        // 检查是否已经是常用文件
        if (manager.isFavoriteFile(file)) {
            Messages.showInfoMessage(project, FreqFilesBundle.message("freqfiles.error.already.in.list"), 
                    FreqFilesBundle.message("freqfiles.settings"));
            return;
        }
        
        // 添加到常用文件
        manager.addFavoriteFile(file);
        
        // 刷新悬浮窗
        FreqFilesFloatingPanel floatingPanel = project.getUserData(FreqFilesFloatingPanel.KEY);
        if (floatingPanel != null) {
            floatingPanel.refreshLists();
        }
    }
    
    /**
     * 从 AnActionEvent 中获取 VirtualFile，尝试多种方式
     */
    private VirtualFile getFileFromEvent(@NotNull AnActionEvent e) {
        VirtualFile file = null;
        
        // 1. 直接从 VIRTUAL_FILE 获取
        file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) return file;
        
        // 2. 从选中的文件数组获取
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles != null && selectedFiles.length > 0) {
            return selectedFiles[0];
        }
        
        // 3. 从 PlatformDataKeys 获取
        file = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.VIRTUAL_FILE);
        if (file != null) return file;
        
        // 4. 从 PlatformDataKeys.SELECTED_ITEMS 获取（项目视图右键菜单常用）
        Object[] selectedItems = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.SELECTED_ITEMS);
        if (selectedItems != null && selectedItems.length > 0) {
            Object selectedItem = selectedItems[0];
            try {
                // 尝试通过反射获取 getValue() 方法
                java.lang.reflect.Method getValueMethod = selectedItem.getClass().getMethod("getValue");
                Object value = getValueMethod.invoke(selectedItem);
                if (value instanceof com.intellij.psi.PsiElement) {
                    com.intellij.psi.PsiElement psiElement = (com.intellij.psi.PsiElement) value;
                    if (psiElement instanceof com.intellij.psi.PsiFile) {
                        return ((com.intellij.psi.PsiFile) psiElement).getVirtualFile();
                    } else {
                        com.intellij.psi.PsiFile containingFile = psiElement.getContainingFile();
                        if (containingFile != null) {
                            return containingFile.getVirtualFile();
                        }
                    }
                } else if (value instanceof VirtualFile) {
                    return (VirtualFile) value;
                }
            } catch (Exception ex) {
                LOG.warn("无法从 SELECTED_ITEMS 获取文件: " + ex.getMessage());
            }
        }
        
        // 5. 从 PSI 文件获取
        com.intellij.psi.PsiElement psiElement = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT);
        if (psiElement instanceof com.intellij.psi.PsiFile) {
            return ((com.intellij.psi.PsiFile) psiElement).getVirtualFile();
        } else if (psiElement != null) {
            com.intellij.psi.PsiFile containingFile = psiElement.getContainingFile();
            if (containingFile != null) {
                return containingFile.getVirtualFile();
            }
        }
        
        return null;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // 使用与 actionPerformed() 相同的逻辑获取文件
        VirtualFile file = getFileFromEvent(e);
        
        // 记录调试信息
        if (file == null) {
            LOG.warn("无法获取文件 - project: " + (project != null ? project.getName() : "null") + 
                    ", VIRTUAL_FILE: " + (e.getData(CommonDataKeys.VIRTUAL_FILE) != null ? "not null" : "null") +
                    ", VIRTUAL_FILE_ARRAY: " + (e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) != null ? "not null" : "null") +
                    ", PSI_ELEMENT: " + (e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT) != null ? "not null" : "null") +
                    ", SELECTED_ITEMS: " + (e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.SELECTED_ITEMS) != null ? "not null" : "null"));
        } else {
            LOG.info("获取到文件: " + file.getPath() + ", isDirectory: " + file.isDirectory() + ", exists: " + file.exists());
        }
        
        // 只在有项目、有文件且文件不是目录时启用
        // 不在这里检查文件是否在项目内，因为 FreqFilesManager.addFavoriteFile 会自己检查
        // 如果文件不在项目内，addFavoriteFile 会静默返回，不会报错
        boolean enabled = project != null && file != null && !file.isDirectory();
        
        LOG.info("菜单项启用状态: " + enabled + " (project: " + (project != null) + 
                ", file: " + (file != null) + ", isDirectory: " + (file != null ? file.isDirectory() : "N/A") + ")");
        
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(true); // 始终可见
    }
}

