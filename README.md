## Flight API (1.21.1+)

Flight API is a library for Minecraft (Fabric/NeoForge) that solves the issue of multiple mods conflicting over player flight control. With Flight API, you can avoid situations where players randomly drop to the ground or cannot fly at all because multiple mods keep toggling `player.getAbilities().flying` and `player.getAbilities().allowFlying` on and off.

While it does not depend on Minecraft versions, it is recommended to use the latest version of the API to avoid any issues. If there will be demand, I will backport the API to older versions of Minecraft. 

1.0.0+ versions of the API are compatible with 1.21.1+ versions of Minecraft.

## Main Concept
   In Vanilla Minecraft (and many mods), flight logic code may be looking like that:

```java
player.getAbilities().flying = true;
player.getAbilities().allowFlying = true;
```
If two (or more) mods do this in an uncoordinated manner, the player’s flight state may toggle rapidly. 

Flight API prevents this by using these concepts:

- Only one mod (one “owner”) can control the player’s flight at any moment. All other flight requests go into a queue. When the current owner releases flight, the next request in the queue becomes active, letting the player continue or stop flying predictably.

- Any direct modification to `flying` or `allowFlying` is intercepted or blocked if unauthorized by mixins.

- Mod developers should (and are strongly encouraged to) call:

    ```java
    FlightAPI.requestFlight(String modId, ServerPlayerEntity player);
    // ...
    FlightAPI.releaseFlight(String modId, ServerPlayerEntity player);
    ```
    
    instead of directly accessing `player.getAbilities().flying`. This single control point ensures only one mod is in charge of flight at a time.

## Dependency Setup (Fabric/NeoForge)
   In your `build.gradle` you need to declare Flight API's maven (or CurseMaven, if you wish to) and use `modImplementation` dependency handler to load the library into your development environment. 
   
Official maven is currently not available, so you can use CurseMaven to load the library into your development environment for now. Sorry for the inconvenience.

Also you can jar-in-jar this API in your mod to make it easier to install your mod without much hassle with the Flight API. However, you can simply depend on it just like any other mod.

## How to Use
   
### Requesting Flight
   Whenever you want to enable flight for a player (e.g., upon pressing a “Jetpack On” button, casting a levitation spell, etc.), do not call `player.abilities.flying = true`. 
   
Instead call `requestFlight(String modId, ServerPlayerEntity player)` method:

```java
    boolean gotControl = FlightAPI.requestFlight(modId, player);
    if (gotControl) {
        // You have immediately become the flight owner.
        // Under the hood, FlightManager sets allowFlying/flying = true for the player.
        // The player can now fly (if not in creative/spectator, etc.).
    } else {
        // Another mod is already in control of flight for this player.
        // Your request is queued. Once the current owner releases flight,
        // you'll get ownership automatically. You can handle this behaviour there.
    }
```
**Note:** `modId` is a unique string identifying your mod (e.g., "mymod").

### Releasing Flight
When your mod is done controlling flight (the player turns off the jetpack, the spell ends, etc.) you should call the `releaseFlight(String modId, ServerPlayerEntity player)` method.

```java
FlightAPI.releaseFlight("MyAwesomeMod", player);
```

If nobody else is waiting in the queue, the player’s flight is disabled.
If there is another mod in the queue, that mod immediately takes over the queue. The player can continue flying under the new owner (or be forced to land, depending on the new mod’s logic).

### Checking the Current Owner
You can check who currently owns the flight:

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

Without Flight API, both mods might continually toggle flying = true/false, making the player jerk up and down if not handling this properly, like if two MCreator mods trying to do the same thing.

With Flight API, `JetpackMod` calls `FlightAPI.requestFlight("JetpackMod", player)`.
`MagicSpellMod` calls `FlightAPI.requestFlight("MagicSpellMod", player`), but `JetpackMod` already owns flight, so `MagicSpellMod` is queued.
When JetpackMod’s fuel runs out and it calls `FlightApi.releaseFlight("JetpackMod", player)`, ownership immediately moves to `MagicSpellMod`. The player does not abruptly lose flight and the spell will continue the flight if conditions are met for the spell flight, of course.

### A “Buggy” Mod breaks my flight! What can I do?
Suppose a “BuggyMod” forcibly does `player.getAbilities().flying = false;` every tick.
Thanks to the mixin intercepting that direct assignment, Flight API checks: “Is BuggyMod the current owner? No?”

The assignment is blocked, and the player does not fall. 

The debug.log in your modpack can constantly show:
```log
[MixinPlayerAbilities] Flight owned by JetpackMod, ignoring NBT flying=false
```

which helps you to find out the issue and report it to the mod author.
 
### Priority Logic
Out of the box, FlightManager handles requests FIFO (first in, first out). Whoever requests first becomes the owner.

### License
Flight API is licensed under the CC0-1.0 License. You can find the license text in the [LICENSE] file.
Do whatever you want to.
