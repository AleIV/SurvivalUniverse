package me.infinityz.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import me.infinityz.SurvivalUniverse;
import me.infinityz.chunks.types.PlayerChunk;
import me.infinityz.cities.City;
import me.infinityz.cities.CityChangeEvent;
import me.infinityz.players.SurvivalPlayer;
import me.infinityz.scoreboard.FastBoard;
import net.md_5.bungee.api.ChatColor;

/**
 * GlobalListeners
 */
public class GlobalListeners implements Listener {
    SurvivalUniverse instance;

    public GlobalListeners(SurvivalUniverse instance) {
        this.instance = instance;
    }
    /* Global events, for both chunk and cities */

    @EventHandler
    public void onPlayerLectern(PlayerTakeLecternBookEvent e) {
        e.setCancelled(maybeInCityOrChunk(e.getLectern().getLocation(), e.getPlayer()));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        switch (e.getEntityType()) {
            case ARMOR_STAND:
            case ITEM_FRAME:
            case PAINTING: {
                if (e.getCause() == DamageCause.ENTITY_EXPLOSION || e.getCause() == DamageCause.BLOCK_EXPLOSION
                        || e.getCause() == DamageCause.PROJECTILE) {
                    e.setCancelled(maybeInCityOrChunk(e.getEntity().getLocation()));
                }
            }
            default:
                for (Entity entity : e.getEntity().getPassengers()) {
                    if (entity instanceof Player) {
                        e.setCancelled(maybeInCityOrChunk(e.getEntity().getLocation()));
                        break;
                    }
                }
                break;
        }

    }

    @EventHandler
    public void onSpread(BlockSpreadEvent e) {
        if (e.getSource().getType() == Material.FIRE) {
            e.setCancelled(maybeInCityOrChunk(e.getBlock().getLocation()));
        }
    }

