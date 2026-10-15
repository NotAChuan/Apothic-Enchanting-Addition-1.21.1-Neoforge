package com.chuan.apothicflux.util;

import dev.shadowsoffire.apothic_enchanting.table.infusion.InfusionRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 自动通量灌注器的候选解析器（客户端与服务端共用）。
 * <p>
 * 读取「当前游戏内所有神化灌注配方」（{@code apothic_enchanting:infusion}），
 * 按输入物匹配收集**全部**候选（包含属性尚未满足的，由 GUI 置灰并禁止选择），
 * 再按位阶升序 → 配方 ID 字典序排序，保证客户端与服务端得到完全一致的顺序。
 * <p>
 * 这里刻意不使用 {@code Ench.RecipeTypes.INFUSION}，而是扫描全部配方做
 * {@code instanceof} 判断：ApothicEnchanting 的编译期版本（build.gradle 里是 1.6.x 系列）
 * 与开发运行期版本（run/mods 里是 1.5.2）并不一致，用 instanceof 可以与版本解耦。
 * 结果按 {@link RecipeManager} 实例弱引用缓存，整合包重载后随实例一同失效。
 */
public final class FluxInfuserCandidates {

    private static final Map<RecipeManager, List<RecipeHolder<?>>> CACHE = new WeakHashMap<>();

    private FluxInfuserCandidates() {
    }

    /**
     * 一个候选：某条灌注配方 + 它当前的产物。
     *
     * @param satisfied 当前属性是否满足该配方的上下限。
     *                  不满足的候选**也会**被返回，由 GUI 置灰且禁止选择——
     *                  这样玩家能看到"这个输入物还有哪些结果、还差多少属性"，
     *                  而不是面对一片空白。
     */
    public record Candidate(ResourceLocation id, InfusionRecipe recipe, ItemStack output, float eterna,
                            boolean satisfied) {
    }

    /**
     * 解析全部候选。
     *
     * @param input  中间输入槽的物品
     * @param eterna 机器采集到的位阶（tableEterna，不含玩家加成）
     */
    public static List<Candidate> resolve(Level level, ItemStack input, float eterna, float quanta, float arcana) {
        return resolve(level, input, eterna, quanta, arcana, false);
    }

    public static List<Candidate> resolve(Level level, ItemStack input, float eterna, float quanta, float arcana,
                                          boolean ignoreRequirements) {
        if (level == null || input.isEmpty()) {
            return List.of();
        }

        List<Candidate> candidates = new ArrayList<>();
        for (RecipeHolder<?> holder : infusionRecipes(level)) {
            if (!(holder.value() instanceof InfusionRecipe recipe)) {
                continue;
            }
            if (!recipe.getInput().test(input)) {
                continue;
            }
            float recipeEterna = ignoreRequirements ? recipe.getRequirements().eterna() : eterna;
            float recipeQuanta = ignoreRequirements ? recipe.getRequirements().quanta() : quanta;
            float recipeArcana = ignoreRequirements ? recipe.getRequirements().arcana() : arcana;
            ItemStack output = recipe.assemble(input, recipeEterna, recipeQuanta, recipeArcana);
            if (output.isEmpty()) {
                continue;
            }
            // 属性不满足的配方也一并返回（satisfied = false），由 GUI 置灰并禁止点击
            boolean satisfied = ignoreRequirements || satisfies(recipe, eterna, quanta, arcana);
            candidates.add(new Candidate(holder.id(), recipe, output, recipe.getRequirements().eterna(), satisfied));
        }

        // 门槛低的在前；同门槛按配方 ID 字典序兜底。
        // 顺序只取决于配方本身、与当前属性无关，所以调整书架时列表不会跳动，
        // 玩家也能稳定地看到"某条结果被锁在哪一档"。
        candidates.sort(Comparator.comparingDouble(Candidate::eterna)
                .thenComparing(candidate -> candidate.id().toString()));
        return candidates;
    }

