package com.github.anniext.gocview.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

/**
 * Goc 构建配置工厂
 */
class GocBuildConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    
    override fun getId(): String = "Goc Build"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return GocBuildConfiguration(project, this, "Goc Build")
    }
    
    override fun getOptionsClass() = GocBuildOptions::class.java
}
