package com.github.anniext.gocview.toolWindow

import com.github.anniext.gocview.model.CoverageBlock
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 覆盖率详细信息面板
 * 
 * 显示选中文件的详细覆盖率块信息
 */
class CoverageDetailPanel : JBPanel<JBPanel<*>>(BorderLayout()) {
    
    private val titleLabel = JBLabel("详细覆盖率信息").apply {
        font = font.deriveFont(Font.BOLD, 13f)
        border = BorderFactory.createEmptyBorder(5, 5, 2, 5)
    }
    
    private val hintLabel = JBLabel("💡 双击代码块跳转到具体位置").apply {
        foreground = JBColor.GRAY
        font = font.deriveFont(Font.ITALIC, 11f)
        border = BorderFactory.createEmptyBorder(0, 5, 5, 5)
    }
    
    private val detailTable: JBTable
    private val tableModel: DefaultTableModel
    
    private var currentProject: Project? = null
    private var currentFilePath: String? = null
    private var currentBlocks: List<CoverageBlock> = emptyList()
    
    init {
        // 创建详细信息表格
        tableModel = object : DefaultTableModel(
            arrayOf("起始位置", "结束位置", "语句数", "执行次数", "状态"),
            0
        ) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        
        detailTable = JBTable(tableModel).apply {
            setShowGrid(true)
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            rowHeight = 26  // 增加行高
            
            // 设置列宽
            columnModel.getColumn(0).preferredWidth = 120 // 起始位置
            columnModel.getColumn(1).preferredWidth = 120 // 结束位置
            columnModel.getColumn(2).preferredWidth = 80  // 语句数
            columnModel.getColumn(3).preferredWidth = 100 // 执行次数
            columnModel.getColumn(4).preferredWidth = 100 // 状态
            
            // 设置所有列的渲染器
            columnModel.getColumn(0).cellRenderer = PositionRenderer()
            columnModel.getColumn(1).cellRenderer = PositionRenderer()
            columnModel.getColumn(2).cellRenderer = CenterAlignRenderer()
            columnModel.getColumn(3).cellRenderer = ExecutionCountRenderer()
            columnModel.getColumn(4).cellRenderer = CoverageStatusRenderer()
            
            // 添加双击事件，跳转到代码位置
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val row = rowAtPoint(e.point)
                        if (row >= 0 && row < currentBlocks.size) {
                            navigateToCode(currentBlocks[row])
                        }
                    }
                }
                
                override fun mouseEntered(e: MouseEvent) {
                    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                }
                
