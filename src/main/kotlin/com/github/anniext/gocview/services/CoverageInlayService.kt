package com.github.anniext.gocview.services

import com.github.anniext.gocview.model.CoverageBlock
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.JLabel

/**
 * 覆盖率内嵌提示服务
 * 
 * 在代码行末显示执行次数
 */
@Service(Service.Level.PROJECT)
class CoverageInlayService(private val project: Project) {
    
    private val logger = thisLogger()
    
    // 存储每个编辑器的内嵌提示
    private val editorInlays = mutableMapOf<Editor, MutableList<Inlay<*>>>()
    
    companion object {
        fun getInstance(project: Project): CoverageInlayService {
            return project.getService(CoverageInlayService::class.java)
        }
    }
    
    /**
     * 为编辑器添加覆盖率内嵌提示
     */
    fun addCoverageInlays(editor: Editor, filePath: String, blocks: List<CoverageBlock>) {
        // 清除旧的内嵌提示
        clearInlays(editor)
        
        val document = editor.document
        val inlayModel = editor.inlayModel
        val inlays = mutableListOf<Inlay<*>>()
        
        // 按行分组覆盖率块
        val blocksByLine = blocks.groupBy { it.endLine }
        
        blocksByLine.forEach { (lineNumber, lineBlocks) ->
            try {
                val line = (lineNumber - 1).coerceAtLeast(0)
                
                if (line >= document.lineCount) {
                    logger.warn("Line number out of range: $line, document has ${document.lineCount} lines")
                    return@forEach
                }
                
                // 计算该行的总执行次数
                val totalExecutions = lineBlocks.sumOf { it.executionCount }
                val isCovered = totalExecutions > 0
                
                // 在行末添加内嵌提示
                val lineEndOffset = document.getLineEndOffset(line)
                
                val renderer = CoverageInlayRenderer(totalExecutions, isCovered)
                val inlay = inlayModel.addInlineElement(lineEndOffset, true, renderer)
                
                if (inlay != null) {
                    inlays.add(inlay)
                }
                
            } catch (e: Exception) {
                logger.error("Failed to add inlay for line: $lineNumber", e)
            }
        }
        
        editorInlays[editor] = inlays
        logger.info("Added ${inlays.size} inlays to editor for file: $filePath")
    }
    
    /**
     * 清除编辑器的内嵌提示
     */
    fun clearInlays(editor: Editor) {
        editorInlays[editor]?.forEach { inlay ->
            inlay.dispose()
        }
        editorInlays.remove(editor)
    }
    
    /**
     * 清除所有编辑器的内嵌提示
     */
    fun clearAllInlays() {
        editorInlays.keys.toList().forEach { editor ->
            clearInlays(editor)
        }
    }
    
    /**
     * 覆盖率内嵌提示渲染器
     */
    private class CoverageInlayRenderer(
        private val executionCount: Int,
        private val isCovered: Boolean
    ) : com.intellij.openapi.editor.EditorCustomElementRenderer {
        
        private val text = " ✓ $executionCount"
        private val label = JLabel(text)
        
        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            val metrics = label.getFontMetrics(label.font)
            return metrics.stringWidth(text) + 10
        }
        
        override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: com.intellij.openapi.editor.markup.TextAttributes) {
            val editor = inlay.editor
            val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
            
            g.font = font.deriveFont(Font.ITALIC, font.size * 0.9f)
            
            // 设置颜色
            g.color = if (isCovered) {
                JBColor(0x4CAF50, 0x81C784) // 绿色
            } else {
                JBColor(0xF44336, 0xE57373) // 红色
            }
            
            // 绘制文本
            val metrics = g.fontMetrics
            val x = targetRegion.x + 5
            val y = targetRegion.y + metrics.ascent
            
            g.drawString(text, x, y)
        }
    }
}
