## Flight API

Flight API is a library for Minecraft (Fabric/NeoForge) that solves the issue of multiple mods conflicting over player flight control. With Flight API, you can avoid situations where players randomly drop to the ground or cannot fly at all because multiple mods keep toggling `player.getAbilities().flying` and `player.getAbilities().allowFlying` on and off.

## Main Concept
   In Vanilla Minecraft (and many mods), flight logic code may be looking like that:

```java
player.getAbilities().flying = true;
player.getAbilities().allowFlying = true;
```
If two (or more) mods do this in an uncoordinated manner, the player’s flight state may toggle rapidly. 

Flight API prevents this by using these concepts:

 - **Owner queue:** Only one mod (one “owner”) can control the player’s flight at any moment. All other flight requests go into a queue. When the current owner releases flight, the next request in the queue becomes active, letting the player continue or stop flying predictably.

 - **Mixin interception:** Any direct modification to `flying` or `allowFlying` is intercepted or blocked if unauthorized.

    - So if a random mod tries to call `flying` directly, it won’t work.
    It’s intercepted, and the library will either redirect that call or deny it. The logs will show a conflict message to help identify the problematic mod.

 - **Unified API:** Mod developers should (and are strongly encouraged to) call:

    ```java
    FlightAPI.requestFlight(String modId, ServerPlayerEntity player);
    // ...
    FlightAPI.releaseFlight(String modId, ServerPlayerEntity player);
    ```
    
    instead of directly accessing `player.getAbilities().flying`. This single control point ensures only one mod is in charge of flight at a time.

## Installation & Setup (Fabric/NeoForge)
   In your build.gradle:
   
```groovy
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/DenisMasterHerobrine/flightapi-${project.mod_loader}")
        }
    }

    dependencies {
        // For Fabric
        modImplementation "dev.denismasterherobrine:flightapi-${project.mod_loader}:${project.flightapi_version}"
    
        // or for NeoForge
        // modImplementation "dev.denismasterherobrine:flightapi-${project.mod_loader}:${project.flightapi_version}"
    }
```

`${project.mod_loader}` is the mod loader you are using to apply Flight API in your `gradle.properties` file. (e.g., "fabric" or "neoforge").

`${project.flightapi_version}` is the version of the Flight API you want to use. You can find the latest version in the [packages]

Also you can jar-in-jar this API in your mod to make it easier to install your mod without much hassle with the Flight API.

## How to Use Flight API in Your Mod
   
### Requesting Flight
   Whenever you want to enable flight for a player (e.g., upon pressing a “Jetpack On” button, casting a levitation spell, etc.), do not call `player.abilities.flying = true`. 
   
Instead call `requestFlight(modId, player)` method:

```java
    boolean gotControl = FlightAPI.requestFlight(modId, player);
    if (gotControl) {
        // You have immediately become the flight owner.
        // Under the hood, FlightManager sets allowFlying/flying = true for the player.
        // The player can now fly (if not in creative/spectator, etc.).
    } else {
        // Another mod is already in control of flight for this player.
        // Your request is queued. Once the current owner releases flight,
        // you'll get ownership automatically.
    }
```
**Note:** modId is a unique string identifying your mod (e.g., "mymod").

### Releasing Flight
When your mod is done controlling flight (the player turns off the jetpack, the spell ends, etc.) you should call the `releaseFlight` method.

```java
FlightApi.releaseFlight("MyAwesomeMod", player);
```

If nobody else is waiting in the queue, the player’s flight is disabled.
If there is another mod in the queue, that mod immediately takes over. The player can continue flying under the new owner (or be forced to land, depending on the new mod’s logic).

### Checking the Current Owner
You can check who currently owns flight:

```java
    Optional<String> currentOwner = FlightAPI.getCurrentOwner(player);

    if (currentOwner.isPresent()) {
        System.out.println("Flight is currently owned by: " + currentOwner.get());
    } else {
        System.out.println("No one owns flight right now");
    }
```
## Example Cases

### “Jetpack” and “Levitation Spell” Mods
   
`JetpackMod` grants flight if the player wears a jetpack item.

`MagicSpellMod` grants flight if the player casts a levitation spell.

Without Flight API, both mods might continually toggle flying = true/false, making the player jerk up and down if not handling this properly.

With Flight API, `JetpackMod` calls `FlightAPI.requestFlight("JetpackMod", player)`.
`MagicSpellMod` calls `FlightAPI.requestFlight("MagicSpellMod", player`), but `JetpackMod` already owns flight, so `MagicSpellMod` is queued.
When JetpackMod’s fuel runs out and it calls `FlightApi.releaseFlight("JetpackMod", player)`, ownership immediately moves to `MagicSpellMod`. The player does not abruptly lose flight.

### A “Buggy” Mod that always sets `flying = false`
Suppose a “BuggyMod” forcibly does `player.getAbilities().flying = false;` every tick.
Thanks to the mixin intercepting that direct assignment, Flight API checks: “Is BuggyMod the current owner? No?”

The assignment is blocked, and the player does not fall. 

The logs can constantly show:
```log
[MixinPlayerAbilities] Flight owned by JetpackMod, ignoring NBT flying=false
```

which helps you to find out the issue and report it to the mod author.
 
### Priority Logic
Out of the box, FlightManager handles requests FIFO (first in, first out). Whoever requests first becomes the owner.

### License
Flight API is licensed under the CC0-1.0 License. You can find the license text in the [LICENSE] file.
Do whatever you want to.