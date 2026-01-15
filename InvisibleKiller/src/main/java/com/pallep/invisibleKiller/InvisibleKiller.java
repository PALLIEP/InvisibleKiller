package com.pallep.invisibleKiller;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InvisibleKiller extends JavaPlugin implements Listener, TabCompleter {

    private YamlConfiguration lang;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        String langCode = getConfig().getString("language", "ru");
        loadLanguage(langCode);

        if (getCommand("iklang") != null) {
            getCommand("iklang").setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
    }

    private void loadLanguage(String code) {
        try (InputStreamReader reader = new InputStreamReader(
                getResource("lang/" + code + ".yml"),
                StandardCharsets.UTF_8
        )) {
            lang = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            getLogger().warning("Не удалось загрузить язык: " + code + ", используется ru по умолчанию");
            loadLanguage("ru");
        }
    }

    private String getMessage(String path, Player player) {
        List<String> list = lang.getStringList(path);

        if (list.isEmpty()) {
            list = lang.getStringList("death.default");
        }

        return list.get(random.nextInt(list.size()))
                .replace("%player%", player.getName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (!killer.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;

        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (lastDamage == null) return;

        EntityDamageEvent.DamageCause cause = lastDamage.getCause();

        String key = switch (cause) {
            case ENTITY_ATTACK -> "death.entity_attack";
            case PROJECTILE -> "death.projectile";
            case FALL -> "death.fall";
            case LAVA -> "death.lava";
            case FIRE, FIRE_TICK -> "death.fire";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "death.explosion";
            default -> "death.default";
        };

        String message = getMessage(key, victim);
        event.deathMessage(Component.text(message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("iklang")) {
            if (args.length != 1) {
                sender.sendMessage(getConfigMessage("command.usage"));
                return true;
            }

            String code = args[0].toLowerCase();
            if (!code.equals("ru") && !code.equals("en")) {
                sender.sendMessage(getConfigMessage("command.invalid"));
                return true;
            }

            loadLanguage(code);
            getConfig().set("language", code);
            saveConfig();

            sender.sendMessage(getConfigMessage("command.changed").replace("%lang%", code));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("iklang")) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String[] supported = {"ru", "en"};
                for (String lang : supported) {
                    if (lang.startsWith(args[0].toLowerCase())) {
                        completions.add(lang);
                    }
                }
            }
            return completions;
        }
        return null;
    }

    private String getConfigMessage(String path) {
        String msg = lang.getString(path, path);
        return msg.replace("&", "§");
    }
}
