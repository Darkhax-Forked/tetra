package se.mickelus.tetra.module;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.ArrayUtils;
import se.mickelus.tetra.NBTHelper;
import se.mickelus.tetra.capabilities.Capability;
import se.mickelus.tetra.capabilities.ICapabilityProvider;
import se.mickelus.tetra.module.data.ModuleData;
import se.mickelus.tetra.module.data.ModuleModel;
import se.mickelus.tetra.module.data.TweakData;
import se.mickelus.tetra.module.schema.Material;
import se.mickelus.tetra.module.schema.RepairDefinition;

public abstract class ItemModule implements ICapabilityProvider {

    protected ModuleData[] data = new ModuleData[0];

    protected TweakData[] tweaks = new TweakData[0];

    protected String slotKey;
    protected String moduleKey;
    protected String dataKey;

    protected Priority renderLayer = Priority.BASE;

    public ItemModule(String slotKey, String moduleKey) {
        this.slotKey = slotKey;
        this.moduleKey = moduleKey;
        this.dataKey = moduleKey + "_material";
    }

    public ItemModule withRenderLayer(Priority layer) {
        this.renderLayer = layer;
        return this;
    }

    public String getKey() {
        return moduleKey;
    }

    public String getUnlocalizedName() {
        return moduleKey;
    }

    public String getSlot() {
        return slotKey;
    }

    public void addModule(ItemStack targetStack, String variantKey, PlayerEntity player) {
        CompoundNBT tag = NBTHelper.getTag(targetStack);

        tag.putString(slotKey, moduleKey);
        tag.putString(dataKey, variantKey);
    }

    public ItemStack[] removeModule(ItemStack targetStack) {
        CompoundNBT tag = NBTHelper.getTag(targetStack);

        tag.remove(slotKey);
        tag.remove(dataKey);

        return new ItemStack[0];
    }

    public void postRemove(ItemStack targetStack, PlayerEntity player) {

    }

    public ModuleData getData(ItemStack itemStack) {
        CompoundNBT tag = NBTHelper.getTag(itemStack);
        String dataName = tag.getString(this.dataKey);

        return getData(dataName);
    }

    public ModuleData getData(String variantKey) {
        return Arrays.stream(data)
                .filter(moduleData -> moduleData.key.equals(variantKey))
                .findAny().orElseGet(this::getDefaultData);
    }

    public ModuleData getDefaultData() {
        return data.length > 0 ? data[0] : new ModuleData();
    }

    public String getName(ItemStack itemStack) {
        return I18n.format(getData(itemStack).key);
    }

    public String getDescription(ItemStack itemStack) {
        String dataKey = getData(itemStack).key;
        if (I18n.hasKey(dataKey + ".description")) {
            return I18n.format(dataKey + ".description");
        }
        return I18n.format(getUnlocalizedName() + ".description");
    }

    public String getItemName(ItemStack itemStack) {
        String name = getData(itemStack).key + ".name";
        if (I18n.hasKey(name)) {
            return I18n.format(name);
        } else if (I18n.hasKey(getUnlocalizedName() + ".item_name")) {
            return I18n.format(getUnlocalizedName() + ".item_name");
        }
        return null;
    }

    public Priority getItemNamePriority(ItemStack itemStack) {
        return Priority.BASE;
    }

    public String getItemPrefix(ItemStack itemStack) {
        String prefix = getData(itemStack).key + ".prefix";
        if (I18n.hasKey(prefix)) {
            return I18n.format(prefix);
        } else if (I18n.hasKey(getUnlocalizedName() + ".prefix")) {
            return I18n.format(getUnlocalizedName() + ".prefix");
        }
        return null;
    }

    public Priority getItemPrefixPriority(ItemStack itemStack) {
        return Priority.BASE;
    }


    /**
     * Returns the integrity gained from this module. Split into two methods as modules with improvements may have
     * internal gains/costs which should be visible.
     *
     * @param itemStack An itemstack containing module data for this module
     * @return Integrity gained from this module, excluding internal costs
     */
    public int getIntegrityGain(ItemStack itemStack) {
        int integrity = getData(itemStack).integrity;
        if (integrity > 0 ) {
            return integrity;
        }
        return 0;
    }

    /**
     * Returns the integrity cost of this module. Split into two methods as modules with improvements may have
     * internal gains/costs which should be visible.
     *
     * @param itemStack An itemstack containing module data for this module
     * @return Integrity cost of this module, excluding internal gains
     */
    public int getIntegrityCost(ItemStack itemStack) {
        int integrity = getData(itemStack).integrity;
        if (integrity < 0 ) {
            return integrity;
        }
        return 0;
    }

    public int getMagicCapacity(ItemStack itemStack) {
        return getMagicCapacityGain(itemStack) - getMagicCapacityCost(itemStack);
    }

    public int getMagicCapacityGain(ItemStack itemStack) {
        int magicCapacity = getData(itemStack).magicCapacity;
        if (magicCapacity > 0 ) {
            return magicCapacity;
        }
        return 0;
    }

    public int getMagicCapacityCost(ItemStack itemStack) {
        int magicCapacity = getData(itemStack).magicCapacity;
        if (magicCapacity < 0 ) {
            return -magicCapacity;
        }
        return 0;
    }

    public int getDurability(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .mapToInt(tweak -> tweak.getDurability(getTweakStep(itemStack, tweak)))
                .sum() + getData(itemStack).durability;
    }

