package com.github.anniext.gocview.startup

import com.github.anniext.gocview.listeners.RunConfigurationListener
import com.intellij.execution.ExecutionManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * 项目启动活动
 * 
 * 检查 Go 插件是否已安装，注册运行配置监听器
 */
class MyProjectActivity : ProjectActivity {

    companion object {
        private const val GO_PLUGIN_ID = "org.jetbrains.plugins.go"
    }

    override suspend fun execute(project: Project) {
        checkGoPluginInstalled(project)
        registerRunConfigurationListener(project)
    }
    
    private fun checkGoPluginInstalled(project: Project) {
        val goPlugin = PluginManagerCore.getPlugin(PluginId.getId(GO_PLUGIN_ID))
        
        if (goPlugin == null || !goPlugin.isEnabled) {
            showGoPluginNotInstalledNotification(project)
        }
    }
    
    private fun showGoPluginNotInstalledNotification(project: Project) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Gocview Notifications")
            .createNotification(
                "Gocview 需要 Go 插件支持",
                "为了使用 Gocview 的完整功能，请先安装 Go 插件。\n" +
                "您可以在 Settings/Preferences → Plugins 中搜索并安装 \"Go\" 插件。",
                NotificationType.WARNING
            )
        
        Notifications.Bus.notify(notification, project)
    }
    
    /**
     * 注册运行配置监听器
     */
    private fun registerRunConfigurationListener(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            RunConfigurationListener(project)
        )
    }
}