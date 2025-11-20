package com.github.anniext.gocview.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Go 模块路径解析器
 * 
 * 将 Go 模块路径（如 github.com/user/repo/main.go）转换为实际的文件系统路径
 */
@Service(Service.Level.PROJECT)
class GoModulePathResolver(private val project: Project) {
    
    private val logger = thisLogger()
    
    // 缓存模块路径映射
    private val modulePathCache = mutableMapOf<String, String>()
    private var projectModuleName: String? = null
    
    companion object {
        fun getInstance(project: Project): GoModulePathResolver {
            return project.getService(GoModulePathResolver::class.java)
        }
    }
    
    init {
        // 初始化时读取项目的模块名
        projectModuleName = readProjectModuleName()
        logger.info("Project module name: $projectModuleName")
    }
    
    /**
     * 将模块路径转换为实际文件路径
     * 
     * @param modulePath Go 模块路径，如 "github.com/user/repo/main.go"
     * @return 实际的文件系统路径，如果找不到则返回 null
     */
    fun resolveModulePath(modulePath: String): VirtualFile? {
        logger.info("Attempting to resolve module path: $modulePath")
        
        // 先检查缓存
        modulePathCache[modulePath]?.let { cachedPath ->
            LocalFileSystem.getInstance().findFileByPath(cachedPath)?.let {
                logger.info("Found in cache: $modulePath -> $cachedPath")
                return it
            }
        }
        
        // 尝试多种解析策略
        val strategies = listOf(
            "Project File" to ::resolveAsProjectFile,
            "Relative Path" to ::resolveAsRelativePath,
            "GOPATH" to ::resolveInGoPath,
            "Go Mod Cache" to ::resolveInGoModCache
        )
        
        for ((strategyName, strategy) in strategies) {
            logger.info("Trying strategy: $strategyName for $modulePath")
            strategy(modulePath)?.let { file ->
                // 缓存成功的解析结果
                modulePathCache[modulePath] = file.path
                logger.info("✓ Resolved module path using $strategyName: $modulePath -> ${file.path}")
                return file
            }
        }
        
        logger.warn("✗ Failed to resolve module path: $modulePath (tried all strategies)")
        logger.warn("  Project module name: $projectModuleName")
        logger.warn("  Project base path: ${project.basePath}")
        return null
    }
    
    /**
     * 策略1：作为项目文件解析
     * 如果模块路径以项目模块名开头，则去掉模块名前缀，在项目中查找
     */
    private fun resolveAsProjectFile(modulePath: String): VirtualFile? {
        val moduleName = projectModuleName ?: return null
        
        if (modulePath.startsWith(moduleName)) {
            // 去掉模块名前缀
            val relativePath = modulePath.removePrefix(moduleName).removePrefix("/")
            val fullPath = "${project.basePath}/$relativePath"
            
            return LocalFileSystem.getInstance().findFileByPath(fullPath)
        }
        
        return null
    }
    
    /**
     * 策略2：作为相对路径解析
     * 直接在项目根目录下查找
     */
    private fun resolveAsRelativePath(modulePath: String): VirtualFile? {
        // 提取文件路径部分（去掉可能的模块前缀）
        val fileName = modulePath.substringAfterLast('/')
        logger.info("  Searching for file: $fileName in project")
        
        // 在项目中搜索同名文件
        val result = findFileInProject(fileName)
        if (result != null) {
            logger.info("  Found file: ${result.path}")
        }
        return result
    }
    
    /**
     * 策略3：在 GOPATH 中查找
     */
    private fun resolveInGoPath(modulePath: String): VirtualFile? {
        val goPath = System.getenv("GOPATH") ?: return null
        val fullPath = "$goPath/src/$modulePath"
        
        return LocalFileSystem.getInstance().findFileByPath(fullPath)
    }
    
    /**
     * 策略4：在 Go mod cache 中查找
     */
    private fun resolveInGoModCache(modulePath: String): VirtualFile? {
        val goPath = System.getenv("GOPATH") ?: return null
        
        // Go mod cache 通常在 $GOPATH/pkg/mod
        val parts = modulePath.split('/')
        if (parts.size < 2) return null
        
        val domain = parts[0]
        val rest = parts.drop(1).joinToString("/")
        
        // 尝试不同的版本路径
        val modCachePath = "$goPath/pkg/mod/$domain"
        val modCacheDir = File(modCachePath)
        
        if (modCacheDir.exists() && modCacheDir.isDirectory) {
            // 在 mod cache 中搜索
            modCacheDir.listFiles()?.forEach { versionDir ->
                if (versionDir.isDirectory) {
                    val filePath = "${versionDir.absolutePath}/$rest"
                    LocalFileSystem.getInstance().findFileByPath(filePath)?.let {
                        return it
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * 在项目中搜索文件
     */
    private fun findFileInProject(fileName: String): VirtualFile? {
        val basePath = project.basePath ?: return null
        val baseDir = File(basePath)
        
        // 递归搜索文件
        val foundFile = searchFileRecursively(baseDir, fileName)
        return foundFile?.let { LocalFileSystem.getInstance().findFileByPath(it.absolutePath) }
    }
    
    /**
     * 递归搜索文件
     */
    private fun searchFileRecursively(dir: File, fileName: String, maxDepth: Int = 10, currentDepth: Int = 0): File? {
        if (currentDepth > maxDepth) return null
        if (!dir.exists() || !dir.isDirectory) return null
        
        // 跳过一些常见的不需要搜索的目录
        val skipDirs = setOf(".git", ".idea", "node_modules", "vendor", "build", "target", ".gradle")
        if (dir.name in skipDirs) return null
        
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name == fileName) {
                return file
            } else if (file.isDirectory) {
                searchFileRecursively(file, fileName, maxDepth, currentDepth + 1)?.let {
                    return it
                }
            }
        }
        
        return null
    }
    
    /**
     * 读取项目的 go.mod 文件，获取模块名
     */
    private fun readProjectModuleName(): String? {
        val basePath = project.basePath ?: return null
        val goModFile = File("$basePath/go.mod")
        
        if (!goModFile.exists()) {
            logger.warn("go.mod file not found in project root")
            return null
        }
        
        try {
            goModFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("module ")) {
                    val moduleName = trimmed.removePrefix("module").trim()
                    return moduleName
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to read go.mod file", e)
        }
        
        return null
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        modulePathCache.clear()
        projectModuleName = readProjectModuleName()
    }
}
