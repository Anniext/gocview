package com.github.anniext.gocview.model

/**
 * 覆盖率数据模型
 *
 * @property filePath 文件路径
 * @property startLine 起始行
 * @property startCol 起始列
 * @property endLine 结束行
 * @property endCol 结束列
 * @property numStatements 该基本块中的语句数量
 * @property executionCount 该基本块被执行到的次数
 */
data class CoverageBlock(
    val filePath: String,
    val startLine: Int,
    val startCol: Int,
    val endLine: Int,
    val endCol: Int,
    val numStatements: Int,
    val executionCount: Int
) {
    /**
     * 是否被覆盖（执行次数大于0）
     */
    val isCovered: Boolean
        get() = executionCount > 0
    
    /**
     * 覆盖率百分比（0-100）
     */
    val coveragePercentage: Double
        get() = if (isCovered) 100.0 else 0.0
}

/**
 * 文件覆盖率统计
 *
 * @property filePath 文件路径
 * @property blocks 覆盖率块列表
 */
data class FileCoverage(
    val filePath: String,
    val blocks: List<CoverageBlock>
) {
    /**
     * 总语句数
     */
    val totalStatements: Int
        get() = blocks.sumOf { it.numStatements }
    
    /**
     * 已覆盖语句数
     */
    val coveredStatements: Int
        get() = blocks.filter { it.isCovered }.sumOf { it.numStatements }
    
    /**
     * 覆盖率百分比
     */
    val coveragePercentage: Double
        get() = if (totalStatements > 0) {
            (coveredStatements.toDouble() / totalStatements) * 100
        } else {
            0.0
        }
}
