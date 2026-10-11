package com.xingling.scenerystaff.blade;

import com.xingling.scenerystaff.SceneryStaff;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组全部注册名的<b>唯一来源</b>。
 * <p>
 * 这里只放 {@link ResourceLocation} 与路径字符串常量，<b>不引用任何 SlashBlade 定义类</b>。
 * 这是刻意的：{@code SceneryStaffBladeRegistry}（datagen 的刀定义表）的静态初始化会牵出
 * {@code SlashBladeDefinition} → {@code PropertiesDefinition} → {@code ComboStateRegistry}，
 * 进而要求 Minecraft 注册表已经 bootstrap；若直接共用定义表的 {@code ResourceKey}，
 * 凡是用到这些名字的地方（刀识别、事件、配方、注册表）都会被那条初始化链拖住。
 * <p>
 * <b>为什么需要这个类</b>：这些名字原先靠字符串在多个文件里人工同步——
 * 刀 ID 分布在刀定义表（每个刀两处：{@code ResourceKey} 与定义里的 {@code name}）、
 * 识别类、配方生成器三处；{@code erosion_domain} / {@code giant_sword} 更是被
 * SA 注册表、连击状态注册表、实体注册表各写一遍。
 * 改名时编译器不会提示漏改，只会安静地产生两个不同的注册项。集中到这里后改名只需动一处。
 */
public final class SceneryIds {

    // ------------------------------------------------------------------ 命名刀

    /** 布景之杖线的上位刀「布景之杖」 */
    public static final ResourceLocation SCENERY_STAFF = SceneryStaff.prefix("scenery_staff");
    /** 布景之杖线的下位刀「残景之杖」 */
    public static final ResourceLocation SCENERY_STAFF_WORN = SceneryStaff.prefix("scenery_staff_worn");
    /** 启明星线的上位刀「永远的启明星」 */
    public static final ResourceLocation QIMINGXING = SceneryStaff.prefix("qimingxing");
    /** 启明星线的下位刀「启明星」 */
    public static final ResourceLocation QIMINGXING_WORN = SceneryStaff.prefix("qimingxing_worn");
    /** 基础刀「潮汐」 */
    public static final ResourceLocation CHAOXI = SceneryStaff.prefix("chaoxi");

    // ------------------------------------------------------------------ 剑技 / 连击状态 / 实体
    // 同一个名字要同时注册进这三类注册表，因此路径与完整 ID 一并提供。

    /** 剑技「蚀域」（同时是连击状态 id 与实体注册名） */
    public static final String EROSION_DOMAIN_PATH = "erosion_domain";
    public static final ResourceLocation EROSION_DOMAIN = SceneryStaff.prefix(EROSION_DOMAIN_PATH);

    /** 剑技「永远的启明星」（同时是连击状态 id 与实体注册名） */
    public static final String GIANT_SWORD_PATH = "giant_sword";
    public static final ResourceLocation GIANT_SWORD = SceneryStaff.prefix(GIANT_SWORD_PATH);

    private SceneryIds() {
    }
}
