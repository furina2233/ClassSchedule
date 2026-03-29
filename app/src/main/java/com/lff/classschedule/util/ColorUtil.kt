package com.lff.classschedule.util

import androidx.core.graphics.toColorInt
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

object ColorUtil {
    private var colorListTemplate = listOf(
        "#E9C980".toColorInt(), // 姜黄
        "#B0CC9C".toColorInt(), // 嫩绿
        "#9DA5B8".toColorInt(), // 灰蓝
        "#8096C5".toColorInt(), // 靛蓝
        "#D4D0CF".toColorInt(), // 浅灰
        "#81AFEB".toColorInt(), // 天蓝
        "#F0AEA0".toColorInt(), // 珊瑚粉
        "#4171C7".toColorInt(), // 深蓝
        "#B8A6D9".toColorInt(), // 薰衣草紫
        "#85C1C0".toColorInt(), // 薄荷绿
        "#F2C280".toColorInt(), // 浅橙色
        "#A8D8EA".toColorInt(), // 冰蓝色
        "#D9A5B3".toColorInt(), // 藕粉色
        "#96C7B7".toColorInt(), // 灰绿色
        "#EAC4D5".toColorInt(), // 浅紫粉
        "#C8D1D3".toColorInt()  // 冷灰色
    )

    // 打乱后的列表模板
    private var shuffledTemplate: List<Int> = emptyList()

    // 克隆列表，避免修改模板列表
    private var colorList: MutableList<Int>

    init {
        // 打乱规则：以学年年份为种子，如果当前处于1到7月，视为上一学年
        val now = LocalDate.now()
        val year = if (now.monthValue in 1..7) now.year - 1 else now.year
        shuffledTemplate = colorListTemplate.shuffled(Random(year))
        colorList = colorListTemplate.toMutableList()
    }

    private fun checkEmpty(){
        if (colorList.isEmpty()){
            colorList.addAll(shuffledTemplate)
        }
    }

    fun resetColorPool() {
        colorList.clear()
        colorList.addAll(shuffledTemplate)
    }

    fun getColor(name: String): Int {
        checkEmpty()
        val hash = name.hashCode() and 0x7FFFFFFF  // 如果是负数，去掉负号
        val targetIndex = hash % colorList.size
        return colorList.removeAt(targetIndex)  // 使用线性探测法处理冲突
    }

}