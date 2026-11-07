package dev.toaru.kazakiri;

import com.mojang.logging.LogUtils;
import dev.toaru.kazakiri.net.Net;
import dev.toaru.kazakiri.skills.AimConvergence;
import dev.toaru.kazakiri.skills.AimRevelation;
import dev.toaru.kazakiri.skills.AngelsDescent;
import dev.toaru.kazakiri.skills.ArchangelProtocol;
import dev.toaru.kazakiri.skills.CelestialSpear;
import dev.toaru.kazakiri.skills.HeavensRejection;
import dev.toaru.kazakiri.skills.Sanctuary;
import dev.toaru.kazakiri.skills.SeraphicArray;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.SkillRequirement;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Kazakiri - AIM Diffusion Field: an add-on category for Re:AcademyCraft 1.5.0, registered through the public API only.
 * AIM Archangel moveset. Tree: Descent(1) -> Spear(2) -> Seraphic Array(3) -> Archangel Protocol(4) -> AIM Convergence(5);
 * Revelation(1) -> Sanctuary(2) -> Heaven's Rejection(3) -> (Protocol).
 */
@Mod(KazakiriMod.MOD_ID)
public final class KazakiriMod {
    public static final String MOD_ID = "kazakiri";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    /** Gives the category in the Ability Developer, like the built-in factors. */
    public static final RegistryObject<Item> AIM_CRYSTAL = ITEMS.register("aim_crystal", () -> new Item(new Item.Properties().stacksTo(16)));

    public static AbilityId id(String path) { return AbilityId.parse(MOD_ID + ":" + path); }
    public static final AbilityId CATEGORY = id("aim_diffusion"),
            ANGELS_DESCENT = id("angels_descent"),
            AIM_REVELATION = id("aim_revelation"),
            CELESTIAL_SPEAR = id("celestial_spear"),
            SANCTUARY = id("sanctuary"),
            SERAPHIC_ARRAY = id("seraphic_array"),
            HEAVENS_REJECTION = id("heavens_rejection"),
            ARCHANGEL_PROTOCOL = id("archangel_protocol"),
            AIM_CONVERGENCE = id("aim_convergence");

    public KazakiriMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Cfg.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientCfg.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) { event.enqueueWork(Net::register); }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(AIM_CRYSTAL);
    }

    private static Presentation shown(AbilityId skill) {
        return new Presentation(MOD_ID + ".skill." + skill.path(), MOD_ID + ".skill." + skill.path() + ".desc", null);
    }

    private static SkillDefinition skill(AbilityId id, int level, List<SkillRequirement> requires, ActivationKind kind,
                                         float x, float y, @Nullable AbilityId parent) {
        return new SkillDefinition(id, CATEGORY, level, requires, kind, shown(id), new TreePosition(x, y, parent));
    }

    private static SkillRequirement needs(AbilityId skill, float proficiency) { return new SkillRequirement(skill, proficiency); }

    /** Re:AcademyCraft fires this once during common setup; registration is only possible here. */
    private void registerAbilities(RegisterAbilitiesEvent event) {
        var r = event.registrar(MOD_ID);
        // Skills per level 1..5: 2, 2, 2, 1, 1 (= 8).
        r.category(new CategoryDefinition(CATEGORY, id("aim_crystal"), List.of(2, 2, 2, 1, 1),
                new Presentation(MOD_ID + ".ability.aim_diffusion", null, null)));

        r.skill(skill(ANGELS_DESCENT, 1, List.of(), ActivationKind.TOGGLE, 15, 22, null));
        r.skill(skill(AIM_REVELATION, 1, List.of(), ActivationKind.TOGGLE, 15, 95, null));
        r.skill(skill(CELESTIAL_SPEAR, 2, List.of(needs(ANGELS_DESCENT, .3f)), ActivationKind.TOGGLE, 65, 22, ANGELS_DESCENT));
        r.skill(skill(SANCTUARY, 2, List.of(needs(AIM_REVELATION, .3f)), ActivationKind.TOGGLE, 65, 95, AIM_REVELATION));
        r.skill(skill(SERAPHIC_ARRAY, 3, List.of(needs(CELESTIAL_SPEAR, .4f)), ActivationKind.TOGGLE, 115, 22, CELESTIAL_SPEAR));
        r.skill(skill(HEAVENS_REJECTION, 3, List.of(needs(SANCTUARY, .4f)), ActivationKind.TOGGLE, 115, 95, SANCTUARY));
        r.skill(skill(ARCHANGEL_PROTOCOL, 4, List.of(needs(SERAPHIC_ARRAY, .5f), needs(HEAVENS_REJECTION, .3f)), ActivationKind.TOGGLE, 170, 58, SERAPHIC_ARRAY));
        r.skill(skill(AIM_CONVERGENCE, 5, List.of(needs(ARCHANGEL_PROTOCOL, .6f)), ActivationKind.TOGGLE, 222, 58, ARCHANGEL_PROTOCOL));

        var b = event.behaviors(MOD_ID);
        b.behavior(ANGELS_DESCENT, new AngelsDescent());
        b.behavior(AIM_REVELATION, new AimRevelation());
        b.behavior(CELESTIAL_SPEAR, new CelestialSpear());
        b.behavior(SANCTUARY, new Sanctuary());
        b.behavior(SERAPHIC_ARRAY, new SeraphicArray());
        b.behavior(HEAVENS_REJECTION, new HeavensRejection());
        b.behavior(ARCHANGEL_PROTOCOL, new ArchangelProtocol());
        b.behavior(AIM_CONVERGENCE, new AimConvergence());
    }
}
