package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, priority = Integer.MAX_VALUE)
public abstract class MixinLocalPlayer {

    @Shadow
    @Final
    public ClientPacketListener connection;

    @Shadow
    public ClientInput input;

    @Shadow
    private double xLast;
    @Shadow
    private double yLast;
    @Shadow
    private double zLast;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;
    @Shadow
    private int positionReminder;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean lastHorizontalCollision;
    @Shadow
    private boolean autoJumpEnabled;

    @Shadow
    protected abstract boolean isControlledCamera();

    @Shadow
    protected abstract void sendIsSprintingIfNeeded();

    /**
     * Replaces the movement packet sending with the TrollHack spoof-aware version.
     * Vanilla runs untouched unless a module cancelled/modified the Pre event.
     */
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void trollhack$sendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        OnUpdateWalkingPlayerEvent.Pre event = Hooks.onUpdateWalkingPlayerPre(self);

        if (!event.getCancelled()) {
            return;
        }

        ci.cancel();
        this.sendIsSprintingIfNeeded();

        if (!event.getCancelAll() && this.isControlledCamera()) {
            Vec3 position = event.getPosition();
            float yRot = event.getRotationX();
            float xRot = event.getRotationY();
            boolean onGround = event.isOnGround();
            boolean horizontalCollision = self.horizontalCollision;

            double deltaX = position.x - this.xLast;
            double deltaY = position.y - this.yLast;
            double deltaZ = position.z - this.zLast;
            double deltaYRot = yRot - this.yRotLast;
            double deltaXRot = xRot - this.xRotLast;

            this.positionReminder++;
            boolean move = !event.getCancelMove()
                && (Mth.lengthSquared(deltaX, deltaY, deltaZ) > Mth.square(2.0E-4) || this.positionReminder >= 20);
            boolean rot = !event.getCancelRotate() && (deltaYRot != 0.0 || deltaXRot != 0.0);

            if (move && rot) {
                this.connection.send(new ServerboundMovePlayerPacket.PosRot(position, yRot, xRot, onGround, horizontalCollision));
            } else if (move) {
                this.connection.send(new ServerboundMovePlayerPacket.Pos(position, onGround, horizontalCollision));
            } else if (rot) {
                this.connection.send(new ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, horizontalCollision));
            } else if (this.lastOnGround != onGround || this.lastHorizontalCollision != horizontalCollision) {
                this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision));
            }

            if (move) {
                this.xLast = position.x;
                this.yLast = position.y;
                this.zLast = position.z;
                this.positionReminder = 0;
            }

            if (rot) {
                this.yRotLast = yRot;
                this.xRotLast = xRot;
            }

            this.lastOnGround = onGround;
            this.lastHorizontalCollision = horizontalCollision;
            this.autoJumpEnabled = net.minecraft.client.Minecraft.getInstance().options.autoJump().get();
        }

        Hooks.onUpdateWalkingPlayerPost(event);
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void trollhack$sendPositionPost(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Hooks.onUpdateWalkingPlayerPost(
            new OnUpdateWalkingPlayerEvent.Pre(self.position(), self.getYRot(), self.getXRot(), self.onGround())
        );
    }

    /** InputUpdateEvent fires right after vanilla ticks the movement input. */
    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/ClientInput;tick()V",
            shift = At.Shift.AFTER
        )
    )
    private void trollhack$inputUpdate(CallbackInfo ci) {
        Hooks.onInputUpdate(this.input);
    }

    /** PlayerMoveEvent.Pre: modules may replace the self-movement vector. */
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 trollhack$moveModify(Vec3 movement, MoverType type) {
        if (type == MoverType.SELF) {
            return Hooks.onPlayerMovePre((LocalPlayer) (Object) this, movement);
        }
        return movement;
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void trollhack$movePost(MoverType type, Vec3 movement, CallbackInfo ci) {
        Hooks.onPlayerMovePost();
    }
}
