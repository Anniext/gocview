package com.github.anniext.gocview.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * 工具窗口工厂
 */
class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val coverageToolWindow = CoverageToolWindow(project)
        val content = ContentFactory.getInstance().createContent(
            coverageToolWindow.getContent(), 
            "覆盖率", 
            false
        )
        toolWindow.contentManager.addContent(content)
        
        // 将工具窗口实例保存到项目服务中，以便其他组件访问
        project.putUserData(COVERAGE_TOOL_WINDOW_KEY, coverageToolWindow)
    }

    override fun shouldBeAvailable(project: Project) = true
    
    companion object {
        val COVERAGE_TOOL_WINDOW_KEY = com.intellij.openapi.util.Key.create<CoverageToolWindow>("gocview.coverage.toolwindow")
    }
}
