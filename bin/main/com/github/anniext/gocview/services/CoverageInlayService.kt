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
        
        companion object {
            // 已覆盖样式（绿色系）
            private val COVERED_BG_LIGHT = java.awt.Color(232, 245, 233, 180)      // 非常浅的绿色背景
            private val COVERED_BG_DARK = java.awt.Color(46, 125, 50, 100)         // 深绿色背景
            private val COVERED_BORDER_LIGHT = java.awt.Color(129, 199, 132, 200)  // 浅绿色边框
            private val COVERED_BORDER_DARK = java.awt.Color(102, 187, 106, 180)   // 深色主题绿色边框
            private val COVERED_TEXT_LIGHT = java.awt.Color(27, 94, 32)            // 深绿色文字
            private val COVERED_TEXT_DARK = java.awt.Color(165, 214, 167)          // 浅绿色文字
            
            // 未覆盖样式（红色系）
            private val UNCOVERED_BG_LIGHT = java.awt.Color(255, 235, 238, 180)    // 非常浅的红色背景
            private val UNCOVERED_BG_DARK = java.awt.Color(183, 28, 28, 100)       // 深红色背景
            private val UNCOVERED_BORDER_LIGHT = java.awt.Color(239, 154, 154, 200) // 浅红色边框
            private val UNCOVERED_BORDER_DARK = java.awt.Color(229, 115, 115, 180) // 深色主题红色边框
            private val UNCOVERED_TEXT_LIGHT = java.awt.Color(183, 28, 28)         // 深红色文字
            private val UNCOVERED_TEXT_DARK = java.awt.Color(239, 154, 154)        // 浅红色文字
            
            // 高频执行样式（琥珀色系，执行次数 > 100）
            private val HOT_BG_LIGHT = java.awt.Color(255, 243, 224, 180)          // 非常浅的橙色背景
            private val HOT_BG_DARK = java.awt.Color(230, 81, 0, 100)              // 深橙色背景
            private val HOT_BORDER_LIGHT = java.awt.Color(255, 183, 77, 200)       // 浅橙色边框
            private val HOT_BORDER_DARK = java.awt.Color(255, 167, 38, 180)        // 深色主题橙色边框
            private val HOT_TEXT_LIGHT = java.awt.Color(230, 81, 0)                // 深橙色文字
            private val HOT_TEXT_DARK = java.awt.Color(255, 183, 77)               // 浅橙色文字
        }
        
        private val text: String
        private val icon: String
        
        init {
            // 根据执行次数格式化文本和图标
            when {
                executionCount > 999999 -> {
                    text = "${executionCount / 1000000}M+"
                    icon = "🔥"
                }
                executionCount > 9999 -> {
                    text = "${executionCount / 1000}k+"
                    icon = "🔥"
                }
                executionCount > 999 -> {
                    text = "${executionCount / 1000}k+"
                    icon = "⚡"
                }
                executionCount > 100 -> {
                    text = "$executionCount"
                    icon = "⚡"
                }
                executionCount > 0 -> {
                    text = "$executionCount"
                    icon = "✓"
                }
                else -> {
                    text = "0"
                    icon = "✗"
                }
            }
        }
        
        private val displayText = " $icon $text "
        private val label = JLabel(displayText)
        
        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            val editor = inlay.editor
            val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
            val metrics = label.getFontMetrics(font.deriveFont(Font.BOLD, font.size * 0.85f))
            return metrics.stringWidth(displayText) + 14
        }
        
        override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: com.intellij.openapi.editor.markup.TextAttributes) {
            val g2d = g as java.awt.Graphics2D
            val editor = inlay.editor
            val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
            
            // 启用高质量渲染
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            )
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY
            )
            
            // 设置字体（稍小且加粗）
            g2d.font = font.deriveFont(Font.BOLD, font.size * 0.85f)
            
            // 根据覆盖状态和执行次数选择颜色
            val (bgColor, borderColor, textColor) = when {
                !isCovered -> Triple(
                    JBColor(UNCOVERED_BG_LIGHT, UNCOVERED_BG_DARK),
                    JBColor(UNCOVERED_BORDER_LIGHT, UNCOVERED_BORDER_DARK),
                    JBColor(UNCOVERED_TEXT_LIGHT, UNCOVERED_TEXT_DARK)
                )
                executionCount > 100 -> Triple(
                    JBColor(HOT_BG_LIGHT, HOT_BG_DARK),
                    JBColor(HOT_BORDER_LIGHT, HOT_BORDER_DARK),
                    JBColor(HOT_TEXT_LIGHT, HOT_TEXT_DARK)
                )
                else -> Triple(
                    JBColor(COVERED_BG_LIGHT, COVERED_BG_DARK),
                    JBColor(COVERED_BORDER_LIGHT, COVERED_BORDER_DARK),
                    JBColor(COVERED_TEXT_LIGHT, COVERED_TEXT_DARK)
                )
            }
            
            // 计算尺寸
            val metrics = g2d.fontMetrics
            val textWidth = metrics.stringWidth(displayText)
            val padding = 5
            val bgX = targetRegion.x + 4
            val bgY = targetRegion.y + 2
            val bgWidth = textWidth + padding * 2
            val bgHeight = targetRegion.height - 4
            val cornerRadius = 6
            
            // 绘制阴影效果（可选，增加立体感）
            g2d.color = java.awt.Color(0, 0, 0, 20)
            g2d.fillRoundRect(bgX + 1, bgY + 1, bgWidth, bgHeight, cornerRadius, cornerRadius)
            
            // 绘制圆角背景
            g2d.color = bgColor
            g2d.fillRoundRect(bgX, bgY, bgWidth, bgHeight, cornerRadius, cornerRadius)
            
            // 绘制边框
            g2d.color = borderColor
            g2d.drawRoundRect(bgX, bgY, bgWidth, bgHeight, cornerRadius, cornerRadius)
            
            // 绘制文本
            g2d.color = textColor
            val x = bgX + padding
            val y = targetRegion.y + metrics.ascent + 2
            
            g2d.drawString(displayText, x, y)
        }
    }
}
