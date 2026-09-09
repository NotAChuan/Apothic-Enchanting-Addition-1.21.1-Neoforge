package com.chuan.apothicflux.jei;

import com.chuan.apothicflux.integration.productivebees.ProductiveBeesIntegration;
import com.chuan.apothicflux.registry.ModRegistry;
import dev.shadowsoffire.apothic_spawners.ASObjects;
import dev.shadowsoffire.apothic_spawners.modifiers.SpawnerModifier;
import dev.shadowsoffire.apothic_spawners.modifiers.StatModifier;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStat;
import dev.shadowsoffire.apothic_spawners.stats.SpawnerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FluxSpawnerModifierJeiUtil {

    private static final ResourceLocation UPGRADE_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_block");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_2_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_2");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_3_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_3");
    private static final ResourceLocation UPGRADE_PRODUCTIVITY_4_ID =
            ResourceLocation.fromNamespaceAndPath("productivelib", "upgrade_productivity_4");

    private static final Ingredient QUARTZ = Ingredient.of(Items.QUARTZ);
    private static final Ingredient EMPTY = Ingredient.EMPTY;

    private FluxSpawnerModifierJeiUtil() {
    }

    public static List<FluxSpawnerModifierView> createViews(RecipeManager recipeManager) {
        ModifierViewGroups apothicViews = createApothicViews(recipeManager);
        ModifierViewGroups productiveBeesViews = ProductiveBeesIntegration.isLoaded()
                ? createProductiveBeesViews()
                : new ModifierViewGroups(List.of(), List.of());

        List<FluxSpawnerModifierView> views = new ArrayList<>();
        views.addAll(apothicViews.upgrades());
        views.addAll(productiveBeesViews.upgrades());
        views.addAll(apothicViews.downgrades());
        views.addAll(productiveBeesViews.downgrades());
        return List.copyOf(views);
    }

    private static ModifierViewGroups createApothicViews(RecipeManager recipeManager) {
        List<RecipeHolder<SpawnerModifier>> holders = recipeManager
                .getAllRecipesFor(ASObjects.SPAWNER_MODIFIER.get())
                .stream()
                .sorted(Comparator.comparing(holder -> holder.id()))
                .toList();

        List<FluxSpawnerModifierView> upgrades = new ArrayList<>();
        List<FluxSpawnerModifierView> downgrades = new ArrayList<>();

        for (RecipeHolder<SpawnerModifier> holder : holders) {
            toView(holder.value()).ifPresent(view -> {
                if (hasOffhand(view)) {
                    downgrades.add(view);
                } else {
                    upgrades.add(view);
                }
            });
        }

        List<String> upgradeKeys = upgrades.stream()
                .map(view -> ingredientPath(view.mainhand()))
                .toList();

        downgrades.sort(Comparator.comparingInt(view -> {
            String key = ingredientPath(view.mainhand());
            return upgradeKeys.indexOf(key);
        }));

        return new ModifierViewGroups(List.copyOf(upgrades), List.copyOf(downgrades));
    }

    private static String ingredientPath(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stacks[0].getItem());
        return id == null ? "" : id.toString();
    }

    private static boolean hasOffhand(FluxSpawnerModifierView view) {
        return view.hasOffhand();
    }

    private static ModifierViewGroups createProductiveBeesViews() {
        List<FluxSpawnerModifierView> upgrades = new ArrayList<>();
        List<FluxSpawnerModifierView> downgrades = new ArrayList<>();

        if (ModRegistry.BEEHIVE_SIMULATION_UPGRADE != null) {
            Ingredient upgrade = Ingredient.of(ModRegistry.BEEHIVE_SIMULATION_UPGRADE.get());
            upgrades.add(lineView(upgrade, EMPTY, "beehive_install", List.of(
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.beehive_install_tip_1"),
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.beehive_install_tip_2")
            )));
            downgrades.add(lineView(upgrade, QUARTZ, "beehive_uninstall", List.of(
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.beehive_uninstall_tip_1"),
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.beehive_uninstall_tip_2")
            )));
        }

        Ingredient combBlock = itemIngredient(UPGRADE_BLOCK_ID);
        if (!combBlock.isEmpty()) {
            upgrades.add(lineView(combBlock, EMPTY, "comb_block_enable", List.of(
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.comb_block_enable_tip")
            )));
            downgrades.add(lineView(combBlock, QUARTZ, "comb_block_disable", List.of(
                    Component.translatable("jei.apothic_flux.flux_spawner_modifier.comb_block_disable_tip")
            )));
        }

        addProductivityViews(upgrades, downgrades, UPGRADE_PRODUCTIVITY_ID, 20, 40);
        addProductivityViews(upgrades, downgrades, UPGRADE_PRODUCTIVITY_2_ID, 50, 100);
        addProductivityViews(upgrades, downgrades, UPGRADE_PRODUCTIVITY_3_ID, 100, 200);
        addProductivityViews(upgrades, downgrades, UPGRADE_PRODUCTIVITY_4_ID, 160, 320);

        return new ModifierViewGroups(List.copyOf(upgrades), List.copyOf(downgrades));
    }

    private static void addProductivityViews(List<FluxSpawnerModifierView> upgrades,
                                             List<FluxSpawnerModifierView> downgrades,
                                             ResourceLocation itemId, int step, int cap) {
        Ingredient upgrade = itemIngredient(itemId);
        if (upgrade.isEmpty()) {
            return;
        }

        Component installLine = Component.translatable(
                "jei.apothic_flux.flux_spawner_modifier.productivity_install", step, cap);
        Component uninstallLine = Component.translatable(
                "jei.apothic_flux.flux_spawner_modifier.productivity_uninstall", step);

        upgrades.add(new FluxSpawnerModifierView(upgrade, EMPTY, false, false, List.of(
                new FluxSpawnerModifierLine(installLine, List.of(
                        Component.translatable("jei.apothic_flux.flux_spawner_modifier.productivity_install_tip_1", step),
                        Component.translatable("jei.apothic_flux.flux_spawner_modifier.productivity_install_tip_2", cap)
                )))));
        downgrades.add(new FluxSpawnerModifierView(upgrade, QUARTZ, true, false, List.of(
                new FluxSpawnerModifierLine(uninstallLine, List.of(
                        Component.translatable("jei.apothic_flux.flux_spawner_modifier.productivity_uninstall_tip_1", step),
                        Component.translatable("jei.apothic_flux.flux_spawner_modifier.productivity_uninstall_tip_2")
                )))));
    }

    private static FluxSpawnerModifierView lineView(Ingredient mainhand, Ingredient offhand,
                                                    String translationKey, List<Component> tooltip) {
        Component line = Component.translatable("jei.apothic_flux.flux_spawner_modifier." + translationKey);
        return new FluxSpawnerModifierView(mainhand, offhand, offhand != Ingredient.EMPTY, false, List.of(
                new FluxSpawnerModifierLine(line, tooltip)));
    }

    private static Ingredient itemIngredient(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == null ? Ingredient.EMPTY : Ingredient.of(item);
    }

    private record ModifierViewGroups(
            List<FluxSpawnerModifierView> upgrades,
            List<FluxSpawnerModifierView> downgrades
    ) {
    }

    private static Optional<FluxSpawnerModifierView> toView(SpawnerModifier modifier) {
        List<FluxSpawnerModifierLine> lines = new ArrayList<>();
        for (StatModifier<?> statModifier : modifier.getStatModifiers()) {
            if (isSupported(statModifier.stat())) {
                lines.add(createStatLine(statModifier));
            }
        }

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        Ingredient offhand = modifier.getOffhandInput().isEmpty()
                ? Ingredient.EMPTY
                : modifier.getOffhandInput();

        return Optional.of(new FluxSpawnerModifierView(
                modifier.getMainhandInput(),
                offhand,
                offhand != Ingredient.EMPTY,
                modifier.consumesOffhand(),
                List.copyOf(lines)
        ));
    }

    private static boolean isSupported(SpawnerStat<?> stat) {
        return stat == SpawnerStats.MIN_DELAY
                || stat == SpawnerStats.MAX_DELAY
                || stat == SpawnerStats.SPAWN_COUNT
                || stat == SpawnerStats.REDSTONE_CONTROL
                || stat == SpawnerStats.ECHOING;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FluxSpawnerModifierLine createStatLine(StatModifier<?> statModifier) {
        SpawnerStat stat = statModifier.stat();
        Object value = statModifier.value();
        Component display;

        if (stat == SpawnerStats.ECHOING && value instanceof Number number) {
            int echoValue = number.intValue();
            display = Component.translatable(
                    "jei.apothic_flux.flux_spawner_modifier.echoing_value",
                    echoValue > 0 ? "+" + echoValue : String.valueOf(echoValue)
            );
        } else if (value instanceof Number) {
            display = Component.translatable(
                    "jei.apothic_flux.flux_spawner_modifier.stat_value",
                    stat.name(),
                    stat.formatValue(value)
            );
        } else if (value instanceof Boolean bool) {
            display = Component.translatable(
                    bool ? "jei.apothic_flux.flux_spawner_modifier.stat_on"
                            : "jei.apothic_flux.flux_spawner_modifier.stat_off",
                    stat.name()
            );
        } else {
            display = Component.literal(stat.name().getString() + ": " + value);
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stat.name().copy().withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE));
        tooltip.add(stat.desc().copy().withStyle(ChatFormatting.GRAY));

        if (statModifier.min().isPresent()) {
            tooltip.add(Component.translatable(
                    "jei.apothic_flux.flux_spawner_modifier.min_value",
                    stat.name(),
                    stat.formatValue(statModifier.min().get())
            ).withStyle(ChatFormatting.GRAY));
        }

        if (statModifier.max().isPresent()) {
            tooltip.add(Component.translatable(
                    "jei.apothic_flux.flux_spawner_modifier.max_value",
                    stat.name(),
                    stat.formatValue(statModifier.max().get())
            ).withStyle(ChatFormatting.GRAY));
        }

        return new FluxSpawnerModifierLine(display, List.copyOf(tooltip));
    }

}
