package com.github.anniext.gocview.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class GocCoverageServiceTest : BasePlatformTestCase() {
    
    private lateinit var service: GocCoverageService
    
    override fun setUp() {
        super.setUp()
        service = GocCoverageService.getInstance(project)
    }
    
    @Test
    fun testExtractGocServerUrl() {
        // 测试正常的 goc server 输出
        val output1 = "[goc] goc server started: http://127.0.0.1:49598"
        val url1 = service.extractGocServerUrl(output1)
        assertEquals("http://127.0.0.1:49598", url1)
        
        // 测试不包含 goc server 的输出
        val output3 = "Normal application output"
        val url3 = service.extractGocServerUrl(output3)
        assertNull(url3)
    }
    
    @Test
    fun testParseCoverageData() {
        val rawData = """
            git.bestfulfill.tech/devops/demo/main.go:8.13,9.6 1 1
            git.bestfulfill.tech/devops/demo/main.go:9.6,12.3 2 70
            git.bestfulfill.tech/devops/demo/handler.go:15.1,16.10 3 0
        """.trimIndent()
        
        val blocks = service.parseCoverageData(rawData)
        
        assertEquals(3, blocks.size)
        
        // 验证第一个块
        val block1 = blocks[0]
        assertEquals("git.bestfulfill.tech/devops/demo/main.go", block1.filePath)
        assertEquals(8, block1.startLine)
        assertEquals(13, block1.startCol)
        assertEquals(9, block1.endLine)
        assertEquals(6, block1.endCol)
        assertEquals(1, block1.numStatements)
        assertEquals(1, block1.executionCount)
        assertTrue(block1.isCovered)
        
        // 验证第二个块
        val block2 = blocks[1]
        assertEquals(2, block2.numStatements)
        assertEquals(70, block2.executionCount)
        
        // 验证第三个块（未覆盖）
        val block3 = blocks[2]
        assertEquals("git.bestfulfill.tech/devops/demo/handler.go", block3.filePath)
        assertEquals(0, block3.executionCount)
        assertFalse(block3.isCovered)
    }
    
    @Test
    fun testGroupByFile() {
        val rawData = """
            git.bestfulfill.tech/devops/demo/main.go:8.13,9.6 1 1
            git.bestfulfill.tech/devops/demo/main.go:9.6,12.3 2 70
            git.bestfulfill.tech/devops/demo/handler.go:15.1,16.10 3 0
        """.trimIndent()
        
        val blocks = service.parseCoverageData(rawData)
        val fileCoverages = service.groupByFile(blocks)
        
        assertEquals(2, fileCoverages.size)
        
        // 验证 main.go 的覆盖率
        val mainCoverage = fileCoverages.find { it.filePath.endsWith("main.go") }
        assertNotNull(mainCoverage)
        assertEquals(2, mainCoverage!!.blocks.size)
        assertEquals(3, mainCoverage.totalStatements) // 1 + 2
        assertEquals(3, mainCoverage.coveredStatements) // 全部覆盖
        assertEquals(100.0, mainCoverage.coveragePercentage, 0.01)
        
        // 验证 handler.go 的覆盖率
        val handlerCoverage = fileCoverages.find { it.filePath.endsWith("handler.go") }
        assertNotNull(handlerCoverage)
        assertEquals(1, handlerCoverage!!.blocks.size)
        assertEquals(3, handlerCoverage.totalStatements)
        assertEquals(0, handlerCoverage.coveredStatements)
        assertEquals(0.0, handlerCoverage.coveragePercentage, 0.01)
    }
}
