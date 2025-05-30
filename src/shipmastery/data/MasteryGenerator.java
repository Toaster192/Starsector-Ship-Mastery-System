package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.impl.random.RandomMastery;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.Set;

public class MasteryGenerator {
    public final Class<? extends MasteryEffect> effectClass;
    public String[] params;
    public final Set<String> tags;
    public final float defaultStrength;
    public final int priority;

    public MasteryGenerator(
            MasteryInfo info,
            String[] params) {
        effectClass = info.effectClass;
        this.params = params;
        tags = info.tags;
        priority = info.priority;
        defaultStrength = info.defaultStrength;
        if (this.params == null || this.params.length == 0) {
            this.params = new String[] {"" + defaultStrength};
        }
    }

    public MasteryEffect generateDontInit(ShipHullSpecAPI spec, int level, int index, String optionId)
            throws IllegalAccessException, NoSuchMethodException {
        BaseMasteryEffect effect = (BaseMasteryEffect) Utils.instantiateClassNoParams(effectClass);
        effect.setOptionId(optionId);
        effect.setPriority(priority);
        effect.setHullSpec(spec);
        effect.setId(MasteryUtils.makeEffectId(effect, level, index));
        effect.setLevel(level);
        effect.setIndex(index);
        effect.setTags(tags);
        return effect;
    }

    public static MasteryEffect copyEffect(Class<?> effectClass, MasteryEffect other, String... args)
            throws IllegalAccessException, NoSuchMethodException {
        BaseMasteryEffect effect = (BaseMasteryEffect) Utils.instantiateClassNoParams(effectClass);
        effect.setOptionId(other.getOptionId());
        effect.setPriority(other.getPriority());
        effect.setHullSpec(other.getHullSpec());
        effect.setId(MasteryUtils.makeEffectId(effect, other.getLevel(), other.getIndex()));
        effect.setLevel(other.getLevel());
        effect.setIndex(other.getIndex());
        effect.setTags(other.getTags());
        return effect.init(args);
    }

    public MasteryEffect generate(
            ShipHullSpecAPI spec,
            int level,
            int index,
            String optionId,
            int seedPrefix,
            Set<Class<?>> avoidWhenGeneratingRandom,
            Set<String> paramsToAvoidWhenGenerating) throws InstantiationException, IllegalAccessException, NoSuchMethodException {
        MasteryEffect effect = generateDontInit(spec, level, index, optionId);

        if (effect instanceof RandomMastery) {
            ((RandomMastery) effect).setSeedPrefix(seedPrefix);
            ((RandomMastery) effect).setAvoidWhenGenerating(avoidWhenGeneratingRandom);
            ((RandomMastery) effect).setParamsToAvoidWhenGenerating(paramsToAvoidWhenGenerating);
        }

        return effect.init(params);
    }
}
