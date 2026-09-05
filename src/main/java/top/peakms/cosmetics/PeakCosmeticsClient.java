package top.peakms.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PeakCosmeticsClient implements ClientModInitializer {
    private static final String API_BASE = "https://auth.peakms.top";
    private static final long REFRESH_MS = 5_000L;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final Map<UUID, CosmeticCache> CACHE = new ConcurrentHashMap<>();
    private static long ticks = 0L;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                return;
            }

            ticks++;
            long now = System.currentTimeMillis();

            for (var player : client.level.players()) {
                UUID uuid = player.getUUID();
                CosmeticCache cached = CACHE.get(uuid);

                if (cached == null || now - cached.fetchedAtMs() >= REFRESH_MS) {
                    refresh(uuid, now);
                }

                if (ticks % 3L == 0L) {
                    render(player, CACHE.get(uuid));
                }
            }
        });
    }

    private static void refresh(UUID uuid, long now) {
        CosmeticCache old = CACHE.get(uuid);
        if (old != null && old.loading()) {
            return;
        }

        CACHE.put(uuid, new CosmeticCache(
                old == null ? "none" : old.cosmeticId(),
                now,
                true
        ));

        String url = API_BASE + "/cosmetics/" + uuid + "/equipped";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(4))
                .header("Accept", "application/json")
                .header("User-Agent", "PeakCosmetics/1.0")
                .GET()
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null || response == null) {
                        CACHE.put(uuid, new CosmeticCache(
                                old == null ? "none" : old.cosmeticId(),
                                System.currentTimeMillis(),
                                false
                        ));
                        return;
                    }

                    if (response.statusCode() == 404) {
                        CACHE.put(uuid, new CosmeticCache(
                                "none",
                                System.currentTimeMillis(),
                                false
                        ));
                        return;
                    }

                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        CACHE.put(uuid, new CosmeticCache(
                                old == null ? "none" : old.cosmeticId(),
                                System.currentTimeMillis(),
                                false
                        ));
                        return;
                    }

                    String cosmetic = parseCosmetic(response.body());
                    CACHE.put(uuid, new CosmeticCache(
                            cosmetic,
                            System.currentTimeMillis(),
                            false
                    ));
                });
    }

    private static String parseCosmetic(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            if (root.has("cosmetic_id") && !root.get("cosmetic_id").isJsonNull()) {
                return root.get("cosmetic_id").getAsString();
            }

            if (root.has("effect") && !root.get("effect").isJsonNull()) {
                return root.get("effect").getAsString();
            }

            if (root.has("equipped") && root.get("equipped").isJsonObject()) {
                JsonObject equipped = root.getAsJsonObject("equipped");
                if (equipped.has("effect") && !equipped.get("effect").isJsonNull()) {
                    return equipped.get("effect").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        return "none";
    }

    private static void render(Player player, CosmeticCache cache) {
        if (cache == null || cache.cosmeticId() == null) {
            return;
        }

        switch (cache.cosmeticId()) {
            case "halo" -> renderHalo(player, ParticleTypes.END_ROD);
            case "soul_halo" -> renderHalo(player, ParticleTypes.SOUL_FIRE_FLAME);
            case "hearts" -> renderHearts(player);
            case "snow" -> renderSnow(player);
            case "wings" -> renderWings(player);
            default -> {
            }
        }
    }

    private static void renderHalo(Player player, ParticleOptions particle) {
        double angle = (ticks * 0.30D) % (Math.PI * 2.0D);
        double radius = 0.46D;
        double x = player.getX() + Math.cos(angle) * radius;
        double y = player.getY() + player.getBbHeight() + 0.28D;
        double z = player.getZ() + Math.sin(angle) * radius;
        spawn(player, particle, x, y, z);

        double opposite = angle + Math.PI;
        spawn(
                player,
                particle,
                player.getX() + Math.cos(opposite) * radius,
                y,
                player.getZ() + Math.sin(opposite) * radius
        );
    }

    private static void renderHearts(Player player) {
        if (ticks % 12L != 0L) {
            return;
        }

        double angle = (ticks * 0.17D) % (Math.PI * 2.0D);
        spawn(
                player,
                ParticleTypes.HEART,
                player.getX() + Math.cos(angle) * 0.55D,
                player.getY() + 1.25D,
                player.getZ() + Math.sin(angle) * 0.55D
        );
    }

    private static void renderSnow(Player player) {
        double angle = (ticks * 0.21D) % (Math.PI * 2.0D);
        double y = player.getY() + 0.4D + ((ticks % 20L) / 20.0D) * 1.5D;
        spawn(
                player,
                ParticleTypes.SNOWFLAKE,
                player.getX() + Math.cos(angle) * 0.65D,
                y,
                player.getZ() + Math.sin(angle) * 0.65D
        );
    }

    private static void renderWings(Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double backX = Math.sin(yaw);
        double backZ = -Math.cos(yaw);
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        double flap = 0.35D + Math.abs(Math.sin(ticks * 0.15D)) * 0.35D;

        double centerX = player.getX() + backX * 0.30D;
        double centerY = player.getY() + 1.25D;
        double centerZ = player.getZ() + backZ * 0.30D;

        spawn(player, ParticleTypes.END_ROD,
                centerX + sideX * flap,
                centerY + 0.25D,
                centerZ + sideZ * flap);
        spawn(player, ParticleTypes.END_ROD,
                centerX - sideX * flap,
                centerY + 0.25D,
                centerZ - sideZ * flap);
        spawn(player, ParticleTypes.END_ROD,
                centerX + sideX * (flap + 0.18D),
                centerY - 0.10D,
                centerZ + sideZ * (flap + 0.18D));
        spawn(player, ParticleTypes.END_ROD,
                centerX - sideX * (flap + 0.18D),
                centerY - 0.10D,
                centerZ - sideZ * (flap + 0.18D));
    }

    private static void spawn(
            Player player,
            ParticleOptions particle,
            double x,
            double y,
            double z
    ) {
        if (player.level().isClientSide()) {
            player.level().addParticle(
                    particle,
                    x,
                    y,
                    z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private record CosmeticCache(
            String cosmeticId,
            long fetchedAtMs,
            boolean loading
    ) {
    }
}
