package pigcart.particlerain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import static pigcart.particlerain.ParticleRainClient.config;

public final class WeatherParticleSpawner {

    private static final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    //TODO: investigate serene seasons compatibility
    private static void spawnParticle(ClientLevel level, Holder<Biome> biome, double x, double y, double z) {
        if (ParticleRainClient.particleCount > config.maxParticleAmount) {
            //TODO: cancel particle spawns above cloud height
            return;
        }
        if (config.doFogParticles && level.random.nextFloat() < config.fog.density / 100F) {
            level.addParticle(ParticleRainClient.FOG, x, y, z, 0, 0, 0);
        }
        Precipitation precipitation = biome.value().getPrecipitationAt(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos));
        //biome.value().hasPrecipitation() isn't reliable for modded biomes and seasons
        if (precipitation == Precipitation.RAIN) {
            if (config.doGroundFogParticles && ParticleRainClient.fogCount < config.groundFog.density) {
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
                if (height <= config.groundFog.spawnHeight && height >= config.groundFog.spawnHeight - 4 && level.getFluidState(BlockPos.containing(x, height - 1, z)).isEmpty()) {
                    level.addParticle(ParticleRainClient.GROUND_FOG, x, height + level.random.nextFloat(), z, 0, 0, 0);
                }
            }
            if (config.doRainParticles && level.random.nextFloat() < config.rain.density / 100F) {
                level.addParticle(ParticleRainClient.RAIN, x, y, z, 0, 0, 0);
            }
        } else if (precipitation == Precipitation.SNOW && config.doSnowParticles) {
            if (level.random.nextFloat() < config.snow.density / 100F) {
                level.addParticle(ParticleRainClient.SNOW, x, y, z, 0, 0, 0);
            }
        } else if (precipitation == Precipitation.NONE && level.getBlockState(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, y, z)).below()).is(BlockTags.SAND) && biome.value().getBaseTemperature() > 0.25) {
            if (config.sand.spawnOnGround) y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            if (config.doSandParticles) {
                if (level.random.nextFloat() < config.sand.density / 100F) {
                    level.addParticle(ParticleRainClient.DUST, x, y, z, 0, 0, 0);
                }
            }
            if (config.doShrubParticles) {
                if (level.random.nextFloat() < config.shrub.density / 100F) {
                    level.addParticle(ParticleRainClient.SHRUB, x, y, z, 0, 0, 0);
                }
            }
    }
    }

    public static void update(ClientLevel level, Entity entity, float partialTicks) {
        if (level.isRaining() || config.alwaysRaining) {
            int density;
            if (level.isThundering())
                if (config.alwaysRaining) {
                    density = config.particleStormDensity;
                } else {
                    density = (int) (config.particleStormDensity * level.getRainLevel(partialTicks));
                }
            else if (config.alwaysRaining) {
                density = config.particleDensity;
            } else {
                density = (int) (config.particleDensity * level.getRainLevel(partialTicks));
            }


            RandomSource rand = RandomSource.create();

            for (int pass = 0; pass < density; pass++) {

                float theta = (float) (2 * Math.PI * rand.nextFloat());
                float phi = (float) Math.acos(2 * rand.nextFloat() - 1);
                double x = config.particleRadius * Mth.sin(phi) * Math.cos(theta);
                double y = config.particleRadius * Mth.sin(phi) * Math.sin(theta);
                double z = config.particleRadius * Mth.cos(phi);

                pos.set(x + entity.getX(), y + entity.getY(), z + entity.getZ());
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY())
                    continue;

                spawnParticle(level, level.getBiome(pos), pos.getX() + rand.nextFloat(), pos.getY() + rand.nextFloat(), pos.getZ() + rand.nextFloat());
            }
        }
    }

    @Nullable
    public static SoundEvent getBiomeSound(BlockPos blockPos, boolean above) {
        Holder<Biome> biome = Minecraft.getInstance().level.getBiome(blockPos);
        if (biome.value().hasPrecipitation()) {
            if (biome.value().getPrecipitationAt(blockPos) == Precipitation.RAIN) {
                return above ? SoundEvents.WEATHER_RAIN_ABOVE : SoundEvents.WEATHER_RAIN;
            } else if (biome.value().getPrecipitationAt(blockPos) == Precipitation.SNOW) {
                return above ? ParticleRainClient.WEATHER_SNOW_ABOVE : ParticleRainClient.WEATHER_SNOW;
            }
        } else if (biome.value().getPrecipitationAt(blockPos) == Precipitation.NONE && String.valueOf(BuiltInRegistries.BLOCK.getKey(Minecraft.getInstance().level.getBlockState(Minecraft.getInstance().level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).below()).getBlock())).contains("sand") && biome.value().getBaseTemperature() >= 1.0F) {
            return above ? ParticleRainClient.WEATHER_SANDSTORM_ABOVE : ParticleRainClient.WEATHER_SANDSTORM;
        }
        return null;
    }
}