package xyz.moeluoyu.velocitypowered.versionmodifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Plugin(
        id = "velocity-version-modifier",
        name = "VelocityVersionModifier",
        version = "1.0",
        description = "修改MC客户端遥测中的服务器版本信息",
        authors = {"MoeLuoYu"}
)
public class VelocityVersionModifier {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private String customVersion;
    private int customVersionProtocol;
    private static final String PERMISSION_NODE = "velocityversionmodifier.admin";
    // 创建一个带有格式化功能的 Gson 实例
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Inject
    public VelocityVersionModifier(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ensureConfigFileExists();
        loadConfig();
        logger.info("定制插件找落雨，买插件上速德优，速德优（北京）网络科技有限公司出品，落雨QQ：1498640871");
        logger.info("自定义服务器版本设置为: {} (协议号: {})", customVersion, customVersionProtocol);
        registerCommand();
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder();
        ServerPing.Version version = new ServerPing.Version(customVersionProtocol, customVersion);
        builder.version(version);
        event.setPing(builder.build());
    }

    private void loadConfig() {
        File configFile = dataDirectory.resolve("config.json").toFile();
        try (FileReader reader = new FileReader(configFile)) {
            // 使用 Gson 从文件中读取 JSON 数据并转换为 Map
            //noinspection unchecked
            Map<String, Object> config = GSON.fromJson(reader, Map.class);
            customVersion = (String) config.getOrDefault("custom-version", "Custom Velocity");
            customVersionProtocol = ((Double) config.getOrDefault("custom-version-protocol", 999.0)).intValue();
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
            customVersion = "Custom Velocity";
            customVersionProtocol = 999;
        }
    }

    private void ensureConfigFileExists() {
        File dataDir = dataDirectory.toFile();
        if (!dataDir.exists()) {
            if (dataDir.mkdirs()) {
                logger.info("已创建插件数据目录: {}", dataDir.getAbsolutePath());
            } else {
                logger.error("创建插件数据目录失败: {}", dataDir.getAbsolutePath());
                return;
            }
        }
        File configFile = dataDirectory.resolve("config.json").toFile();
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
    }

    private void createDefaultConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("custom-version", "Custom Velocity");
            defaultConfig.put("custom-version-protocol", 999);
            // 使用格式化后的 Gson 实例将 Map 转换为 JSON 并写入文件
            GSON.toJson(defaultConfig, writer);
            logger.info("已创建默认配置文件");
        } catch (IOException e) {
            logger.error("创建默认配置文件失败", e);
        }
    }

    private void registerCommand() {
        CommandManager commandManager = server.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("velocityversionmodifier")
                .aliases("vvm")
                .build();
        commandManager.register(commandMeta, new SimpleCommand() {
            // 命令执行和补全逻辑保持不变
            @Override
            public boolean hasPermission(Invocation invocation) {
                return invocation.source().hasPermission(PERMISSION_NODE);
            }

            @Override
            public void execute(Invocation invocation) {
                String[] args = invocation.arguments();
                if (args.length == 0) {
                    sendHelpMessage(invocation);
                    return;
                }
                switch (args[0].toLowerCase()) {
                    case "version":
                        if (args.length == 1) {
                            invocation.source().sendMessage(Component.text("当前设置的版本: " + customVersion));
                        } else {
                            String newVersion = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                            customVersion = newVersion;
                            saveConfig();
                            invocation.source().sendMessage(Component.text("已将版本名称设置为: " + newVersion));
                        }
                        break;
                    case "protocol":
                        if (args.length == 1) {
                            invocation.source().sendMessage(Component.text("当前设置的协议号: " + customVersionProtocol));
                        } else {
                            try {
                                int newProtocol = Integer.parseInt(args[1]);
                                customVersionProtocol = newProtocol;
                                saveConfig();
                                invocation.source().sendMessage(Component.text("已将协议号设置为: " + newProtocol));
                            } catch (NumberFormatException e) {
                                invocation.source().sendMessage(Component.text("协议号必须是一个有效的整数。"));
                            }
                        }
                        break;
                    case "reload":
                        loadConfig();
                        invocation.source().sendMessage(Component.text("已重新加载配置文件，版本信息已更新。"));
                        break;
                    default:
                        invocation.source().sendMessage(Component.text("未知子命令，请使用 /velocityversionmodifier [version | protocol | reload]"));
                }
            }

            @Override
            public List<String> suggest(Invocation invocation) {
                String[] args = invocation.arguments();
                if (args.length == 0) {
                    return Arrays.asList("version", "protocol", "reload");
                } else if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    for (String subCommand : Arrays.asList("version", "protocol", "reload")) {
                        if (subCommand.startsWith(args[0].toLowerCase())) {
                            suggestions.add(subCommand);
                        }
                    }
                    return suggestions;
                } else if (args.length == 2) {
                    switch (args[0].toLowerCase()) {
                        case "version":
                            return Collections.singletonList("<版本名称>");
                        case "protocol":
                            return Collections.singletonList("<协议号>");
                    }
                }
                return Collections.emptyList();
            }
        });
    }

    private void sendHelpMessage(SimpleCommand.Invocation invocation) {
        invocation.source().sendMessage(Component.text("用法:"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier version - 查看当前设置的版本"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier version <版本名称> - 设置新的版本名称"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier protocol - 查看当前设置的协议号"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier protocol <协议号> - 设置新的协议号"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier reload - 重新加载配置文件"));
    }

    private void saveConfig() {
        File configFile = dataDirectory.resolve("config.json").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            Map<String, Object> config = new HashMap<>();
            config.put("custom-version", customVersion);
            config.put("custom-version-protocol", customVersionProtocol);
            // 使用格式化后的 Gson 实例将 Map 转换为 JSON 并写入文件
            GSON.toJson(config, writer);
            logger.info("已保存配置文件");
        } catch (IOException e) {
            logger.error("保存配置文件失败", e);
        }
    }
}
