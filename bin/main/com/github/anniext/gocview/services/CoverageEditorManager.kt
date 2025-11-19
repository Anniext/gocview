package com.github.anniext.gocview.services

import com.github.anniext.gocview.model.CoverageBlock
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 覆盖率编辑器管理器
 * 
 * 管理编辑器的覆盖率显示，监听文件打开事件
 */
@Service(Service.Level.PROJECT)
class CoverageEditorManager(private val project: Project) {
    
    private val logger = thisLogger()
    private val highlightService = CoverageHighlightService.getInstance(project)
    private val inlayService = CoverageInlayService.getInstance(project)
    private val pathResolver = GoModulePathResolver.getInstance(project)
    
    // 存储每个文件的覆盖率数据（key 是模块路径）
    private val fileCoverageData = mutableMapOf<String, List<CoverageBlock>>()
    
    companion object {
        fun getInstance(project: Project): CoverageEditorManager {
            return project.getService(CoverageEditorManager::class.java)
        }
    }
    
    init {
        // 监听文件编辑器事件
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    applyCoverageToFile(file)
                }
                
                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    // 文件关闭时清理资源
                    val editor = getEditorForFile(file)
                    if (editor != null) {
                        highlightService.clearHighlights(editor)
                        inlayService.clearInlays(editor)
                    }
                }
            }
        )
    }
    
    /**
     * 更新覆盖率数据
     */
    fun updateCoverageData(coverageMap: Map<String, List<CoverageBlock>>) {
        fileCoverageData.clear()
        fileCoverageData.putAll(coverageMap)
        
        // 刷新所有打开的编辑器
        ApplicationManager.getApplication().invokeLater {
            refreshAllEditors()
        }
    }
    
    /**
     * 清除所有覆盖率显示
     */
    fun clearAllCoverage() {
        fileCoverageData.clear()
        highlightService.clearAllHighlights()
        inlayService.clearAllInlays()
    }
    
    /**
     * 刷新所有打开的编辑器
     */
    private fun refreshAllEditors() {
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openFiles.forEach { file ->
            applyCoverageToFile(file)
        }
    }
    
    /**
     * 为指定文件应用覆盖率显示
     */
    private fun applyCoverageToFile(file: VirtualFile) {
        val editor = getEditorForFile(file) ?: return
        val filePath = file.path
        
        // 查找匹配的覆盖率数据
        val blocks = findCoverageBlocks(filePath)
        
        if (blocks.isNotEmpty()) {
            logger.info("Applying coverage to file: $filePath (${blocks.size} blocks)")
            highlightService.applyCoverageHighlight(editor, filePath, blocks)
            inlayService.addCoverageInlays(editor, filePath, blocks)
        } else {
            // 清除高亮
            highlightService.clearHighlights(editor)
            inlayService.clearInlays(editor)
        }
    }
    
    /**
     * 查找文件的覆盖率数据
     * 支持模块路径匹配
     */
    private fun findCoverageBlocks(filePath: String): List<CoverageBlock> {
        // 1. 精确匹配文件路径
        fileCoverageData[filePath]?.let { return it }
        
        // 2. 模糊匹配：查找以文件路径结尾的键
        val fileName = filePath.substringAfterLast('/')
        fileCoverageData.entries.forEach { (modulePath, blocks) ->
            if (modulePath.endsWith(filePath) || modulePath.endsWith(fileName)) {
                return blocks
            }
        }
        
        // 3. 反向匹配：检查当前文件是否匹配某个模块路径
        fileCoverageData.entries.forEach { (modulePath, blocks) ->
            val resolvedFile = pathResolver.resolveModulePath(modulePath)
            if (resolvedFile != null && resolvedFile.path == filePath) {
                return blocks
            }
        }
        
        return emptyList()
    }
    
    /**
     * 获取文件对应的编辑器
     */
    private fun getEditorForFile(file: VirtualFile): Editor? {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val fileEditor = fileEditorManager.getSelectedEditor(file)
        
        return if (fileEditor is TextEditor) {
            fileEditor.editor
        } else {
            null
        }
    }
}
