package com.github.anniext.gocview.services

import com.github.anniext.gocview.model.CoverageBlock
import com.github.anniext.gocview.model.FileCoverage
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.util.regex.Pattern

/**
 * Goc 覆盖率服务
 * 
 * 负责调用 goc 命令获取覆盖率数据并解析
 */
@Service(Service.Level.PROJECT)
class GocCoverageService(private val project: Project) {
    
    companion object {
        // 匹配 goc server 地址的正则表达式
        // 示例: [goc] goc server started: http://127.0.0.1:49598
        private val GOC_SERVER_PATTERN = Pattern.compile(
            "\\[goc\\]\\s+goc\\s+server\\s+started:\\s+(http?://[\\d.]+:\\d+)"
        )
        
        // 匹配覆盖率数据的正则表达式
        // 示例: git.bestfulfill.tech/devops/demo/main.go:8.13,9.6 1 1
        private val COVERAGE_PATTERN = Pattern.compile(
            "([^:]+):(\\d+)\\.(\\d+),(\\d+)\\.(\\d+)\\s+(\\d+)\\s+(\\d+)"
        )
        
        fun getInstance(project: Project): GocCoverageService {
            return project.getService(GocCoverageService::class.java)
        }
    }
    
    /**
     * 从控制台输出中提取 goc server 地址
     */
    fun extractGocServerUrl(consoleOutput: String): String? {
        val matcher = GOC_SERVER_PATTERN.matcher(consoleOutput)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
    
    /**
     * 调用 goc profile 命令获取覆盖率数据
     */
    fun fetchCoverageData(centerUrl: String): Result<String> {
        return runCatching {
            thisLogger().info("Fetching coverage data from: $centerUrl")
            
            val commandLine = GeneralCommandLine("goc", "profile", "--center=$centerUrl")
            commandLine.withWorkDirectory(project.basePath)
            
            val output: ProcessOutput = ExecUtil.execAndGetOutput(commandLine, 30000)
            
            if (output.exitCode != 0) {
                val errorMsg = output.stderr
                
                // 检查是否是 "no profiles" 错误
                if (errorMsg.contains("no profiles", ignoreCase = true)) {
                    throw NoProfilesException(
                        "Goc server 尚未收集到覆盖率数据。\n\n" +
                        "可能的原因：\n" +
                        "1. 程序刚启动，还没有执行任何代码\n" +
                        "2. 需要触发应用程序的功能（如访问 HTTP 端点、调用函数等）\n\n" +
                        "建议：\n" +
                        "- 执行一些操作来触发代码执行\n" +
                        "- 然后点击「刷新覆盖率」按钮重试"
                    )
                }
                
                throw RuntimeException("goc profile 命令执行失败: $errorMsg")
            }
            
            thisLogger().info("Coverage data fetched successfully")
            output.stdout
        }
    }
    
    /**
     * 自定义异常：表示 goc server 还没有收集到覆盖率数据
     */
    class NoProfilesException(message: String) : Exception(message)
    
    /**
     * 解析覆盖率数据
     */
    fun parseCoverageData(rawData: String): List<CoverageBlock> {
        val blocks = mutableListOf<CoverageBlock>()
        
        rawData.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            
            val matcher = COVERAGE_PATTERN.matcher(line)
            if (matcher.matches()) {
                try {
                    val block = CoverageBlock(
                        filePath = matcher.group(1),
                        startLine = matcher.group(2).toInt(),
                        startCol = matcher.group(3).toInt(),
                        endLine = matcher.group(4).toInt(),
                        endCol = matcher.group(5).toInt(),
                        numStatements = matcher.group(6).toInt(),
                        executionCount = matcher.group(7).toInt()
                    )
                    blocks.add(block)
                } catch (e: Exception) {
                    thisLogger().warn("Failed to parse coverage line: $line", e)
                }
            } else {
                thisLogger().debug("Skipping non-matching line: $line")
            }
        }
        
        return blocks
    }
    
    /**
     * 按文件分组覆盖率数据
     */
    fun groupByFile(blocks: List<CoverageBlock>): List<FileCoverage> {
        return blocks
            .groupBy { it.filePath }
            .map { (filePath, fileBlocks) ->
                FileCoverage(filePath, fileBlocks)
            }
            .sortedByDescending { it.coveragePercentage }
    }
}
