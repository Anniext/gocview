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
        font = font.deriveFont(Font.BOLD, 14f)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
            
            // 设置列宽
            columnModel.getColumn(0).preferredWidth = 120 // 起始位置
            columnModel.getColumn(1).preferredWidth = 120 // 结束位置
            columnModel.getColumn(2).preferredWidth = 80  // 语句数
            columnModel.getColumn(3).preferredWidth = 80  // 执行次数
            columnModel.getColumn(4).preferredWidth = 100 // 状态
            
            // 设置状态列的渲染器
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
            })
        }
        
        add(titleLabel, BorderLayout.NORTH)
        add(JBScrollPane(detailTable), BorderLayout.CENTER)
    }
    
    /**
     * 显示文件的详细覆盖率信息
     */
    fun showFileDetails(project: Project, filePath: String, blocks: List<CoverageBlock>) {
        currentProject = project
        currentFilePath = filePath
        currentBlocks = blocks.sortedBy { it.startLine }
        
        titleLabel.text = "文件: $filePath (${blocks.size} 个代码块) - 双击跳转到代码"
        
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
     * 覆盖状态渲染器
     */
    private class CoverageStatusRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value == "已覆盖") {
                component.background = JBColor(0xE8F5E9, 0x1B5E20) // 绿色
            } else if (value == "未覆盖") {
                component.background = JBColor(0xFFCDD2, 0xB71C1C) // 红色
            }
            
            return component
        }
    }
}
