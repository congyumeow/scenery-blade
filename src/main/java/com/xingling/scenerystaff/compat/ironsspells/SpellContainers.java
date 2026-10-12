package com.xingling.scenerystaff.compat.ironsspells;

import com.xingling.scenerystaff.SceneryStaff;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * 铁魔法「法术容器」在 1.20.1 上的两代 API 差异，以及运行期自适应。
 *
 * <h2>为什么必须做自适应</h2>
 * 铁魔法在 1.20.1 上迭代出两种<b>互不兼容</b>的容器 API，而玩家完全可以装其中任意一代
 * （实测同一份 mods 目录里 3.4.0.9 与 3.16.3 同时存在）。两代差别：
 *
 * <table border="1">
 *   <caption>两代容器 API 对照</caption>
 *   <tr><th></th><th>旧（≤3.5.x，如 3.4.0.9）</th><th>新（≥3.6.x，如 3.16.3）</th></tr>
 *   <tr><td>可变容器类型</td><td>没有 {@code ISpellContainerMutable}</td><td>有</td></tr>
 *   <tr><td>增删法术</td><td>{@code ISpellContainer} 上的实例方法，
 *       要额外传 {@code ItemStack}，并靠 {@code save(stack)} 落盘</td>
 *       <td>{@code ISpellContainerMutable} 上的方法，不需要 ItemStack</td></tr>
 *   <tr><td>写入物品</td><td>实例 {@code save(stack)}</td>
 *       <td>静态 {@code ISpellContainer.set(stack, c)}</td></tr>
 *   <tr><td>槽位类型</td><td>{@code SpellData}（带 {@code getSpell/getLevel/isLocked}）</td>
 *       <td>{@code SpellSlot}（访问器同名，但是另一个类）</td></tr>
 * </table>
 *
 * 因此「用新一代 API 写、旧版本跑」会在运行期 {@code NoSuchMethodError}（实测：每秒巡检背包时崩）。
 * 反过来「用旧一代写、新版本跑」同样会崩。而<b>一个编译产物不可能同时静态引用两代符号</b> ——
 * 引用 {@code ISpellContainerMutable} 就编译不过旧版本，引用旧版实例方法就编译不过新版本。
 * 唯一出路是：用最保守的那一代做静态编译，把版本敏感的部分收进反射，运行期探测一次后固定下来。
 *
 * <h2>静态编译目标</h2>
 * 只静态依赖两代都有的符号：{@code ISpellContainer.isSpellContainer / get / mustEquip /
 * getMaxSpellCount} 与 {@code AbstractSpell.getSpellResource()}。
 * 槽位内容、增删法术、写入物品全部走反射。
 *
 * <p>反射只发生在「每秒一次」的巡检与真正写入法术时，不在热路径上。
 * 本类只在铁魔法确实存在时才会被加载（见 {@link IronsSpellsCompat#register()}）。
 */
final class SpellContainers {

    /** 新 API 的标志类型：存在即说明是新一代 */
    private static final String MUTABLE_CONTAINER = "io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable";

    /** 一个法术槽的内容（已经从两代各自的槽位类型里取出来，调用方不必关心是哪个类） */
    record SlotData(AbstractSpell spell, int level, boolean locked) {
    }

    /** 两代共同操作的抽象 */
    interface Ops {

        /** 这个容器里有没有法术（<b>不</b>用 isEmpty()，原因见实现） */
        boolean hasSpell(ISpellContainer container);

        /**
         * 取第一个有法术的槽。
         *
         * @return 没有法术时返回 {@code null}
         */
        SlotData first(ISpellContainer container);

        /**
         * 按法术 ID 找槽。
         *
         * @return 找到返回槽内容，否则 {@code null}
         */
        SlotData find(ISpellContainer container, ResourceLocation spellId);

        /**
         * 用全新的单格容器写入门派法术。
         * <p>
         * {@code create(maxSpells, spellWheel, mustEquip)} 的两个布尔极易写反：
         * {@code spellWheel=true} 才进法术轮盘；{@code mustEquip} 必须为
         * {@code false}（传 true 等于「只在护甲/饰品栏生效」，刀拿在手上反而不进轮盘）。
         */
        void write(ItemStack stack, AbstractSpell spell, int level, boolean locked);
    }

    private static final Ops OPS = detect();

    private SpellContainers() {
    }

    static Ops ops() {
        return OPS;
    }

    private static Ops detect() {
        try {
            Class.forName(MUTABLE_CONTAINER);
            return new ModernOps();
        } catch (ClassNotFoundException e) {
            return new LegacyOps();
        }
    }

    // ------------------------------------------------------------------ 工具

    /** 取第一个非空槽；没有法术时返回 null */
    private static SlotData firstSpell(ISpellContainer container) {
        for (int i = 0; i < container.getMaxSpellCount(); i++) {
            SlotData data = readSlot(container, i);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    /**
     * 读取单个槽位，兼容两代槽位类型。
     * <p>
     * 旧版返回 {@code SpellData}、新版返回 {@code SpellSlot}，但两者的访问器同名
     * （{@code getSpell / getLevel / isLocked}），所以这里按名字调用；空槽在两代里
     * 都表现为 {@code getSpell()} 返回空法术，这里判断成 null。
     */
    private static SlotData readSlot(ISpellContainer container, int index) {
        try {
            Object slot = container.getSpellAtIndex(index);
            if (slot == null) {
                return null;
            }
            Method getSpell = slot.getClass().getMethod("getSpell");
            Object spell = getSpell.invoke(slot);
            if (!(spell instanceof AbstractSpell abstractSpell) || abstractSpell.getSpellResource() == null) {
                return null;
            }
            int level = (Integer) slot.getClass().getMethod("getLevel").invoke(slot);
            boolean locked = (Boolean) slot.getClass().getMethod("isLocked").invoke(slot);
            return new SlotData(abstractSpell, level, locked);
        } catch (ReflectiveOperationException e) {
            SceneryStaff.LOGGER.warn("[铁魔法联动] 读取法术槽失败（index={}），跳过该槽", index, e);
            return null;
        }
    }

    /** 按法术 ID 在容器里找槽（两代通用） */
    private static SlotData findSpell(ISpellContainer container, ResourceLocation spellId) {
        for (int i = 0; i < container.getMaxSpellCount(); i++) {
            SlotData data = readSlot(container, i);
            if (data != null && spellId.equals(data.spell().getSpellResource())) {
                return data;
            }
        }
        return null;
    }

    /** 新版把容器落地到物品的静态入口 */
    private static void callStaticSet(ItemStack stack, Object container) {
        try {
            Method set = ISpellContainer.class.getMethod("set", ItemStack.class, ISpellContainer.class);
            set.invoke(null, stack, container);
        } catch (ReflectiveOperationException e) {
            SceneryStaff.LOGGER.error("[铁魔法联动] 写入法术容器失败（ISpellContainer.set）", e);
        }
    }

    // ------------------------------------------------------------------ 新 API（≥3.6）

    private static final class ModernOps implements Ops {

        @Override
        public boolean hasSpell(ISpellContainer container) {
            return firstSpell(container) != null;
        }

        @Override
        public SlotData first(ISpellContainer container) {
            return firstSpell(container);
        }

        @Override
        public SlotData find(ISpellContainer container, ResourceLocation spellId) {
            return findSpell(container, spellId);
        }

        @Override
        public void write(ItemStack stack, AbstractSpell spell, int level, boolean locked) {
            try {
                Object container = ISpellContainer.create(1, true, false);
                Method mutableCopy = container.getClass().getMethod("mutableCopy");
                Object mutable = mutableCopy.invoke(container);
                Method add = mutable.getClass().getMethod("addSpellAtIndex",
                        AbstractSpell.class, int.class, int.class, boolean.class);
                add.invoke(mutable, spell, level, 0, locked);
                Object immutable = mutable.getClass().getMethod("toImmutable").invoke(mutable);
                callStaticSet(stack, immutable);
            } catch (ReflectiveOperationException e) {
                SceneryStaff.LOGGER.error("[铁魔法联动] 写入法术容器失败（新一代 API）", e);
            }
        }
    }

    // ------------------------------------------------------------------ 旧 API（≤3.5）

    private static final class LegacyOps implements Ops {

        @Override
        public boolean hasSpell(ISpellContainer container) {
            // 刻意不用 isEmpty()：旧版 create(...) 造出的容器 activeSpellCount 恒为 0，
            // 用它就会永远判定为空、每秒重写一次，把玩家在奥术铁砧上改好的法术冲掉。
            return firstSpell(container) != null;
        }

        @Override
        public SlotData first(ISpellContainer container) {
            return firstSpell(container);
        }

        @Override
        public SlotData find(ISpellContainer container, ResourceLocation spellId) {
            return findSpell(container, spellId);
        }

        @Override
        public void write(ItemStack stack, AbstractSpell spell, int level, boolean locked) {
            try {
                // 旧版必须把目标物品栈一并传进去，再显式 save 才会落到 NBT
                Object container = ISpellContainer.create(1, true, false);
                Method add = container.getClass().getMethod("addSpellAtIndex",
                        AbstractSpell.class, int.class, int.class, boolean.class, ItemStack.class);
                add.invoke(container, spell, level, 0, locked, stack);
                container.getClass().getMethod("save", ItemStack.class).invoke(container, stack);
            } catch (ReflectiveOperationException e) {
                SceneryStaff.LOGGER.error("[铁魔法联动] 写入法术容器失败（旧一代 API）", e);
            }
        }
    }
}
