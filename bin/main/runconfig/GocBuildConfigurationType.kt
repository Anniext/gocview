package com.github.anniext.gocview.runconfig

import com.github.anniext.gocview.MyBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Goc 构建配置类型
 * 
 * 在"添加新配置"菜单中显示
 */
class GocBuildConfigurationType : ConfigurationType {
    
    override fun getDisplayName(): String = MyBundle.message("goc.build.configuration.name")
    
    override fun getConfigurationTypeDescription(): String = 
        MyBundle.message("goc.build.configuration.description")
    
    override fun getIcon(): Icon = AllIcons.Actions.Compile
    
    override fun getId(): String = "GocBuildConfiguration"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(GocBuildConfigurationFactory(this))
    }
}
