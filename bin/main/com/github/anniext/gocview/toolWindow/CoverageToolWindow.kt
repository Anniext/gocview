package com.github.anniext.gocview.toolWindow

import com.github.anniext.gocview.model.CoverageBlock
import com.github.anniext.gocview.model.FileCoverage
import com.github.anniext.gocview.services.CoverageEditorManager
import com.github.anniext.gocview.services.GocCoverageService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 覆盖率工具窗口
 */
class CoverageToolWindow(private val project: Project) {
    
    private val logger = thisLogger()
    private val coverageService = GocCoverageService.getInstance(project)
    private val editorManager = CoverageEditorManager.getInstance(project)
    
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val statusLabel = JBLabel("等待 goc server 启动...")
    private val refreshButton = JButton("刷新覆盖率")
    private val coverageTable: JBTable
    private val tableModel: DefaultTableModel
    private val detailPanel = CoverageDetailPanel()
    
    private var currentServerUrl: String? = null
    private var currentFileCoverages: List<FileCoverage> = emptyList()
    
    init {
        // 创建表格模型
        tableModel = object : DefaultTableModel(
            arrayOf("文件", "覆盖率", "已覆盖", "总语句数", "执行次数"),
            0
        ) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        
        coverageTable = JBTable(tableModel).apply {
            setShowGrid(true)
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            
            // 设置列宽
            columnModel.getColumn(0).preferredWidth = 300 // 文件
            columnModel.getColumn(1).preferredWidth = 100 // 覆盖率
            columnModel.getColumn(2).preferredWidth = 80  // 已覆盖
            columnModel.getColumn(3).preferredWidth = 80  // 总语句数
            columnModel.getColumn(4).preferredWidth = 80  // 执行次数
            
            // 设置覆盖率列的渲染器
            columnModel.getColumn(1).cellRenderer = CoveragePercentageRenderer()
            
            // 添加行选择监听器
            selectionModel.addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    val selectedRow = selectedRow
                    if (selectedRow >= 0 && selectedRow < currentFileCoverages.size) {
                        val fileCoverage = currentFileCoverages[selectedRow]
                        detailPanel.showFileDetails(project, fileCoverage.filePath, fileCoverage.blocks)
                    }
                }
            }
        }
        
        // 顶部面板
        val topPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(statusLabel)
            add(Box.createHorizontalGlue())
            add(refreshButton)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        
        // 刷新按钮事件
        refreshButton.addActionListener {
            refreshCoverageData()
        }
        refreshButton.isEnabled = false
        
        // 创建分割面板
        val splitPane = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            JBScrollPane(coverageTable),
            detailPanel
        ).apply {
            dividerLocation = 300
            resizeWeight = 0.6
        }
        
        // 组装主面板
        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(splitPane, BorderLayout.CENTER)
    }
    
    fun getContent(): JComponent = mainPanel
    
    /**
     * 当检测到 goc server 时调用
     */
    fun onGocServerDetected(serverUrl: String) {
        currentServerUrl = serverUrl
        
        ApplicationManager.getApplication().invokeLater {
            statusLabel.text = "Goc Server: $serverUrl"
            refreshButton.isEnabled = true
            
            // 自动刷新一次
            refreshCoverageData()
        }
    }
    
    /**
     * 刷新覆盖率数据
     */
    private fun refreshCoverageData() {
        val serverUrl = currentServerUrl ?: return
        
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                statusLabel.text = "正在获取覆盖率数据..."
                refreshButton.isEnabled = false
                
                // 获取覆盖率数据
                val result = coverageService.fetchCoverageData(serverUrl)
                
                result.onSuccess { rawData ->
                    // 解析数据
                    val blocks = coverageService.parseCoverageData(rawData)
                    val fileCoverages = coverageService.groupByFile(blocks)
                    
                    // 更新编辑器高亮
                    val coverageMap = fileCoverages.associate { it.filePath to it.blocks }
                    editorManager.updateCoverageData(coverageMap)
                    
                    // 更新 UI
                    ApplicationManager.getApplication().invokeLater {
                        updateTable(fileCoverages, blocks)
                        statusLabel.text = "覆盖率数据已更新 (${fileCoverages.size} 个文件)"
                        refreshButton.isEnabled = true
                    }
                }
                
                result.onFailure { error ->
                    logger.error("Failed to fetch coverage data", error)
                    ApplicationManager.getApplication().invokeLater {
                        refreshButton.isEnabled = true
                        
                        // 针对 NoProfilesException 提供更友好的提示
                        if (error is GocCoverageService.NoProfilesException) {
                            statusLabel.text = "暂无覆盖率数据，请先触发代码执行"
                            
                            // 在表格中显示提示信息
                            tableModel.rowCount = 0
                            tableModel.addRow(
                                arrayOf(
                                    "暂无覆盖率数据",
                                    "-",
                                    "-",
                                    "-",
                                    "-"
                                )
                            )
                            tableModel.addRow(
                                arrayOf(
                                    "提示：请先触发应用程序的功能，然后点击「刷新覆盖率」",
                                    "",
                                    "",
                                    "",
                                    ""
                                )
                            )
                            detailPanel.clear()
                        } else {
                            statusLabel.text = "获取覆盖率数据失败: ${error.message}"
                            
                            // 在表格中显示错误信息
                            tableModel.rowCount = 0
                            tableModel.addRow(
                                arrayOf(
                                    "获取覆盖率数据失败",
                                    "-",
                                    "-",
                                    "-",
                                    "-"
                                )
                            )
                            tableModel.addRow(
                                arrayOf(
                                    "错误: ${error.message}",
                                    "",
                                    "",
                                    "",
                                    ""
                                )
                            )
                            detailPanel.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error refreshing coverage data", e)
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = "刷新失败"
                    refreshButton.isEnabled = true
                }
            }
        }
    }
    
    /**
     * 更新表格数据
     */
    private fun updateTable(fileCoverages: List<FileCoverage>, allBlocks: List<CoverageBlock>) {
        // 保存当前数据
        currentFileCoverages = fileCoverages
        
        // 清空现有数据
        tableModel.rowCount = 0
        
        // 添加文件级别的汇总
        fileCoverages.forEach { fileCoverage ->
            val totalExecutions = fileCoverage.blocks.sumOf { it.executionCount }
            tableModel.addRow(
                arrayOf(
                    fileCoverage.filePath,
                    String.format("%.2f%%", fileCoverage.coveragePercentage),
                    fileCoverage.coveredStatements,
                    fileCoverage.totalStatements,
                    totalExecutions
                )
            )
        }
        
        // 如果没有数据，显示提示
        if (fileCoverages.isEmpty()) {
            tableModel.addRow(
                arrayOf("暂无覆盖率数据", "-", "-", "-", "-")
            )
            detailPanel.clear()
        } else {
            // 自动选择第一行
            coverageTable.setRowSelectionInterval(0, 0)
        }
    }
    
    /**
     * 覆盖率百分比渲染器
     * 根据覆盖率高低显示不同颜色
     */
    private class CoveragePercentageRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value is String && value.endsWith("%")) {
                try {
                    val percentage = value.removeSuffix("%").toDouble()
                    
                    // 根据覆盖率设置背景色
                    component.background = when {
                        percentage >= 80.0 -> JBColor(0xE8F5E9, 0x1B5E20) // 绿色
                        percentage >= 50.0 -> JBColor(0xFFF9C4, 0xF57F17) // 黄色
                        percentage > 0.0 -> JBColor(0xFFECB3, 0xE65100)   // 橙色
                        else -> JBColor(0xFFCDD2, 0xB71C1C)                // 红色
                    }
                } catch (e: NumberFormatException) {
                    // 忽略解析错误
                }
            }
            
            return component
        }
    }
}
