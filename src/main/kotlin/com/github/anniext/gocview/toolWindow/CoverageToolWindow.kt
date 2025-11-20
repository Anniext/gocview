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
    private val clearButton = JButton("清除高亮")
    private val coverageTable: JBTable
    private val tableModel: DefaultTableModel
    private val detailPanel = CoverageDetailPanel()
    
    private var currentServerUrl: String? = null
    private var currentFileCoverages: List<FileCoverage> = emptyList()
    
    companion object {
        // 延迟刷新时间（毫秒）
        private const val REFRESH_DELAY_MS = 3000L
    }
    
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
            rowHeight = 28  // 增加行高
            
            // 设置列宽
            columnModel.getColumn(0).preferredWidth = 300 // 文件
            columnModel.getColumn(1).preferredWidth = 100 // 覆盖率
            columnModel.getColumn(2).preferredWidth = 80  // 已覆盖
            columnModel.getColumn(3).preferredWidth = 80  // 总语句数
            columnModel.getColumn(4).preferredWidth = 80  // 执行次数
            
            // 设置所有列的渲染器
            columnModel.getColumn(0).cellRenderer = FilePathRenderer()
            columnModel.getColumn(1).cellRenderer = CoveragePercentageRenderer()
            columnModel.getColumn(2).cellRenderer = NumberRenderer()
            columnModel.getColumn(3).cellRenderer = NumberRenderer()
            columnModel.getColumn(4).cellRenderer = NumberRenderer()
            
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
            
            // 添加双击事件，跳转到文件
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    if (e.clickCount == 2) {
                        val row = rowAtPoint(e.point)
                        if (row >= 0 && row < currentFileCoverages.size) {
                            navigateToFile(currentFileCoverages[row])
                        }
                    }
                }
                
                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                }
                
                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    cursor = java.awt.Cursor.getDefaultCursor()
                }
            })
            
            // 添加键盘快捷键支持（Enter 键跳转）
            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                        val row = selectedRow
                        if (row >= 0 && row < currentFileCoverages.size) {
                            navigateToFile(currentFileCoverages[row])
                        }
                    }
                }
            })
        }
        
        // 提示标签
        val hintLabel = JBLabel("💡 双击文件跳转到代码").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(java.awt.Font.ITALIC, 11f)
        }
        
        // 状态信息面板（第一行）
        val statusPanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(statusLabel, BorderLayout.WEST)
            add(hintLabel, BorderLayout.CENTER)
        }
        
        // 按钮面板（第二行）
        val buttonPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            add(clearButton)
            add(Box.createHorizontalStrut(5))
            add(refreshButton)
        }
        
        // 顶部面板（垂直布局）
        val topPanel = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(statusPanel, BorderLayout.NORTH)
            add(buttonPanel, BorderLayout.SOUTH)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        
        // 刷新按钮事件
        refreshButton.addActionListener {
            refreshCoverageData()
        }
        refreshButton.isEnabled = false
        
        // 清除按钮事件
        clearButton.addActionListener {
            clearCoverageHighlights()
        }
        clearButton.toolTipText = "清除编辑器中的覆盖率高亮"
        
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
            refreshButton.isEnabled = true
            
            // 延迟刷新，给程序时间启动和初始化
            scheduleDelayedRefresh(REFRESH_DELAY_MS)
        }
    }
    
    /**
     * 延迟刷新覆盖率数据（带倒计时显示）
     */
    private fun scheduleDelayedRefresh(delayMillis: Long) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val startTime = System.currentTimeMillis()
                val endTime = startTime + delayMillis
                
                // 倒计时显示
                while (System.currentTimeMillis() < endTime) {
                    val remainingSeconds = ((endTime - System.currentTimeMillis()) / 1000.0).toInt() + 1
                    
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Goc Server: $currentServerUrl (${remainingSeconds}秒后自动刷新...)"
                    }
                    
                    Thread.sleep(500) // 每 0.5 秒更新一次
                }
                
                // 在 UI 线程中更新状态并刷新
                ApplicationManager.getApplication().invokeLater {
                    if (currentServerUrl != null) {
                        logger.info("Starting delayed coverage refresh after ${delayMillis}ms")
                        refreshCoverageData()
                    }
                }
            } catch (e: InterruptedException) {
                logger.warn("Delayed refresh was interrupted", e)
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = "Goc Server: $currentServerUrl"
                }
            }
        }
    }
    
    /**
     * 清除覆盖率高亮
     */
    private fun clearCoverageHighlights() {
        ApplicationManager.getApplication().invokeLater {
            try {
                editorManager.clearAllCoverage()
                statusLabel.text = "已清除所有覆盖率高亮"
                logger.info("Coverage highlights cleared manually")
                
                // 清空表格
                tableModel.rowCount = 0
                currentFileCoverages = emptyList()
                detailPanel.clear()
                
            } catch (e: Exception) {
                logger.error("Failed to clear coverage highlights", e)
                statusLabel.text = "清除高亮失败: ${e.message}"
            }
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
     * 跳转到文件
     */
    private fun navigateToFile(fileCoverage: FileCoverage) {
        val modulePath = fileCoverage.filePath
        
        // 使用路径解析器将模块路径转换为实际文件路径
        val pathResolver = com.github.anniext.gocview.services.GoModulePathResolver.getInstance(project)
        val virtualFile = pathResolver.resolveModulePath(modulePath)
        
        if (virtualFile != null) {
            // 找到第一个未覆盖的代码块，如果没有则跳转到第一个覆盖的代码块
            val targetBlock = fileCoverage.blocks.firstOrNull { !it.isCovered }
                ?: fileCoverage.blocks.firstOrNull()
            
            if (targetBlock != null) {
                // 打开文件并跳转到指定行
                val line = (targetBlock.startLine - 1).coerceAtLeast(0)
                val column = (targetBlock.startCol - 1).coerceAtLeast(0)
                
                val descriptor = com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile, line, column)
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            } else {
                // 如果没有代码块，就打开文件的第一行
                val descriptor = com.intellij.openapi.fileEditor.OpenFileDescriptor(project, virtualFile, 0, 0)
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            }
        } else {
            logger.warn("File not found for module path: $modulePath")
            statusLabel.text = "文件未找到: $modulePath (模块路径无法解析)"
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
     * 文件路径渲染器
     */
    private class FilePathRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.LEFT
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
            
            if (value is String && value.contains("/")) {
                // 只显示文件名，完整路径作为工具提示
                val fileName = value.substringAfterLast("/")
                component.text = "📄 $fileName"
                component.toolTipText = value
            }
            
            return component
        }
    }
    
    /**
     * 数字渲染器
     */
    private class NumberRenderer : DefaultTableCellRenderer() {
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
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (!isSelected) {
                component.background = JBColor.WHITE
            }
            
            return component
        }
    }
    
    /**
     * 覆盖率百分比渲染器
     * 根据覆盖率高低显示不同颜色
     */
    private class CoveragePercentageRenderer : DefaultTableCellRenderer() {
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
            
            if (value is String && value.endsWith("%")) {
                try {
                    val percentage = value.removeSuffix("%").toDouble()
                    
                    // 根据覆盖率设置背景色和前景色
                    val (bgColor, fgColor, icon) = when {
                        percentage >= 80.0 -> Triple(
                            JBColor(java.awt.Color(200, 250, 205), java.awt.Color(50, 120, 60)),
                            JBColor(java.awt.Color(27, 94, 32), java.awt.Color(200, 250, 205)),
                            "✓"
                        )
                        percentage >= 50.0 -> Triple(
                            JBColor(java.awt.Color(255, 249, 196), java.awt.Color(245, 127, 23)),
                            JBColor(java.awt.Color(245, 127, 23), java.awt.Color(255, 249, 196)),
                            "◐"
                        )
                        percentage > 0.0 -> Triple(
                            JBColor(java.awt.Color(255, 236, 179), java.awt.Color(230, 81, 0)),
                            JBColor(java.awt.Color(230, 81, 0), java.awt.Color(255, 236, 179)),
                            "◔"
                        )
                        else -> Triple(
                            JBColor(java.awt.Color(255, 205, 210), java.awt.Color(183, 28, 28)),
                            JBColor(java.awt.Color(183, 28, 28), java.awt.Color(255, 205, 210)),
                            "✗"
                        )
                    }
                    
                    if (!isSelected) {
                        component.background = bgColor
                        component.foreground = fgColor
                    }
                    
                    component.text = "$icon $value"
                    component.font = component.font.deriveFont(java.awt.Font.BOLD)
                    component.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
                    
                } catch (e: NumberFormatException) {
                    // 忽略解析错误
                }
            }
            
            return component
        }
    }
}
