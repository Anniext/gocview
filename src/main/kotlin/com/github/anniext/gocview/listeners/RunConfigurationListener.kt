package com.github.anniext.gocview.listeners

import com.github.anniext.gocview.services.CoverageEditorManager
import com.github.anniext.gocview.toolWindow.MyToolWindowFactory
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * 运行配置监听器
 * 
 * 监听程序运行，附加控制台输出监听器
 */
class RunConfigurationListener(private val project: Project) : ExecutionListener {
    
    private val logger = thisLogger()
    
    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        logger.info("Process started: ${env.runProfile.name}")
        
        // 获取工具窗口实例
        val toolWindow = project.getUserData(MyToolWindowFactory.COVERAGE_TOOL_WINDOW_KEY)
        
        if (toolWindow != null) {
            // 附加控制台输出监听器
            val outputListener = ConsoleOutputListener(project) { serverUrl ->
                logger.info("Goc server detected in console: $serverUrl")
                toolWindow.onGocServerDetected(serverUrl)
            }
            
            handler.addProcessListener(outputListener)
            logger.info("Console output listener attached")
        } else {
            logger.warn("Coverage tool window not found")
        }
    }
    
    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
        logger.info("Process terminated: ${env.runProfile.name}, exit code: $exitCode")
        
        // 清除所有覆盖率高亮
        ApplicationManager.getApplication().invokeLater {
            try {
                val editorManager = CoverageEditorManager.getInstance(project)
                editorManager.clearAllCoverage()
                logger.info("Coverage highlights cleared after process termination")
            } catch (e: Exception) {
                logger.error("Failed to clear coverage highlights", e)
            }
        }
    }
}
