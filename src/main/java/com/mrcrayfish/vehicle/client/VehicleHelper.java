package com.mrcrayfish.vehicle.client;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.Buttons;
import com.mrcrayfish.controllable.client.Controller;
import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.client.ClientHandler;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.KeyBinds;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.audio.MovingSoundHorn;
import com.mrcrayfish.vehicle.client.audio.MovingSoundHornRiding;
import com.mrcrayfish.vehicle.client.audio.MovingSoundVehicle;
import com.mrcrayfish.vehicle.client.audio.MovingSoundVehicleRiding;
import com.mrcrayfish.vehicle.common.entity.HeldVehicleDataHandler;
import com.mrcrayfish.vehicle.common.inventory.IStorage;
import com.mrcrayfish.vehicle.entity.HelicopterEntity;
import com.mrcrayfish.vehicle.entity.PlaneEntity;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.util.FluidUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Author: MrCrayfish
 */
public class VehicleHelper
{
    private static final WeakHashMap<UUID, Map<SoundType, ITickableSound>> SOUND_TRACKER = new WeakHashMap<>();
    
    public static void playVehicleSound(PlayerEntity player, PoweredVehicleEntity vehicle)
    {
        Minecraft.getInstance().enqueue(() ->
        {
            Map<SoundType, ITickableSound> soundMap = SOUND_TRACKER.computeIfAbsent(vehicle.getUniqueID(), uuid -> new HashMap<>());
            if(vehicle.getRidingSound() != null && player.equals(Minecraft.getInstance().player))
            {
                ITickableSound sound = soundMap.get(SoundType.ENGINE_RIDING);
                if(sound == null || sound.isDonePlaying() || !Minecraft.getInstance().getSoundHandler().isPlaying(sound))
                {
                    sound = new MovingSoundVehicleRiding(player, vehicle);
                    soundMap.put(SoundType.ENGINE_RIDING, sound);
                    Minecraft.getInstance().getSoundHandler().play(sound);
                }
            }
            if(vehicle.getMovingSound() != null && !player.equals(Minecraft.getInstance().player))
            {
                ITickableSound sound = soundMap.get(SoundType.ENGINE);
                if(sound == null || sound.isDonePlaying() || !Minecraft.getInstance().getSoundHandler().isPlaying(sound))
                {
                    sound = new MovingSoundVehicle(vehicle);
                    soundMap.put(SoundType.ENGINE, sound);
                    Minecraft.getInstance().getSoundHandler().play(new MovingSoundVehicle(vehicle));
                }
            }
            if(vehicle.getHornSound() != null && !player.equals(Minecraft.getInstance().player))
            {
                ITickableSound sound = soundMap.get(SoundType.HORN);
                if(sound == null || sound.isDonePlaying() || !Minecraft.getInstance().getSoundHandler().isPlaying(sound))
                {
                    sound = new MovingSoundHorn(vehicle);
                    soundMap.put(SoundType.HORN, sound);
                    Minecraft.getInstance().getSoundHandler().play(sound);
                }
            }
            if(vehicle.getHornRidingSound() != null && player.equals(Minecraft.getInstance().player))
            {
                ITickableSound sound = soundMap.get(SoundType.HORN_RIDING);
                if(sound == null || sound.isDonePlaying() || !Minecraft.getInstance().getSoundHandler().isPlaying(sound))
                {
                    sound = new MovingSoundHornRiding(player, vehicle);
                    soundMap.put(SoundType.HORN_RIDING, sound);
                    Minecraft.getInstance().getSoundHandler().play(sound);
                }
            }
        });
    }

    public static void playSound(SoundEvent soundEvent, BlockPos pos, float volume, float pitch)
    {
        ISound sound = new SimpleSound(soundEvent, SoundCategory.BLOCKS, volume, pitch, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
        Minecraft.getInstance().deferTask(() -> Minecraft.getInstance().getSoundHandler().play(sound));
    }

    public static void playSound(SoundEvent soundEvent, float volume, float pitch)
    {
        Minecraft.getInstance().deferTask(() -> Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(soundEvent, volume, pitch)));
    }

    //@SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onFogDensity(EntityViewRenderEvent.FogDensity event)
    {
        /*if(event.getEntity().isInsideOfMaterial(ModMaterials.FUELIUM))
        {
            event.setDensity(0.5F);
        }
        else
        {
            event.setDensity(0.01F);
        }
        event.setCanceled(true);*/
    }

