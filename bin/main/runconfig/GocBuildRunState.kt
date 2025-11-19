package com.github.anniext.gocview.runconfig

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import java.io.File

/**
 * Goc 构建运行状态
 * 
 * 负责执行 goc 构建命令
 */
class GocBuildRunState(
    environment: ExecutionEnvironment,
    private val configuration: GocBuildConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val commandLine = createCommandLine()
        val processHandler = KillableColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
    
    private fun createCommandLine(): GeneralCommandLine {
        val workDir = File(configuration.workingDirectory)
        
        // 获取 goc 可执行文件路径
        val gocPath = when {
            configuration.gocExecutablePath.isEmpty() -> "goc"
            configuration.gocExecutablePath == "<无 SDK>" -> "goc"
            else -> configuration.gocExecutablePath
        }
        
        // 构建命令行
        val commandLine = GeneralCommandLine()
            .withWorkDirectory(workDir)
            .withExePath(gocPath)
        
        // 添加命令（如 build）
        val commandParts = configuration.gocCommand.split(" ").filter { it.isNotEmpty() }
        commandParts.forEach { commandLine.addParameter(it) }
        
        // 添加额外参数
        if (configuration.gocArgs.isNotEmpty()) {
            val args = configuration.gocArgs.split(" ").filter { it.isNotEmpty() }
            args.forEach { commandLine.addParameter(it) }
        }
        
        // 添加输出路径
        if (configuration.outputPath.isNotEmpty()) {
            commandLine.addParameter("-o")
            commandLine.addParameter(configuration.outputPath)
        }
        
        return commandLine
    }
}
