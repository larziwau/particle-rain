package pigcart.particlerain.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pigcart.particlerain.ParticleRainClient;

import java.awt.*;

public class StreakParticle extends WeatherParticle {

    private StreakParticle(ClientLevel level, double x, double y, double z, double facingRotation, SpriteSet provider) {
        super(level, x, y, z);

        if (ParticleRainClient.config.rain.biomeTint) {
            final Color waterColor = new Color(BiomeColors.getAverageWaterColor(level, this.pos));
            final Color fogColor = new Color(this.level.getBiome(this.pos).value().getFogColor());
            this.rCol = (Mth.lerp(ParticleRainClient.config.rain.mix / 100F, waterColor.getRed(), fogColor.getRed()) / 255F);
            this.gCol = (Mth.lerp(ParticleRainClient.config.rain.mix / 100F, waterColor.getGreen(), fogColor.getGreen()) / 255F);
            this.bCol = (Mth.lerp(ParticleRainClient.config.rain.mix / 100F, waterColor.getBlue(), fogColor.getBlue()) / 255F);
        }

        this.setSprite(provider.get(level.getRandom()));
        this.quadSize = 0.5F;
        this.roll = (float) (facingRotation * Mth.HALF_PI);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age % 10 == 0) {
            if (random.nextBoolean()) {
                this.gravity = random.nextFloat() / 10;
            } else {
                this.gravity = 0;
            }
        }
        if (this.onGround) this.shouldFadeOut = true;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float f) {
        Vec3 camPos = camera.getPosition();
        float x = (float) (Mth.lerp(f, this.xo, this.x) - camPos.x());
        float y = (float) (Mth.lerp(f, this.yo, this.y) - camPos.y());
        float z = (float) (Mth.lerp(f, this.zo, this.z) - camPos.z());

        Quaternionf quaternion = new Quaternionf(new AxisAngle4d(this.roll, 0, 1, 0));
        this.flipItTurnwaysIfBackfaced(quaternion, new Vector3f(x, y, z));
        this.renderRotatedQuad(vertexConsumer, quaternion, x, y, z, f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class DefaultFactory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet provider;

        public DefaultFactory(SpriteSet provider) {
            this.provider = provider;
        }

        @Override
        public Particle createParticle(SimpleParticleType parameters, ClientLevel level, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new StreakParticle(level, x, y, z, velocityX, this.provider);
        }
    }
}