    public static PoweredVehicleEntity.AccelerationDirection getAccelerationDirection(LivingEntity entity)
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                if(Config.CLIENT.useTriggers.get())
                {
                    if(controller.getRTriggerValue() != 0.0F && controller.getLTriggerValue() == 0.0F)
                    {
                        return PoweredVehicleEntity.AccelerationDirection.FORWARD;
                    }
                    else if(controller.getLTriggerValue() != 0.0F && controller.getRTriggerValue() == 0.0F)
                    {
                        return PoweredVehicleEntity.AccelerationDirection.REVERSE;
                    }
                }

                boolean forward = controller.getButtonsStates().getState(Buttons.A);
                boolean reverse = controller.getButtonsStates().getState(Buttons.B);
                if(forward && reverse)
                {
                    return PoweredVehicleEntity.AccelerationDirection.CHARGING;
                }
                else if(forward)
                {
                    return PoweredVehicleEntity.AccelerationDirection.FORWARD;
                }
                else if(reverse)
                {
                    return PoweredVehicleEntity.AccelerationDirection.REVERSE;
                }
            }
        }

        GameSettings settings = Minecraft.getInstance().gameSettings;
        boolean forward = settings.keyBindForward.isKeyDown();
        boolean reverse = settings.keyBindBack.isKeyDown();
        if(forward && reverse)
        {
            return PoweredVehicleEntity.AccelerationDirection.CHARGING;
        }
        else if(forward)
        {
            return PoweredVehicleEntity.AccelerationDirection.FORWARD;
        }
        else if(reverse)
        {
            return PoweredVehicleEntity.AccelerationDirection.REVERSE;
        }

        return PoweredVehicleEntity.AccelerationDirection.fromEntity(entity);
    }

    public static PoweredVehicleEntity.TurnDirection getTurnDirection(LivingEntity entity)
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                if(controller.getLThumbStickXValue() > 0.0F)
                {
                    return PoweredVehicleEntity.TurnDirection.RIGHT;
                }
                if(controller.getLThumbStickXValue() < 0.0F)
                {
                    return PoweredVehicleEntity.TurnDirection.LEFT;
                }
                if(controller.getButtonsStates().getState(Buttons.DPAD_RIGHT))
                {
                    return PoweredVehicleEntity.TurnDirection.RIGHT;
                }
                if(controller.getButtonsStates().getState(Buttons.DPAD_LEFT))
                {
                    return PoweredVehicleEntity.TurnDirection.LEFT;
                }
            }
        }
        if(entity.moveStrafing < 0)
        {
            return PoweredVehicleEntity.TurnDirection.RIGHT;
        }
        else if(entity.moveStrafing > 0)
        {
            return PoweredVehicleEntity.TurnDirection.LEFT;
        }
        return PoweredVehicleEntity.TurnDirection.FORWARD;
    }

    public static float getTargetTurnAngle(PoweredVehicleEntity vehicle, boolean drifting)
    {
        PoweredVehicleEntity.TurnDirection direction = vehicle.getTurnDirection();
        if(vehicle.getControllingPassenger() != null)
        {
            if(ClientHandler.isControllableLoaded())
            {
                Controller controller = Controllable.getController();
                if(controller != null)
                {
                    float turnNormal = controller.getLThumbStickXValue();
                    if(turnNormal != 0.0F)
                    {
                        float newTurnAngle = vehicle.turnAngle + ((vehicle.getMaxTurnAngle() * -turnNormal) - vehicle.turnAngle) * 0.15F;
                        if(Math.abs(newTurnAngle) > vehicle.getMaxTurnAngle())
                        {
                            return vehicle.getMaxTurnAngle() * direction.getDir();
                        }
                        return newTurnAngle;
                    }
                }
            }

            if(direction != PoweredVehicleEntity.TurnDirection.FORWARD)
            {
                float amount = direction.getDir() * vehicle.getTurnSensitivity();
                if(drifting)
                {
                    amount *= 0.45F;
                }
                float newTurnAngle = vehicle.turnAngle + amount;
                if(Math.abs(newTurnAngle) > vehicle.getMaxTurnAngle())
                {
                    return vehicle.getMaxTurnAngle() * direction.getDir();
                }
                return newTurnAngle;
            }
        }

        if(drifting)
        {
            return vehicle.turnAngle * 0.95F;
        }
        return vehicle.turnAngle * 0.75F;
    }

    public static boolean isDrifting()
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                if(controller.getButtonsStates().getState(Buttons.RIGHT_BUMPER))
                {
                    return true;
                }
            }
        }
        return Minecraft.getInstance().gameSettings.keyBindJump.isKeyDown();
    }

    public static boolean isHonking()
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                if(controller.isButtonPressed(Buttons.RIGHT_THUMB_STICK))
                {
                    return true;
                }
            }
        }
        return KeyBinds.KEY_HORN.isKeyDown();
    }

    public static PlaneEntity.FlapDirection getFlapDirection()
    {
        boolean flapUp = Minecraft.getInstance().gameSettings.keyBindJump.isKeyDown();
        boolean flapDown = Minecraft.getInstance().gameSettings.keyBindSprint.isKeyDown();
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                flapUp |= controller.getButtonsStates().getState(Buttons.RIGHT_BUMPER);
                flapDown |= controller.getButtonsStates().getState(Buttons.LEFT_BUMPER);
            }
        }
        return PlaneEntity.FlapDirection.fromInput(flapUp, flapDown);
    }

    public static HelicopterEntity.AltitudeChange getAltitudeChange()
    {
        boolean flapUp = Minecraft.getInstance().gameSettings.keyBindJump.isKeyDown();
        boolean flapDown = Minecraft.getInstance().gameSettings.keyBindSprint.isKeyDown();
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                flapUp |= controller.getButtonsStates().getState(Buttons.RIGHT_BUMPER);
                flapDown |= controller.getButtonsStates().getState(Buttons.LEFT_BUMPER);
            }
        }
        return HelicopterEntity.AltitudeChange.fromInput(flapUp, flapDown);
    }

    public static float getTravelDirection(HelicopterEntity vehicle)
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                float xAxis = controller.getLThumbStickXValue();
                float yAxis = controller.getLThumbStickYValue();
                if(xAxis != 0.0F || yAxis != 0.0F)
                {
                    float angle = (float) Math.toDegrees(Math.atan2(-xAxis, yAxis)) + 180F;
                    return vehicle.rotationYaw + angle;
                }
            }
        }

        PoweredVehicleEntity.AccelerationDirection accelerationDirection = vehicle.getAcceleration();
        PoweredVehicleEntity.TurnDirection turnDirection = vehicle.getTurnDirection();
        if(vehicle.getControllingPassenger() != null)
        {
            if(accelerationDirection == PoweredVehicleEntity.AccelerationDirection.FORWARD)
            {
                return vehicle.rotationYaw + turnDirection.getDir() * -45F;
            }
            else if(accelerationDirection == PoweredVehicleEntity.AccelerationDirection.REVERSE)
            {
                return vehicle.rotationYaw + 180F + turnDirection.getDir() * 45F;
            }
            else
            {
                return vehicle.rotationYaw + turnDirection.getDir() * -90F;
            }
        }
        return vehicle.rotationYaw;
    }

    public static float getTravelSpeed(HelicopterEntity helicopter)
    {
        if(ClientHandler.isControllableLoaded())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                float xAxis = controller.getLThumbStickXValue();
                float yAxis = controller.getLThumbStickYValue();
                if(xAxis != 0.0F || yAxis != 0.0F)
                {
                    return (float) Math.min(1.0, Math.sqrt(Math.pow(xAxis, 2) + Math.pow(yAxis, 2)));
                }
            }
        }
        return helicopter.getAcceleration() != PoweredVehicleEntity.AccelerationDirection.NONE || helicopter.getTurnDirection() != PoweredVehicleEntity.TurnDirection.FORWARD ? 1.0F : 0.0F;
    }

    public static float getPower(PoweredVehicleEntity vehicle)
    {
        if(ClientHandler.isControllableLoaded() && Config.CLIENT.useTriggers.get())
        {
            Controller controller = Controllable.getController();
            if(controller != null)
            {
                PoweredVehicleEntity.AccelerationDirection accelerationDirection = vehicle.getAcceleration();
                if(accelerationDirection == PoweredVehicleEntity.AccelerationDirection.FORWARD)
                {
                    return controller.getRTriggerValue();
                }
                else if(accelerationDirection == PoweredVehicleEntity.AccelerationDirection.REVERSE)
                {
                    return controller.getLTriggerValue();
                }
            }
        }
        return 1.0F;
    }

    public static boolean canApplyVehicleYaw(Entity passenger)
    {
        if(passenger.equals(Minecraft.getInstance().player))
        {
            return Config.CLIENT.rotateCameraWithVehicle.get();
        }
        return false;
    }

    public static void spawnWheelParticle(BlockPos pos, BlockState state, double x, double y, double z, Vector3d motion)
    {
        Minecraft mc = Minecraft.getInstance();
        ClientWorld world = mc.world;
        if(world != null)
        {
            DiggingParticle particle = new DiggingParticle(world, x, y, z, motion.x, motion.y, motion.z, state);
            particle.setBlockPos(pos);
            particle.multiplyVelocity((float) motion.length());
            mc.particles.addEffect(particle);
        }
    }

    private enum SoundType
    {
        ENGINE,
        ENGINE_RIDING,
        HORN,
        HORN_RIDING;
    }
}
