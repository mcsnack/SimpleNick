package de.mcsnack.simplenick;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class SimpleNick extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final Map<UUID, String> names = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        if (config.isConfigurationSection("nicknames")) {
            for (String id : Objects.requireNonNull(config.getConfigurationSection("nicknames")).getKeys(false)) {
                try { names.put(UUID.fromString(id), config.getString("nicknames." + id, "")); }
                catch (IllegalArgumentException ignored) { getLogger().warning("Invalid UUID in nicknames: " + id); }
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String name : List.of("nick", "unnick", "nicklist")) {
            Objects.requireNonNull(getCommand(name)).setExecutor(this);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(this);
        }
        for (Player player : Bukkit.getOnlinePlayers()) apply(player);
    }

    private void apply(Player player) {
        String nick = names.get(player.getUniqueId());
        player.displayName(nick == null ? Component.text(player.getName()) : Component.text(nick));
        player.playerListName(nick == null ? Component.text(player.getName()) : Component.text(nick));
    }

    private void persist() {
        getConfig().set("nicknames", null);
        names.forEach((id, nick) -> getConfig().set("nicknames." + id, nick));
        saveConfig();
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { apply(event.getPlayer()); }

    @EventHandler public void onChat(AsyncChatEvent event) {
        String nick = names.get(event.getPlayer().getUniqueId());
        if (nick != null) event.renderer((source, sourceDisplayName, message, viewer) ->
            Component.text(nick).append(Component.text(": ")).append(message));
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) { sender.sendMessage("Only server operators can use this command."); return true; }
        String action = command.getName().toLowerCase(Locale.ROOT);
        if (action.equals("nicklist")) {
            if (names.isEmpty()) { sender.sendMessage("No active nicknames."); return true; }
            names.forEach((id, nick) -> {
                String original = Bukkit.getOfflinePlayer(id).getName();
                sender.sendMessage((original == null ? id.toString() : original) + " -> " + nick);
            });
            return true;
        }
        if (args.length < (action.equals("nick") ? 2 : 1)) { sender.sendMessage("Usage: " + command.getUsage()); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage("Player must be online (use their real account name)."); return true; }
        if (action.equals("nick")) {
            String nick = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            if (nick.isEmpty() || nick.length() > 32 || nick.contains("\n") || nick.contains("\r")) {
                sender.sendMessage("Nickname must be 1-32 characters without line breaks."); return true;
            }
            names.put(target.getUniqueId(), nick);
            sender.sendMessage("Renamed " + target.getName() + " to " + nick);
        } else {
            names.remove(target.getUniqueId());
            sender.sendMessage("Reset nickname of " + target.getName());
        }
        apply(target);
        persist();
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return Collections.emptyList();
        if (command.getName().equalsIgnoreCase("nicklist")) return Collections.emptyList();
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !command.getName().equalsIgnoreCase("unnick") || names.containsKey(p.getUniqueId()))
            .map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .sorted().toList();
    }
}
