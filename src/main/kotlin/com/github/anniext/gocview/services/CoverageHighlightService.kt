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
        
        // 已覆盖代码的背景色（绿色）
        private val COVERED_BACKGROUND = JBColor(0xE8F5E9, 0x1B5E20)
        
        // 未覆盖代码的背景色（红色）
        private val UNCOVERED_BACKGROUND = JBColor(0xFFCDD2, 0xB71C1C)
        
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
        
        blocks.forEach { block ->
            try {
                // 计算起始和结束偏移量
                val startLine = (block.startLine - 1).coerceAtLeast(0)
                val endLine = (block.endLine - 1).coerceAtLeast(0)
                
                if (startLine >= document.lineCount || endLine >= document.lineCount) {
                    logger.warn("Line number out of range: $startLine-$endLine, document has ${document.lineCount} lines")
                    return@forEach
                }
                
                val startOffset = document.getLineStartOffset(startLine) + (block.startCol - 1).coerceAtLeast(0)
                val endOffset = document.getLineStartOffset(endLine) + (block.endCol - 1).coerceAtLeast(0)
                
                if (startOffset < 0 || endOffset > document.textLength || startOffset > endOffset) {
                    logger.warn("Invalid offset range: $startOffset-$endOffset")
                    return@forEach
                }
                
                // 创建文本属性
                val textAttributes = TextAttributes().apply {
                    backgroundColor = if (block.isCovered) COVERED_BACKGROUND else UNCOVERED_BACKGROUND
                }
                
                // 添加高亮
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
            append("覆盖率信息\n")
            append("位置: ${block.startLine}:${block.startCol} - ${block.endLine}:${block.endCol}\n")
            append("语句数: ${block.numStatements}\n")
            append("执行次数: ${block.executionCount}\n")
            append("状态: ${if (block.isCovered) "已覆盖" else "未覆盖"}")
        }
    }
}
