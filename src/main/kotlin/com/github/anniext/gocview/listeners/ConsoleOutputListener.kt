package com.github.anniext.gocview.listeners

import com.github.anniext.gocview.services.GocCoverageService
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 控制台输出监听器
 * 
 * 监听运行配置的控制台输出，捕获 goc server 地址
 */
class ConsoleOutputListener(
    private val project: Project,
    private val onGocServerDetected: (String) -> Unit
) : ProcessAdapter() {
    
    private val logger = thisLogger()
    private val coverageService = GocCoverageService.getInstance(project)
    
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        
        // 尝试从输出中提取 goc server 地址
        val serverUrl = coverageService.extractGocServerUrl(text)
        if (serverUrl != null) {
            logger.info("Detected goc server: $serverUrl")
            
            // 自动打开覆盖率工具窗口
            ApplicationManager.getApplication().invokeLater {
                openCoverageToolWindow()
            }
            
            onGocServerDetected(serverUrl)
        }
    }
    
    /**
     * 打开覆盖率工具窗口
     */
    private fun openCoverageToolWindow() {
        try {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("Goc Coverage")
            
            if (toolWindow != null) {
                // 激活并显示工具窗口
                toolWindow.show()
                logger.info("Coverage tool window opened automatically")
            } else {
                logger.warn("Coverage tool window not found")
            }
        } catch (e: Exception) {
            logger.error("Failed to open coverage tool window", e)
        }
    }
}
