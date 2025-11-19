package com.github.anniext.gocview.services

import com.github.anniext.gocview.model.CoverageBlock
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.Font

/**
 * 覆盖率高亮服务
 * 
 * 负责在编辑器中高亮显示覆盖率信息
 */
@Service(Service.Level.PROJECT)
class CoverageHighlightService(private val project: Project) {
    
    private val logger = thisLogger()
    
    // 存储每个编辑器的高亮器
    private val editorHighlighters = mutableMapOf<Editor, MutableList<RangeHighlighter>>()
    
    companion object {
        fun getInstance(project: Project): CoverageHighlightService {
            return project.getService(CoverageHighlightService::class.java)
        }
        
        // 已覆盖代码的背景色（更柔和的绿色渐变效果）
        private val COVERED_BACKGROUND = JBColor(
            java.awt.Color(220, 255, 225, 60),   // 浅色主题：非常浅的薄荷绿，更低透明度
            java.awt.Color(45, 100, 55, 50)      // 深色主题：深绿色，更低透明度
        )
        
        // 已覆盖代码的边框色（更鲜明的绿色）
        private val COVERED_BORDER = JBColor(
            java.awt.Color(76, 175, 80),         // 浅色主题：Material Design 绿色
            java.awt.Color(102, 187, 106)        // 深色主题：稍亮的绿色
        )
        
        // 未覆盖代码的背景色（柔和的红色）
        private val UNCOVERED_BACKGROUND = JBColor(
            java.awt.Color(255, 235, 238, 60),   // 浅色主题：非常浅的粉红色
            java.awt.Color(100, 45, 50, 50)      // 深色主题：深红色
        )
        
        // 未覆盖代码的边框色
        private val UNCOVERED_BORDER = JBColor(
            java.awt.Color(239, 83, 80),         // 浅色主题：Material Design 红色
            java.awt.Color(229, 115, 115)        // 深色主题：稍亮的红色
        )
        
        // 部分覆盖代码的背景色（柔和的黄色）
        private val PARTIAL_BACKGROUND = JBColor(
            java.awt.Color(255, 248, 225, 60),   // 浅色主题：非常浅的黄色
            java.awt.Color(100, 90, 45, 50)      // 深色主题：深黄色
        )
        
        // 部分覆盖代码的边框色
        private val PARTIAL_BORDER = JBColor(
            java.awt.Color(255, 193, 7),         // 浅色主题：Material Design 琥珀色
            java.awt.Color(255, 213, 79)         // 深色主题：稍亮的琥珀色
        )
        
        // 高亮层级（在选择层之下，但在语法高亮之上）
        private const val HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1
    }
    
    /**
     * 为编辑器应用覆盖率高亮
     */
    fun applyCoverageHighlight(editor: Editor, filePath: String, blocks: List<CoverageBlock>) {
        // 清除旧的高亮
        clearHighlights(editor)
        
        val document = editor.document
        val markupModel = editor.markupModel
        val highlighters = mutableListOf<RangeHighlighter>()
        
        // 高亮所有代码块（已覆盖和未覆盖）
        blocks.forEach { block ->
            try {
                // 计算起始和结束偏移量
                val startLine = (block.startLine - 1).coerceAtLeast(0)
                val endLine = (block.endLine - 1).coerceAtLeast(0)
                
                if (startLine >= document.lineCount || endLine >= document.lineCount) {
                    logger.warn("Line number out of range: $startLine-$endLine, document has ${document.lineCount} lines")
                    return@forEach
                }
                
                // 精确计算起始和结束偏移量（包含列信息）
                val lineStartOffset = document.getLineStartOffset(startLine)
                val lineEndOffset = document.getLineEndOffset(endLine)
                
                val startCol = (block.startCol - 1).coerceAtLeast(0)
                val endCol = (block.endCol - 1).coerceAtLeast(0)
                
                val startOffset = (lineStartOffset + startCol).coerceIn(0, document.textLength)
                val endOffset = (document.getLineStartOffset(endLine) + endCol).coerceIn(0, document.textLength)
                
                if (startOffset >= endOffset) {
                    logger.warn("Invalid offset range: $startOffset-$endOffset")
                    return@forEach
                }
                
                // 根据覆盖状态选择样式
                val (backgroundColor, borderColor) = when {
                    block.isCovered -> COVERED_BACKGROUND to COVERED_BORDER
                    block.executionCount > 0 -> PARTIAL_BACKGROUND to PARTIAL_BORDER
                    else -> UNCOVERED_BACKGROUND to UNCOVERED_BORDER
                }
                
                // 创建文本属性（带背景色和圆角边框）
                val textAttributes = TextAttributes().apply {
                    this.backgroundColor = backgroundColor
                    effectColor = borderColor
                    // 使用圆角边框效果，更加美观
                    effectType = com.intellij.openapi.editor.markup.EffectType.ROUNDED_BOX
                }
                
                // 添加高亮（精确范围）
                val highlighter = markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HIGHLIGHT_LAYER,
                    textAttributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
                
                // 设置工具提示
                val tooltip = buildTooltip(block)
                highlighter.errorStripeTooltip = tooltip
                
                highlighters.add(highlighter)
                
            } catch (e: Exception) {
                logger.error("Failed to apply highlight for block: $block", e)
            }
        }
        
        editorHighlighters[editor] = highlighters
        logger.info("Applied ${highlighters.size} highlights to editor for file: $filePath")
    }
    
    /**
     * 清除编辑器的覆盖率高亮
     */
    fun clearHighlights(editor: Editor) {
        editorHighlighters[editor]?.forEach { highlighter ->
            editor.markupModel.removeHighlighter(highlighter)
        }
        editorHighlighters.remove(editor)
    }
    
    /**
     * 清除所有编辑器的高亮
     */
    fun clearAllHighlights() {
        editorHighlighters.keys.toList().forEach { editor ->
            clearHighlights(editor)
        }
    }
    
    /**
     * 构建工具提示文本
     */
    private fun buildTooltip(block: CoverageBlock): String {
        val statusIcon = when {
            block.isCovered -> "✅"
            block.executionCount > 0 -> "⚠️"
            else -> "❌"
        }
        
        val statusText = when {
            block.isCovered -> "已覆盖"
            block.executionCount > 0 -> "部分覆盖"
            else -> "未覆盖"
        }
        
        val coveragePercent = if (block.numStatements > 0) {
            (block.executionCount.toDouble() / block.numStatements * 100).toInt()
        } else {
            0
        }
        
        return buildString {
            append("$statusIcon 覆盖率信息\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📍 位置: ${block.startLine}:${block.startCol} → ${block.endLine}:${block.endCol}\n")
            append("📊 语句数: ${block.numStatements}\n")
            append("🔄 执行次数: ${block.executionCount}\n")
            if (block.numStatements > 0) {
                append("📈 覆盖率: $coveragePercent%\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("$statusIcon 状态: $statusText")
        }
    }
}
