package de.mcsnack.simplenick;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class SimpleNick extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private record Group(String name, NamedTextColor color) {}
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Group> groups = new HashMap<>();
    private static final Map<String, NamedTextColor> COLORS = Map.ofEntries(
        Map.entry("black", NamedTextColor.BLACK), Map.entry("dark_blue", NamedTextColor.DARK_BLUE),
        Map.entry("dark_green", NamedTextColor.DARK_GREEN), Map.entry("dark_aqua", NamedTextColor.DARK_AQUA),
        Map.entry("dark_red", NamedTextColor.DARK_RED), Map.entry("dark_purple", NamedTextColor.DARK_PURPLE),
        Map.entry("gold", NamedTextColor.GOLD), Map.entry("gray", NamedTextColor.GRAY),
        Map.entry("dark_gray", NamedTextColor.DARK_GRAY), Map.entry("blue", NamedTextColor.BLUE),
        Map.entry("green", NamedTextColor.GREEN), Map.entry("aqua", NamedTextColor.AQUA),
        Map.entry("red", NamedTextColor.RED), Map.entry("light_purple", NamedTextColor.LIGHT_PURPLE),
        Map.entry("yellow", NamedTextColor.YELLOW), Map.entry("white", NamedTextColor.WHITE)
    );

    @Override public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        if (cfg.isConfigurationSection("nicknames")) {
            for (String id : Objects.requireNonNull(cfg.getConfigurationSection("nicknames")).getKeys(false)) {
                try { names.put(UUID.fromString(id), cfg.getString("nicknames." + id, "")); }
                catch (IllegalArgumentException ex) { getLogger().warning("Invalid nickname UUID: " + id); }
            }
        }
        if (cfg.isConfigurationSection("groups")) {
            for (String id : Objects.requireNonNull(cfg.getConfigurationSection("groups")).getKeys(false)) {
                try {
                    String name = cfg.getString("groups." + id + ".name", "");
                    String color = cfg.getString("groups." + id + ".color", "white").toLowerCase(Locale.ROOT);
                    if (name.matches("[A-Za-z0-9_]{1,8}") && COLORS.containsKey(color))
                        groups.put(UUID.fromString(id), new Group(name, COLORS.get(color)));
                } catch (IllegalArgumentException ex) { getLogger().warning("Invalid group UUID: " + id); }
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String name : List.of("nick", "unnick", "nicklist", "group", "ungroup")) {
            Objects.requireNonNull(getCommand(name)).setExecutor(this);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(this);
        }
        for (Player player : Bukkit.getOnlinePlayers()) apply(player);
    }

    private Component label(Player player) {
        Component base = Component.text(names.getOrDefault(player.getUniqueId(), player.getName()), NamedTextColor.WHITE);
        Group group = groups.get(player.getUniqueId());
        return group == null ? base : Component.text("[", NamedTextColor.WHITE)
            .append(Component.text(group.name(), group.color()))
            .append(Component.text("] ", NamedTextColor.WHITE))
            .append(base);
    }

    private void apply(Player player) {
        player.displayName(label(player));
        player.playerListName(label(player));
    }

    private void persist() {
        FileConfiguration cfg = getConfig();
        cfg.set("nicknames", null);
        names.forEach((id, nick) -> cfg.set("nicknames." + id, nick));
        cfg.set("groups", null);
        groups.forEach((id, group) -> {
            cfg.set("groups." + id + ".name", group.name());
            cfg.set("groups." + id + ".color", COLORS.entrySet().stream()
                .filter(e -> e.getValue().equals(group.color())).map(Map.Entry::getKey).findFirst().orElse("white"));
        });
        saveConfig();
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { apply(event.getPlayer()); }

    @EventHandler public void onChat(AsyncChatEvent event) {
        if (names.containsKey(event.getPlayer().getUniqueId()) || groups.containsKey(event.getPlayer().getUniqueId())) {
            Component display = label(event.getPlayer());
            event.renderer((source, sourceDisplayName, message, viewer) ->
                display.append(Component.text(": ")).append(message));
        }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String action = command.getName().toLowerCase(Locale.ROOT);
        boolean isGroup = action.equals("group") || action.equals("ungroup");
        if (!isGroup && !sender.isOp()) {
            sender.sendMessage("Only operators can use this command."); return true;
        }
        if (action.equals("nicklist")) {
            if (names.isEmpty()) sender.sendMessage("No active nicknames.");
            else names.forEach((id, nick) -> {
                String original = Bukkit.getOfflinePlayer(id).getName();
                sender.sendMessage((original == null ? id : original) + " -> " + nick);
            });
            return true;
        }

        Player target;
        int offset;
        if (isGroup && ((action.equals("group") && args.length == 2)
                || (action.equals("ungroup") && args.length == 0))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must specify a player."); return true;
            }
            target = player;
            offset = 0;
        } else {
            int required = switch (action) { case "nick" -> 2; case "group" -> 3; default -> 1; };
            if (args.length < required) {
                sender.sendMessage("Usage: " + command.getUsage()); return true;
            }
            if (isGroup && !sender.isOp()) {
                sender.sendMessage("Only operators can change other players' groups."); return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("Player must be online; use their real username."); return true;
            }
            offset = 1;
        }
        UUID id = target.getUniqueId();
        switch (action) {
            case "nick" -> {
                String nick = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                if (nick.isEmpty() || nick.length() > 32 || nick.contains("\n") || nick.contains("\r")) {
                    sender.sendMessage("Nickname must be 1-32 characters without line breaks."); return true;
                }
                names.put(id, nick);
                sender.sendMessage("Nickname set for " + target.getName());
            }
            case "unnick" -> { names.remove(id); sender.sendMessage("Nickname removed."); }
            case "group" -> {
                String groupName = args[offset];
                if (!groupName.matches("[A-Za-z0-9_]{1,8}")) {
                    sender.sendMessage("Group must be 1-8 letters, numbers or underscores."); return true;
                }
                NamedTextColor color = COLORS.get(args[offset + 1].toLowerCase(Locale.ROOT));
                if (color == null) {
                    sender.sendMessage("Unknown color. Use TAB to see the 16 standard colors."); return true;
                }
                groups.put(id, new Group(groupName, color));
                sender.sendMessage("Group [" + groupName + "] assigned to " + target.getName());
            }
            case "ungroup" -> { groups.remove(id); sender.sendMessage("Group removed."); }
            default -> { return false; }
        }
        apply(target);
        persist();
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String action = command.getName().toLowerCase(Locale.ROOT);
        if (action.equals("nicklist")) return List.of();
        if (action.equals("group")) {
            if (args.length == 2 && !sender.isOp()) return filter(COLORS.keySet(), args[1]);
            if (args.length == 3 && sender.isOp()) return filter(COLORS.keySet(), args[2]);
            if (args.length == 1 && sender.isOp())
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[0]);
            return List.of();
        }
        if (action.equals("ungroup")) {
            if (!sender.isOp() || args.length != 1) return List.of();
            return filter(Bukkit.getOnlinePlayers().stream()
                .filter(p -> groups.containsKey(p.getUniqueId())).map(Player::getName).toList(), args[0]);
        }
        if (!sender.isOp() || args.length != 1) return List.of();
        return filter(Bukkit.getOnlinePlayers().stream()
            .filter(p -> !action.equals("unnick") || names.containsKey(p.getUniqueId()))
            .map(Player::getName).toList(), args[0]);
    }

    private List<String> filter(Collection<String> values, String prefix) {
        return values.stream().filter(s -> s.toLowerCase(Locale.ROOT)
            .startsWith(prefix.toLowerCase(Locale.ROOT))).sorted().toList();
    }
}
