package wraith.fwaystones.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wraith.fwaystones.FabricWaystones;
import wraith.fwaystones.access.PlayerEntityMixinAccess;
import wraith.fwaystones.packets.client.VoidRevivePacket;
import wraith.fwaystones.registry.DataComponentRegistry;
import wraith.fwaystones.registry.ItemRegistry;
import wraith.fwaystones.util.TeleportSources;
import wraith.fwaystones.util.Utils;
import java.util.ArrayList;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract ItemStack getStackInHand(Hand hand);

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract boolean clearStatusEffects();

    @Shadow
    public abstract boolean addStatusEffect(StatusEffectInstance effect);

    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    public void revive(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!(_this() instanceof PlayerEntity player)) {
            return;
        }
        ItemStack stack = null;
        for (Hand hand : Hand.values()) {
            var currentStack = getStackInHand(hand);
            if (currentStack.getItem() != ItemRegistry.get("void_totem")) continue;
            stack = currentStack.copy();
            currentStack.decrement(1);
            break;
        }
        if (stack == null) {
            return;
        }
        setHealth(1.0F);
        clearStatusEffects();
        addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        var teleported = false;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new VoidRevivePacket());
            // Try to get the stored waystone
            var hash = stack.get(DataComponentRegistry.BOUND_WAYSTONE);
            if (hash == null) {
                // If no such waystone exists, get a random discovered waystone
                var discovered = ((PlayerEntityMixinAccess) player).fabricWaystones$getDiscoveredWaystones();
                if (!discovered.isEmpty()) {
                    var list = new ArrayList<>(discovered);
                    hash = list.get(Utils.random.nextInt(list.size()));
                }
            }
            if (hash != null) {
                var waystone = FabricWaystones.WAYSTONE_STORAGE.getWaystoneEntity(hash);
                if (waystone != null) {
                    player.fallDistance = 0;
                    waystone.teleportPlayer(player, false, TeleportSources.VOID_TOTEM);
                    teleported = true;
                }
            }
        }
        cir.setReturnValue(teleported || !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY));
        cir.cancel();
    }

    @Unique
    private LivingEntity _this() {
        return (LivingEntity) (Object) this;
    }

}
