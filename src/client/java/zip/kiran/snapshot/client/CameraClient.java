package zip.kiran.snapshot.client;

import zip.kiran.snapshot.CameraMod;
import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.net.payload.CameraSettingsC2S;
import zip.kiran.snapshot.client.capture.PhotoCapture;
import zip.kiran.snapshot.client.net.ClientCameraNetworking;
import zip.kiran.snapshot.client.photo.ClientPhotoCache;
import zip.kiran.snapshot.client.render.CameraHud;
import zip.kiran.snapshot.client.render.CameraItemRenderer;
import zip.kiran.snapshot.client.render.DofPostProcessor;
import zip.kiran.snapshot.client.render.PhotographItemRenderer;
import zip.kiran.snapshot.component.CameraSettings;
import zip.kiran.snapshot.item.Items;
import zip.kiran.snapshot.item.misc.CameraItem;
import zip.kiran.snapshot.sound.Sounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CameraClient {
    private CameraClient() {
    }

    private static final ViewfinderState STATE = new ViewfinderState();
    private static final DofPostProcessor DEPTH_OF_FIELD = new DofPostProcessor();
    private static final Map<KeyBinding, Boolean> KEY_WAS_DOWN = new IdentityHashMap<>();
    private static final ClientCameraState CLIENT_STATE = new ClientCameraState();

    private static final double ENTITY_FOCUS_RANGE = 96.0D;

    public static ViewfinderState state() {
        return STATE;
    }

    public static void init() {
        CameraKeys.register();
        ClientCameraNetworking.register();

        ClientTickEvents.END_CLIENT_TICK.register(CameraClient::tick);
        WorldRenderEvents.END.register(context -> onWorldFrameEnd());
        HudRenderCallback.EVENT.register(CameraHud::render);

        ModelLoadingPlugin.register(context -> {
            context.addModels(CameraItemRenderer.BODY);
        });
        BuiltinItemRendererRegistry.INSTANCE.register(Items.PHOTOGRAPH, new PhotographItemRenderer());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPhotoCache.clear();
            CLIENT_STATE.setViewfinderOn(false);
            STATE.setActive(false);
            CLIENT_STATE.setWasActive(false);
            CLIENT_STATE.reset();
        });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return new Identifier(CameraMod.MOD_ID, "camera_shaders");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        DEPTH_OF_FIELD.invalidate();
                    }
                });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return new Identifier(CameraMod.MOD_ID, "hud_colours");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        try {
                            java.util.Properties props = new java.util.Properties();
                            var is = manager.open(new Identifier(CameraMod.MOD_ID, "hud_colours.properties"));
                            props.load(is);
                            int mask = Integer.parseUnsignedInt(props.getProperty("mask", "3355837958"));
                            int bracket = Integer.parseUnsignedInt(props.getProperty("bracket", "4293913570"));
                            int readout = Integer.parseUnsignedInt(props.getProperty("readout", "4293453018"));
                            int readoutSelected = Integer.parseUnsignedInt(props.getProperty("readout_selected", "4294952043"));
                            int readoutDim = Integer.parseUnsignedInt(props.getProperty("readout_dim", "4287268219"));
                            int card = Integer.parseUnsignedInt(props.getProperty("card", "4294243051"));
                            zip.kiran.snapshot.client.render.CameraHud.configure(mask, bracket, readout, readoutSelected, readoutDim, card);
                        } catch (Exception e) {
                            CameraMod.LOGGER.warn("Failed to load HUD colours", e);
                        }
                    }
                });
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean holdingCamera = player != null && !heldCamera(player).isEmpty();
        boolean canOperate = holdingCamera && client.currentScreen == null;

        boolean useEdge = justPressed(client.options.useKey);
        boolean toggleEdge = justPressed(CameraKeys.toggleViewfinder);
        if (canOperate && (useEdge || toggleEdge)) {
            CLIENT_STATE.setViewfinderOn(!CLIENT_STATE.isViewfinderOn());
        }
        if (!canOperate) {
            CLIENT_STATE.setViewfinderOn(false);
        }

        boolean active = CLIENT_STATE.isViewfinderOn() && canOperate;

        if (active && !CLIENT_STATE.wasActive()) {
            STATE.setSettings(CameraItem.settingsOf(heldCamera(player)));
        } else if (!active && CLIENT_STATE.wasActive() && client.getNetworkHandler() != null) {
            ClientPlayNetworking.send(new CameraSettingsC2S(STATE.settings()));
        }

        CLIENT_STATE.setWasActive(active);
        STATE.setActive(active);

        pollEdge(client.options.attackKey);
        pollEdge(CameraKeys.cycleDial);
        pollEdge(CameraKeys.adjustUp);
        pollEdge(CameraKeys.adjustDown);
        pollEdge(CameraKeys.autofocus);

        if (!active) {
            return;
        }

        if (consumeEdge(client.options.attackKey)) {
            fireShutter();
        }
        if (consumeEdge(CameraKeys.cycleDial)) {
            STATE.cycleDial();
            click(client, 1.45F);
        }
        if (consumeEdge(CameraKeys.adjustUp)) {
            adjust(client, 1);
        }
        if (consumeEdge(CameraKeys.adjustDown)) {
            adjust(client, -1);
        }
        if (consumeEdge(CameraKeys.autofocus)) {
            autofocus(client);
        }
    }

    private static final Map<KeyBinding, Boolean> EDGES = new IdentityHashMap<>();

    private static void pollEdge(KeyBinding binding) {
        EDGES.put(binding, justPressed(binding));
    }

    private static boolean consumeEdge(KeyBinding binding) {
        Boolean edge = EDGES.put(binding, Boolean.FALSE);
        return edge != null && edge;
    }

    private static boolean justPressed(KeyBinding binding) {
        boolean down = binding.isPressed();
        boolean was = KEY_WAS_DOWN.getOrDefault(binding, Boolean.FALSE);
        KEY_WAS_DOWN.put(binding, down);
        return down && !was;
    }

    private static ItemStack heldCamera(ClientPlayerEntity player) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Items.CAMERA)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void onWorldFrameEnd() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (STATE.isActive()) {
            DEPTH_OF_FIELD.render(client, STATE);
        }
        PhotoCapture.onWorldFrameEnd(client, STATE.settings());
    }

    private static void fireShutter() {
        long now = System.currentTimeMillis();
        if (now - CLIENT_STATE.getLastShutterMillis() < CameraConstants.SHUTTER_COOLDOWN_MS) {
            return;
        }
        CLIENT_STATE.setLastShutterMillis(now);
        PhotoCapture.request();
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(PositionedSoundInstance.master(Sounds.CAMERA_SHUTTER, 1.0F, 0.9F));
    }

    public static boolean onAttackPressed() {
        if (!STATE.isActive()) {
            return false;
        }
        fireShutter();
        return true;
    }

    public static boolean isViewfinderActive() {
        return STATE.isActive();
    }

    public static boolean onScroll(double vertical) {
        if (!STATE.isActive() || vertical == 0.0D) {
            return STATE.isActive();
        }
        adjust(MinecraftClient.getInstance(), vertical > 0.0D ? 1 : -1);
        return true;
    }

    public static double fovFor(double vanillaFov) {
        return STATE.fov(vanillaFov);
    }

    private static void adjust(MinecraftClient client, int direction) {
        if (STATE.adjust(direction)) {
            click(client, 1.0F + direction * 0.07F);
        }
    }

    private static void autofocus(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        if (camera == null) {
            return;
        }

        Vec3d origin = camera.getCameraPosVec(1.0F);
        float distance = CameraSettings.MAX_FOCUS;

        HitResult blockHit = camera.raycast(CameraSettings.MAX_FOCUS, 1.0F, false);
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            distance = (float) blockHit.getPos().distanceTo(origin);
        }

        double range = Math.min(distance, ENTITY_FOCUS_RANGE);
        Vec3d direction = camera.getRotationVec(1.0F).multiply(range);
        Vec3d end = origin.add(direction);
        Box box = camera.getBoundingBox().stretch(direction).expand(1.0D);

        EntityHitResult entityHit = ProjectileUtil.raycast(camera, origin, end, box,
                entity -> !entity.isSpectator() && entity.isAlive(), range * range);
        if (entityHit != null) {
            float entityDistance = (float) entityHit.getPos().distanceTo(origin);
            if (entityDistance < distance) {
                distance = entityDistance;
            }
        }

        STATE.setFocusDistance(MathHelper.clamp(distance, CameraSettings.MIN_FOCUS, CameraSettings.MAX_FOCUS));
        click(client, 1.7F);
    }

    private static void click(MinecraftClient client, float pitch) {
        client.getSoundManager().play(PositionedSoundInstance.master(Sounds.CAMERA_DIAL, pitch, 0.35F));
    }
}
