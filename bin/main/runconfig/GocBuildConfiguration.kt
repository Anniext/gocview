package com.github.anniext.gocview.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element

/**
 * Goc 构建运行配置
 * 
 * 提供独立的 Goc 构建配置类型
 */
class GocBuildConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<GocBuildRunState>(project, factory, name) {
    
    var gocExecutablePath: String = ""
    var workingDirectory: String = project.basePath ?: ""
    var gocCommand: String = "run"
    var gocArgs: String = "."
    var outputPath: String = ""
    
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return GocBuildConfigurationEditor()
    }
    
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return GocBuildRunState(environment, this)
    }
    
    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.getChild("goc-build")?.let { gocElement ->
            gocExecutablePath = gocElement.getAttributeValue("gocExecutablePath") ?: gocExecutablePath
            workingDirectory = gocElement.getAttributeValue("workingDirectory") ?: workingDirectory
            gocCommand = gocElement.getAttributeValue("gocCommand") ?: gocCommand
            gocArgs = gocElement.getAttributeValue("gocArgs") ?: gocArgs
            outputPath = gocElement.getAttributeValue("outputPath") ?: outputPath
        }
    }
    
    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        val gocElement = Element("goc-build")
        gocElement.setAttribute("gocExecutablePath", gocExecutablePath)
        gocElement.setAttribute("workingDirectory", workingDirectory)
        gocElement.setAttribute("gocCommand", gocCommand)
        gocElement.setAttribute("gocArgs", gocArgs)
        gocElement.setAttribute("outputPath", outputPath)
        element.addContent(gocElement)
    }
}