    @EventHandler
    public void onSpread(BlockBurnEvent e) {
        e.setCancelled(maybeInCityOrChunk(e.getBlock().getLocation()));
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player player = (Player) e.getDamager();
            switch (e.getEntity().getType()) {
                case ARMOR_STAND:
                case ITEM_FRAME: {
                    e.setCancelled(maybeInCityOrChunk(e.getEntity().getLocation(), player));
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onHanging(HangingBreakEvent e) {
        if (e.getCause() == RemoveCause.PHYSICS || e.getCause() == RemoveCause.ENTITY)
            return;

        if (instance.chunkManager.findIChunkfromChunk(e.getEntity().getLocation().getChunk()) != null
                || instance.cityManager.isInCity(e.getEntity().getLocation()) != null) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onHangingEntity(HangingBreakByEntityEvent e) {
        if (!(e.getRemover() instanceof Player)) {
            e.setCancelled(maybeInCityOrChunk(e.getEntity().getLocation()));
            return;
        }
        final Player player = (Player) e.getRemover();
        e.setCancelled(maybeInCityOrChunk(e.getEntity().getLocation(), player));

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreakCity(BlockBreakEvent e) {
        e.setCancelled(e.getPlayer() != null ? maybeInCityOrChunk(e.getBlock().getLocation(), e.getPlayer())
                : maybeInCityOrChunk(e.getBlock().getLocation()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlaceCity(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.LECTERN)
            return;
        e.setCancelled(e.getPlayer() != null ? maybeInCityOrChunk(e.getBlock().getLocation(), e.getPlayer())
                : maybeInCityOrChunk(e.getBlock().getLocation()));
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (Bukkit.getOnlinePlayers().size() >= 10 && !e.getPlayer().hasPermission("reserved.slot"))
            e.disallow(Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', "&fServer is full! \n &aGet your rank at survivalrip.buycraft.net \n &bAnd don`t forget to follow us on Twitter @Survival_U"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interactEvent(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() == Material.AIR)
            return;
        switch (e.getClickedBlock().getType()) {
            case ENDER_CHEST:
            case LECTERN:
            case BELL:
            case STONECUTTER:
            case LOOM:
            case FLETCHING_TABLE:
            case CARTOGRAPHY_TABLE:
            case SMITHING_TABLE:
            case GRINDSTONE:
            case NOTE_BLOCK:
            case TRIPWIRE_HOOK:
            case TRIPWIRE:
            case CRAFTING_TABLE:
                return;
            default:
                if (e.getClickedBlock().getType().toString().toLowerCase().contains("plate")
                        || e.getClickedBlock().getType().toString().toLowerCase().endsWith("bed"))
                    return;
                e.setCancelled(maybeInCityOrChunk(e.getClickedBlock().getLocation(), e.getPlayer()));
                return;
        }
    }

    @EventHandler
    public void onChunkChange(PlayerChangeChunkEvent e) {
        instance.scoreboardManager.scoreboardHashMap.get(e.player.getUniqueId()).updateLine(2,
                ChatColor.GREEN + "Chunk: " + ChatColor.WHITE + getChunkCoord(e.to));
        instance.scoreboardManager.scoreboardHashMap.get(e.player.getUniqueId()).updateLine(3,
                ChatColor.GREEN + "Zone: " + ChatColor.WHITE + getZone(e.toLocation));

    }

    @EventHandler
    public void onCityChanged(CityChangeEvent e) {
        if (e.from != null) {
            e.player.sendMessage(ChatColor.RED + "You left " + e.from.cityName + ".");
        }
        if (e.to != null) {
            e.player.sendMessage(ChatColor.GREEN + "You entered " + e.to.cityName + ".");
        }

        instance.scoreboardManager.scoreboardHashMap.get(e.player.getUniqueId()).updateLine(3,
                ChatColor.GREEN + "Zone: " + ChatColor.WHITE + getZone(e.player.getLocation()));
    }

    /*
     * Loads the scoreboard and sets it to easy access. Possibly do some more
     * calculations here.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        instance.playerManager.survivalPlayerMap.put(player.getUniqueId(), new SurvivalPlayer(player.getUniqueId()));
        FastBoard fb = new FastBoard(player);

        fb.updateTitle(ChatColor.translateAlternateColorCodes('&', "  &b&lSurvival Universe  "));

        fb.updateLines(ChatColor.GREEN + "World: " + ChatColor.WHITE + player.getWorld().getName(), " ",
                ChatColor.GREEN + "Chunk: " + ChatColor.WHITE + getChunkCoord(player.getLocation()),
                ChatColor.GREEN + "Zone: " + ChatColor.WHITE + getZone(player.getLocation()), "  ",
                ChatColor.GREEN + "Players: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size(), "   ",
                ChatColor.AQUA + "  survival.rip  ");

        instance.scoreboardManager.scoreboardHashMap.put(player.getUniqueId(), fb);

    }

    String getZone(Location loc) {
        final PlayerChunk chunk = (PlayerChunk) instance.chunkManager.findIChunkfromChunk(loc.getChunk());
        if (chunk != null)
            return "Private";
        final City ci = instance.cityManager.isInCity(loc);
        if (ci != null)
            return "City";
        return "Free";
    }

    String getChunkCoord(Location loc) {
        return loc.getChunk().getX() + ", " + loc.getChunk().getZ();
    }

    String getChunkCoord(Chunk loc) {
        return loc.getX() + ", " + loc.getZ();
    }

    /* Clears the cache from the player that has left */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        instance.playerManager.survivalPlayerMap.remove(player.getUniqueId());
        instance.scoreboardManager.scoreboardHashMap.remove(player.getUniqueId());
        instance.cityManager.lastKnownCityMap.remove(player.getUniqueId());

    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent e) {
        instance.scoreboardManager.scoreboardHashMap.get(e.getPlayer().getUniqueId()).updateLine(0,
                ChatColor.GREEN + "World: " + ChatColor.WHITE + e.getPlayer().getWorld().getName());

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, () -> {
            Bukkit.getOnlinePlayers().parallelStream().forEach(all -> {
                final FastBoard fb = instance.scoreboardManager.scoreboardHashMap.get(all.getUniqueId());
                if (fb == null)
                    return;
                fb.updateLine(5, ChatColor.GREEN + "Players: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
            });

        }, 20L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, () -> {
            Bukkit.getOnlinePlayers().parallelStream().forEach(all -> {
                final FastBoard fb = instance.scoreboardManager.scoreboardHashMap.get(all.getUniqueId());
                if (fb == null)
                    return;
                fb.updateLine(5, ChatColor.GREEN + "Players: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
            });

        }, 20L);
    }

    /* Handles the city change event */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.isCancelled())
            return;
        final Chunk to = e.getTo().getChunk();
        final Chunk from = e.getFrom().getChunk();
        if (to.getX() != from.getX() || to.getZ() != from.getZ()) {
            final Player player = e.getPlayer();
            Bukkit.getPluginManager().callEvent(new PlayerChangeChunkEvent(player, from, to, e.getTo()));
        }
        if (e.getTo().getBlockX() != e.getFrom().getBlockX() || e.getTo().getBlockZ() != e.getFrom().getBlockZ()) {

            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                City player_city = instance.cityManager.lastKnownCityMap.get(e.getPlayer().getUniqueId());
                City found_city = null;
                for (City city : instance.cityManager.cities) {
                    if (instance.cityManager.isInRectangle(e.getTo(), city)) {
                        found_city = city;
                        break;
                    }
                }
                if (player_city == found_city)
                    return;
                Bukkit.getPluginManager().callEvent(new CityChangeEvent(e.getPlayer(), player_city, found_city));
                instance.cityManager.lastKnownCityMap.put(e.getPlayer().getUniqueId(), found_city);
            });

        }
    }

    /* Handles the city change event */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.isCancelled())
            return;
        final Chunk to = e.getTo().getChunk();
        final Chunk from = e.getFrom().getChunk();
        if (to.getX() != from.getX() || to.getZ() != from.getZ()) {
            final Player player = e.getPlayer();
            Bukkit.getPluginManager().callEvent(new PlayerChangeChunkEvent(player, from, to, e.getTo()));
        }
        if (e.getTo().getBlockX() != e.getFrom().getBlockX() || e.getTo().getBlockZ() != e.getFrom().getBlockZ()) {

            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                City player_city = instance.cityManager.lastKnownCityMap.get(e.getPlayer().getUniqueId());
                City found_city = null;
                for (City city : instance.cityManager.cities) {
                    if (instance.cityManager.isInRectangle(e.getTo(), city)) {
                        found_city = city;
                        break;
                    }
                }
                if (player_city == found_city)
                    return;
                Bukkit.getPluginManager().callEvent(new CityChangeEvent(e.getPlayer(), player_city, found_city));
                instance.cityManager.lastKnownCityMap.put(e.getPlayer().getUniqueId(), found_city);
            });

        }

    }

