# BHB Mod (Fabric)

This [Fabric](https://fabricmc.net/) mod is an integrated **CLIENT-SIDE** implementation of [BHB](https://www.github.com/DavidArthurcole/bhb).  The mod is currently compatible with Minecraft 1.20.1 and below.

## Documentation
- `/help blend` in-game will provide a brief overview of the formatting:
  - `/blend [# of colors] [color 1] [color 2] [etc] ... [name]`
  - `# of colors` dictates how many colors should be blended from start to finish, ex:
    - `/blend 2 000000 FFFFFF` would blend: black to white
    - `/blend 3 000000 FFFFFF 7A7A7A` would blend: black to white to gray
---
## Downloading

Downloads are available on the ["Releases" page](https://github.com/DavidArthurCole/bhb-mod/releases)

---
## Build Instructions
- Download the source
- Run `.\gradlew build`
- Mod jar will be located at `~\build\libs\bhb\bhb-X.X.X.jar`