                override fun mouseExited(e: MouseEvent) {
                    cursor = java.awt.Cursor.getDefaultCursor()
                }
            })
            
            // 添加键盘快捷键支持（Enter 键跳转）
            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                        val row = selectedRow
                        if (row >= 0 && row < currentBlocks.size) {
                            navigateToCode(currentBlocks[row])
                        }
                    }
                }
            })
        }
        
        // 顶部面板（标题 + 提示）
        val topPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(titleLabel)
            add(hintLabel)
        }
        
        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(detailTable), BorderLayout.CENTER)
    }
    
    /**
     * 显示文件的详细覆盖率信息
     */
    fun showFileDetails(project: Project, filePath: String, blocks: List<CoverageBlock>) {
        currentProject = project
        currentFilePath = filePath
        currentBlocks = blocks.sortedBy { it.startLine }
        
        // 只显示文件名，完整路径作为工具提示
        val fileName = filePath.substringAfterLast("/")
        titleLabel.text = "📄 $fileName (${blocks.size} 个代码块)"
        titleLabel.toolTipText = filePath
        
        // 清空现有数据
        tableModel.rowCount = 0
        
        // 添加每个覆盖率块的详细信息
        currentBlocks.forEach { block ->
            tableModel.addRow(
                arrayOf(
                    "${block.startLine}:${block.startCol}",
                    "${block.endLine}:${block.endCol}",
                    block.numStatements,
                    block.executionCount,
                    if (block.isCovered) "已覆盖" else "未覆盖"
                )
            )
        }
        
        if (blocks.isEmpty()) {
            tableModel.addRow(
                arrayOf("暂无数据", "-", "-", "-", "-")
            )
        }
    }
    
    /**
     * 跳转到代码位置
     */
    private fun navigateToCode(block: CoverageBlock) {
        val project = currentProject ?: return
        val filePath = currentFilePath ?: return
        
        // 查找文件
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
            ?: LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$filePath")
        
        if (virtualFile != null) {
            // 打开文件并跳转到指定行
            val line = (block.startLine - 1).coerceAtLeast(0)
            val column = (block.startCol - 1).coerceAtLeast(0)
            
            val descriptor = OpenFileDescriptor(project, virtualFile, line, column)
            FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            
            // 更新提示信息
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                val fileName = filePath.substringAfterLast("/")
                titleLabel.text = "📄 $fileName (${currentBlocks.size} 个代码块) - 已跳转到 ${block.startLine}:${block.startCol}"
                
                // 3 秒后恢复原始标题
                Timer(3000) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        titleLabel.text = "📄 $fileName (${currentBlocks.size} 个代码块)"
                    }
                }.apply {
                    isRepeats = false
                    start()
                }
            }
        } else {
            // 文件未找到提示
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                titleLabel.text = "❌ 文件未找到: $filePath"
            }
        }
    }
    
    /**
     * 清空详细信息
     */
    fun clear() {
        titleLabel.text = "详细覆盖率信息"
        tableModel.rowCount = 0
    }
    
    /**
     * 位置渲染器
     */
    private class PositionRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.CENTER
        }
        
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            
            if (value is String && value.contains(":")) {
                component.text = "$value"
                component.font = component.font.deriveFont(Font.PLAIN)
            }
            
            return component
        }
    }
    
    /**
     * 居中对齐渲染器
     */
    private class CenterAlignRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.CENTER
        }
    }
    
    /**
     * 执行次数渲染器
     */
    private class ExecutionCountRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.CENTER
        }
        
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            
            if (value is Int || value is String) {
                val count = value.toString().toIntOrNull() ?: 0
                
                // 根据执行次数设置不同的显示样式
                val displayText = when {
                    count > 1000 -> "🔥 ${count / 1000}k+"
                    count > 100 -> "⚡ $count"
                    count > 0 -> "✓ $count"
                    else -> "- $count"
                }
                
                component.text = displayText
                component.font = component.font.deriveFont(Font.BOLD)
                
                if (!isSelected) {
                    component.foreground = when {
                        count > 100 -> JBColor(java.awt.Color(230, 81, 0), java.awt.Color(255, 167, 38))
                        count > 0 -> JBColor(java.awt.Color(46, 125, 50), java.awt.Color(129, 199, 132))
                        else -> JBColor.GRAY
                    }
                }
            }
            
            return component
        }
    }
    
    /**
     * 覆盖状态渲染器
     */
    private class CoverageStatusRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.CENTER
            isOpaque = true
        }
        
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            
            if (value == "已覆盖") {
                if (!isSelected) {
                    component.background = JBColor(
                        java.awt.Color(200, 250, 205),
                        java.awt.Color(50, 120, 60)
                    )
                    component.foreground = JBColor(
                        java.awt.Color(27, 94, 32),
                        java.awt.Color(200, 250, 205)
                    )
                }
                component.text = "✓ 已覆盖"
                component.font = component.font.deriveFont(Font.BOLD)
            } else if (value == "未覆盖") {
                if (!isSelected) {
                    component.background = JBColor(
                        java.awt.Color(255, 205, 210),
                        java.awt.Color(183, 28, 28)
                    )
                    component.foreground = JBColor(
                        java.awt.Color(183, 28, 28),
                        java.awt.Color(255, 205, 210)
                    )
                }
                component.text = "✗ 未覆盖"
                component.font = component.font.deriveFont(Font.BOLD)
            }
            
            component.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            
            return component
        }
    }
}
