package com.github.anniext.gocview.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Goc 构建配置编辑器
 * 
 * 用于编辑 Goc 构建配置的 UI
 */
class GocBuildConfigurationEditor : SettingsEditor<GocBuildConfiguration>() {
    
    private val gocExecutableComboBox = ComboBox<String>().apply {
        isEditable = true
        model = DefaultComboBoxModel(detectGocExecutables().toTypedArray())
    }
    
    private val gocExecutablePanel = JPanel(BorderLayout()).apply {
        add(gocExecutableComboBox, BorderLayout.CENTER)
        add(createBrowseButton(), BorderLayout.EAST)
    }
    
    private fun createBrowseButton(): JButton {
        return JButton("浏览...").apply {
            addActionListener {
                val descriptor = FileChooserDescriptor(
                    true,  // chooseFiles
                    false, // chooseFolders
                    false, // chooseJars
                    false, // chooseJarsAsFiles
                    false, // chooseJarContents
                    false  // chooseMultiple
                ).withTitle("选择 Goc 可执行文件")
                    .withDescription("选择 goc 命令的路径")
                
                val chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, null, null)
                val files = chooser.choose(null)
                
                if (files.isNotEmpty()) {
                    val selectedPath = files[0].path
                    gocExecutableComboBox.selectedItem = selectedPath
                    
                    // 如果选择的路径不在列表中，添加到列表
                    val model = gocExecutableComboBox.model as DefaultComboBoxModel<String>
                    var found = false
                    for (i in 0 until model.size) {
                        if (model.getElementAt(i) == selectedPath) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        model.addElement(selectedPath)
                    }
                }
            }
        }
    }
    
    private val workingDirectoryField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "选择工作目录",
            "选择 Go 项目的根目录",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }
    
    private val gocCommandField = JBTextField("goc build", 30)
    
    private val gocArgsField = JBTextField(30)
    
    private val outputPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "选择输出路径",
            "选择编译输出的二进制文件路径",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
    }
    
    private val mainPanel: JPanel = createPanel()
    
    /**
     * 检测环境变量 PATH 中的所有 goc 可执行文件
     */
    private fun detectGocExecutables(): List<String> {
        val gocPaths = mutableListOf<String>()
        
        // 获取 PATH 环境变量
        val pathEnv = System.getenv("PATH")
        if (pathEnv != null) {
            val pathSeparator = File.pathSeparator
            val paths = pathEnv.split(pathSeparator)
            
            // 在每个 PATH 目录中查找 goc
            for (path in paths) {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) continue
                
                // 检查 goc 可执行文件
                val gocFile = File(dir, "goc")
                if (gocFile.exists() && gocFile.canExecute()) {
                    val absolutePath = gocFile.absolutePath
                    if (!gocPaths.contains(absolutePath)) {
                        gocPaths.add(absolutePath)
                    }
                }
            }
        }
        
        // 如果没有找到任何 goc，添加 "<无 SDK>"
        if (gocPaths.isEmpty()) {
            gocPaths.add("<无 SDK>")
        }
        
        return gocPaths
    }
    
    private fun createPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        
        gbc.insets = JBUI.insets(5)
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        
        // Goc 可执行文件路径
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JBLabel("Goc 路径:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(gocExecutablePanel, gbc)
        
        // 工作目录
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JBLabel("工作目录:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(workingDirectoryField, gbc)
        
        // Goc 命令
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        panel.add(JBLabel("Goc 命令:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(gocCommandField, gbc)
        
        // 命令参数
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.weightx = 0.0
        panel.add(JBLabel("命令参数:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(gocArgsField, gbc)
        
        // 输出路径
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.weightx = 0.0
        panel.add(JBLabel("输出路径:"), gbc)
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(outputPathField, gbc)
        
        // 填充剩余空间
        gbc.gridx = 0
        gbc.gridy = 5
        gbc.gridwidth = 2
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        panel.add(JPanel(), gbc)
        
        return panel
    }
    
    override fun resetEditorFrom(configuration: GocBuildConfiguration) {
        // 如果配置的路径为空，使用检测到的第一个路径
        val pathToUse = if (configuration.gocExecutablePath.isEmpty()) {
            val model = gocExecutableComboBox.model as DefaultComboBoxModel<String>
            if (model.size > 0) model.getElementAt(0) else "<无 SDK>"
        } else {
            configuration.gocExecutablePath
        }
        
        gocExecutableComboBox.selectedItem = pathToUse
        workingDirectoryField.text = configuration.workingDirectory
        gocCommandField.text = configuration.gocCommand
        gocArgsField.text = configuration.gocArgs
        outputPathField.text = configuration.outputPath
    }
    
    override fun applyEditorTo(configuration: GocBuildConfiguration) {
        val selectedPath = gocExecutableComboBox.selectedItem?.toString() ?: ""
        // 如果选择的是 "<无 SDK>"，保存为空字符串
        configuration.gocExecutablePath = if (selectedPath == "<无 SDK>") "" else selectedPath
        configuration.workingDirectory = workingDirectoryField.text
        configuration.gocCommand = gocCommandField.text
        configuration.gocArgs = gocArgsField.text
        configuration.outputPath = outputPathField.text
    }
    
    override fun createEditor(): JComponent = mainPanel
}
