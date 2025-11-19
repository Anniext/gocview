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
        
        // 已覆盖代码的背景色（柔和的绿色）
        private val COVERED_BACKGROUND = JBColor(
            java.awt.Color(200, 250, 205, 100),  // 浅色主题：半透明浅绿色
            java.awt.Color(50, 120, 60, 80)      // 深色主题：半透明深绿色
        )
        
        // 已覆盖代码的边框色
        private val COVERED_BORDER = JBColor(
            java.awt.Color(100, 200, 110),       // 浅色主题：绿色边框
            java.awt.Color(80, 180, 90)          // 深色主题：绿色边框
        )
        
        // 高亮层级
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
        
        // 只高亮已覆盖的代码块
        blocks.filter { it.isCovered }.forEach { block ->
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
                
                // 创建文本属性（带背景色和下划线）
                val textAttributes = TextAttributes().apply {
                    backgroundColor = COVERED_BACKGROUND
                    effectColor = COVERED_BORDER
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
        return buildString {
            append("✓ 覆盖率信息\n")
            append("━━━━━━━━━━━━━━━━\n")
            append("📍 位置: ${block.startLine}:${block.startCol} → ${block.endLine}:${block.endCol}\n")
            append("📊 语句数: ${block.numStatements}\n")
            append("🔄 执行次数: ${block.executionCount}\n")
            append("✅ 状态: 已覆盖")
        }
    }
}