    public float getDurabilityMultiplier(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .map(tweak -> tweak.getDurabilityMultiplier(getTweakStep(itemStack, tweak)))
                .reduce(getData(itemStack).durabilityMultiplier, (a, b) -> a * b);
    }

    public Material getRepairMaterial(ItemStack itemStack) {
        RepairDefinition definition = ItemUpgradeRegistry.instance.getRepairDefinition(getData(itemStack).key);
        if (definition != null) {
            return definition.material;
        }
        return null;
    }

    public int getRepairAmount(ItemStack itemStack) {
        return getData(itemStack).durability;
    }

    public Collection<Capability> getRepairRequiredCapabilities(ItemStack itemStack) {
        RepairDefinition definition = ItemUpgradeRegistry.instance.getRepairDefinition(getData(itemStack).key);
        if (definition != null) {
            return definition.requiredCapabilities.getValues();
        }
        return Collections.emptyList();
    }

    public int getRepairRequiredCapabilityLevel(ItemStack itemStack, Capability capability) {
        RepairDefinition definition = ItemUpgradeRegistry.instance.getRepairDefinition(getData(itemStack).key);
        if (definition != null) {
            return definition.requiredCapabilities.getLevel(capability);
        }
        return 0;
    }

    public boolean isTweakable(ItemStack itemStack) {
        String variant = NBTHelper.getTag(itemStack).getString(this.dataKey);
        return Arrays.stream(tweaks)
                .anyMatch(data -> variant.equals(data.variant));
    }

    public TweakData[] getTweaks(ItemStack itemStack) {
        CompoundNBT tag = NBTHelper.getTag(itemStack);
        String variant = tag.getString(this.dataKey);
        return Arrays.stream(tweaks)
                .filter(tweak -> variant.equals(tweak.variant))
                .toArray(TweakData[]::new);
    }

    public boolean hasTweak(ItemStack itemStack, String tweakKey) {
        return Arrays.stream(getTweaks(itemStack))
                .map(tweak -> tweak.key)
                .anyMatch(tweakKey::equals);
    }

    public int getTweakStep(ItemStack itemStack, TweakData tweak) {
        return Math.max(Math.min(NBTHelper.getTag(itemStack).getInt(slotKey + ":" + tweak.key), tweak.steps), -tweak.steps);
    }

    public void setTweakStep(ItemStack itemStack, String tweakKey, int step) {
        NBTHelper.getTag(itemStack).putInt(slotKey + ":" + tweakKey, step);
    }

    public double getDamageModifier(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .mapToDouble(tweak -> tweak.getDamage(getTweakStep(itemStack, tweak)))
                .sum() + getData(itemStack).damage;
    }

    public double getDamageMultiplierModifier(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .map(tweak -> tweak.getDamageMultiplier(getTweakStep(itemStack, tweak)))
                .reduce(getData(itemStack).damageMultiplier, (a, b) -> a * b);
    }

    public double getSpeedModifier(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .mapToDouble(tweak -> tweak.getAttackSpeed(getTweakStep(itemStack, tweak)))
                .sum() + getData(itemStack).attackSpeed;
    }

    public double getSpeedMultiplierModifier(ItemStack itemStack) {
        return Arrays.stream(getTweaks(itemStack))
                .map(tweak -> tweak.getAttackSpeedMultiplier(getTweakStep(itemStack, tweak)))
                .reduce(getData(itemStack).attackSpeedMultiplier, (a, b) -> a * b);
    }

    public ResourceLocation[] getTextures(ItemStack itemStack) {
        return new ResourceLocation[] { getData(itemStack).getTextureLocation() };
    }

    public ModuleModel[] getModels(ItemStack itemStack) {
        ModuleData data = getData(itemStack);

        if (data.models == null || data.models.length == 0) {
            return Arrays.stream(getTextures(itemStack))
                    .map(ModuleModel::new)
                    .toArray(ModuleModel[]::new);
        }

        return data.models;
    }

    public Priority getRenderLayer() {
        return renderLayer;
    }

    // todo 1.14: remove this as all textures are loaded regardless of modules
    @Deprecated
    public ResourceLocation[] getAllTextures() {
        return Arrays.stream(data)
                .map(ModuleData::getTextureLocation)
                .toArray(ResourceLocation[]::new);
    }

    public void hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {}

    public int getEffectLevel(ItemStack itemStack, ItemEffect effect) {
        return getData(itemStack).effects.getLevel(effect);
    }

    public float getEffectEfficiency(ItemStack itemStack, ItemEffect effect) {
        return (float) Arrays.stream(getTweaks(itemStack))
                .mapToDouble(tweak -> tweak.getEffectEfficiency(effect, getTweakStep(itemStack, tweak)))
                .sum() + getData(itemStack).effects.getEfficiency(effect);
    }

    public Collection<ItemEffect> getEffects(ItemStack itemStack) {
        return getData(itemStack).effects.getValues();
    }

    @Override
    public int getCapabilityLevel(ItemStack itemStack, Capability capability) {
        return getData(itemStack).capabilities.getLevel(capability);
    }

    @Override
    public float getCapabilityEfficiency(ItemStack itemStack, Capability capability) {
        return (float) Arrays.stream(getTweaks(itemStack))
                .mapToDouble(tweak -> tweak.getCapabilityEfficiency(capability, getTweakStep(itemStack, tweak)))
                .sum() + getData(itemStack).capabilities.getEfficiency(capability);
    }

    @Override
    public Collection<Capability> getCapabilities(ItemStack itemStack) {
        return getData(itemStack).capabilities.getValues();
    }
}
