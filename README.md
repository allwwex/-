# Peak Cosmetics

Client-only Fabric mod for PeakMC on Minecraft Java **26.1.2**.

This first version renders server-authoritative cosmetic effects around players:

- `halo` - white End Rod halo
- `soul_halo` - blue soul-flame halo
- `hearts` - heart aura
- `snow` - snow aura
- `wings` - glowing particle wings

The mod polls the public Peak API endpoint:

`GET https://auth.peakms.top/cosmetics/{uuid}/equipped`

Expected response:

```json
{
  "uuid": "player-uuid",
  "cosmetic_id": "halo"
}
```

## Versions

- Minecraft: 26.1.2
- Fabric Loader: 0.19.3
- Fabric API: 0.155.2+26.1.2
- Fabric Loom: 1.17-SNAPSHOT
- Java/JDK: 25
- Gradle: 9.5.1

These pins follow the Fabric 26.1.2 example project.

## Build on Windows

1. Install JDK 25.
2. Double-click `BUILD_MOD.bat`.
3. The script downloads Gradle 9.5.1 automatically on the first build.
4. Take the normal JAR from `build\libs\` (do not use the `-sources.jar`).
5. Put the JAR in the PeakMC instance `mods` folder together with Fabric API.

## Security

There are no API secrets in this mod. Cosmetics are fetched through a public read-only endpoint by player UUID. Buying and equipping cosmetics must stay authenticated in the launcher/API.