    /* Handles the player's pvp booleans */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() != EntityType.PLAYER)
            return;
        Player damager;
        /*
         * Perform the checks to know if the damager was a projectile and if the shooter
         * of that projectile was a player
         */
        if (e.getDamager().getType() != EntityType.PLAYER) {
            if (!(e.getDamager() instanceof Projectile))
                return;
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() != null && !(projectile.getShooter() instanceof Player))
                return;
            damager = (Player) projectile.getShooter();
        } else {
            damager = (Player) e.getDamager();
        }
        final SurvivalPlayer survivalDamagerPlayer = instance.playerManager.survivalPlayerMap
                .get(damager.getUniqueId());
        if (!survivalDamagerPlayer.pvp) {
            damager.sendMessage(ChatColor.RED + "Your PVP is disabled!");
            e.setCancelled(true);
            return;
        }
        final Player damaged = (Player) e.getEntity();
        final SurvivalPlayer survivalDamagedPlayer = instance.playerManager.survivalPlayerMap
                .get(damaged.getUniqueId());
        if (!survivalDamagedPlayer.pvp) {
            e.setCancelled(true);
            damager.sendMessage(ChatColor.RED + damaged.getName() + " has not enabled their pvp!");
            return;
        }
    }

    boolean maybeInCityOrChunk(Location location, Player player) {
        boolean bol = false;
        final City city = instance.cityManager.isInCity(location);
        if (city != null) {
            if (!(city.isOwner(player) || city.isHelper(player))) {
                bol = true;
            }
        }
        final PlayerChunk chunk = (PlayerChunk) instance.chunkManager.findIChunkfromChunk(location.getChunk());
        if (chunk != null) {
            if (chunk.owner.getMostSignificantBits() == player.getUniqueId().getMostSignificantBits()) {
                bol = false;
            }
        }
        return bol;
    }

    boolean maybeInCityOrChunk(Location location) {
        return instance.chunkManager.findIChunkfromChunk(location.getChunk()) != null
                || instance.cityManager.isInCity(location) != null;
    }

}