    /** 该物品能否匹配到至少一条灌注配方（忽略属性），用于输入槽过滤。 */
    public static boolean hasAnyRecipe(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<?> holder : infusionRecipes(level)) {
            if (holder.value() instanceof InfusionRecipe recipe && recipe.getInput().test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 给定的属性是否满足配方的下限与上限。
     * <p>
     * ⚠️ 神化在 {@code max_requirements} 里用 <b>负数（-1）表示「该项不限制上限」</b>，例如：
     * <pre>
     * xp_bottle.json : "max_requirements": { "eterna": -1, "quanta": -1, "arcana": -1 }
     * carrot.json    : "max_requirements": { "eterna": 20, "quanta": 30, "arcana": -1 }
     * </pre>
     * 因此必须把负数当成"无上限"，否则 {@code 100 > -1} 成立会把所有带 -1 的配方全部误杀。
     * {@code max_requirements} 缺失时默认值同样是 -1（部分配方如 echo_shard.json / deepshelf.json 不写该项）。
     */
    public static boolean satisfies(InfusionRecipe recipe, float eterna, float quanta, float arcana) {
        var requirements = recipe.getRequirements();
        if (eterna < requirements.eterna() || quanta < requirements.quanta() || arcana < requirements.arcana()) {
            return false;
        }
        var maxRequirements = recipe.getMaxRequirements();
        if (maxRequirements == null) {
            return true;
        }
        return isWithinMax(eterna, maxRequirements.eterna())
                && isWithinMax(quanta, maxRequirements.quanta())
                && isWithinMax(arcana, maxRequirements.arcana());
    }

    /** {@code max_requirements} 中负数表示「不限制上限」。 */
    private static boolean isWithinMax(float value, float max) {
        return max < 0.0F || value <= max;
    }

    /**
     * 只解析指定 ID 的那一条候选，并校验它当前是否仍然合法。
     * 用于机器 tick 校验与"点击选择"的服务端校验，
     * 避免每 tick 为全部候选都调用一次 assemble 构造产物。
     */
    @Nullable
    public static Candidate resolveById(Level level, ItemStack input, float eterna, float quanta, float arcana,
                                        @Nullable ResourceLocation id) {
        return resolveById(level, input, eterna, quanta, arcana, id, false);
    }

    @Nullable
    public static Candidate resolveById(Level level, ItemStack input, float eterna, float quanta, float arcana,
                                        @Nullable ResourceLocation id, boolean ignoreRequirements) {
        if (level == null || input.isEmpty() || id == null) {
            return null;
        }
        for (RecipeHolder<?> holder : infusionRecipes(level)) {
            if (!holder.id().equals(id)) {
                continue;
            }
            if (!(holder.value() instanceof InfusionRecipe recipe)) {
                return null;
            }
            if (!recipe.getInput().test(input)) {
                return null;
            }
            if (!ignoreRequirements && !satisfies(recipe, eterna, quanta, arcana)) {
                return null;
            }
            float recipeEterna = ignoreRequirements ? recipe.getRequirements().eterna() : eterna;
            float recipeQuanta = ignoreRequirements ? recipe.getRequirements().quanta() : quanta;
            float recipeArcana = ignoreRequirements ? recipe.getRequirements().arcana() : arcana;
            ItemStack output = recipe.assemble(input, recipeEterna, recipeQuanta, recipeArcana);
            if (output.isEmpty()) {
                return null;
            }
            // 能走到这里说明属性已满足，satisfied 恒为 true
            return new Candidate(holder.id(), recipe, output, recipe.getRequirements().eterna(), true);
        }
        return null;
    }

    private static List<RecipeHolder<?>> infusionRecipes(Level level) {
        RecipeManager manager = level.getRecipeManager();
        List<RecipeHolder<?>> cached = CACHE.get(manager);
        if (cached != null) {
            return cached;
        }
        List<RecipeHolder<?>> found = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (holder.value() instanceof InfusionRecipe) {
                found.add(holder);
            }
        }
        List<RecipeHolder<?>> result = List.copyOf(found);
        CACHE.put(manager, result);
        return result;
    }

    /** 供数据包重载等场景手动失效缓存。 */
    public static void invalidate() {
        CACHE.clear();
    }
}
