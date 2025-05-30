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
import org.jetbrains.annotations.NotNull;
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
        version = "1.4",
        description = "修改MC客户端遥测中的服务器版本信息",
        authors = {"MoeLuoYu"}
)
public class VelocityVersionModifier {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private String customVersion;
    private List<Integer> customVersionProtocols;
    private static final String PERMISSION_NODE = "velocityversionmodifier.admin";
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
        logger.info("自定义服务器版本设置为: {} (协议号: {})", customVersion, customVersionProtocols);
        registerCommand();
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder();

        int clientProtocol = event.getConnection().getProtocolVersion().getProtocol();
        int selectedProtocol;

        if (customVersionProtocols.contains(clientProtocol)) {
            selectedProtocol = clientProtocol;
        } else if (!customVersionProtocols.isEmpty()) {
            selectedProtocol = customVersionProtocols.get(0);
        } else {
            selectedProtocol = 999;
        }

        String displayVersion = customVersion + " " + String.valueOf(customVersionProtocols)
                .replaceAll("[\\[\\]]", "");

        // 临时替换标记（使用UUID确保唯一性）
        final Map<String, String> replacements = getStringStringMap();

        // 第一阶段：替换为唯一标记
        Map<String, String> tempMarkers = new HashMap<>();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String protocol = entry.getKey();
            String replacement = entry.getValue();
            String marker = "PROTOCOL_" + UUID.randomUUID().toString().replace("-", "_");
            tempMarkers.put(marker, replacement);
            displayVersion = displayVersion.replaceAll("\\b" + protocol + "\\b", marker);
        }

        // 第二阶段：恢复真实替换值
        for (Map.Entry<String, String> entry : tempMarkers.entrySet()) {
            displayVersion = displayVersion.replace(entry.getKey(), entry.getValue());
        }

        ServerPing.Version version = new ServerPing.Version(selectedProtocol, displayVersion);
        builder.version(version);

        event.setPing(builder.build());
    }

    private static @NotNull Map<String, String> getStringStringMap() {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("769", "1.21.4");
        replacements.put("768", "1.21.2/1.21.3");
        replacements.put("767", "1.21.1");
        replacements.put("766", "1.20.5/1.20.6");
        replacements.put("765", "1.20.3/1.20.4");
        replacements.put("764", "1.20.2");
        replacements.put("763", "1.20/1.20.1");
        replacements.put("762", "1.19.4");
        replacements.put("761", "1.19.3");
        replacements.put("760", "1.19.1/1.19.2");
        replacements.put("759", "1.19");
        replacements.put("758", "1.18.2");
        replacements.put("757", "1.18/1.18.1");
        replacements.put("756", "1.17.1");
        replacements.put("755", "1.17");
        replacements.put("754", "1.16.4/1.16.5");
        replacements.put("753", "1.16.3");
        replacements.put("751", "1.16.2");
        replacements.put("736", "1.16.1");
        replacements.put("735", "1.16");
        replacements.put("578", "1.15.2");
        replacements.put("575", "1.15.1");
        replacements.put("573", "1.15");
        replacements.put("498", "1.14.4");
        replacements.put("490", "1.14.3");
        replacements.put("485", "1.14.2");
        replacements.put("480", "1.14.1");
        replacements.put("477", "1.14");
        replacements.put("404", "1.13.2");
        replacements.put("401", "1.13.1");
        replacements.put("393", "1.13");
        replacements.put("340", "1.12.2");
        replacements.put("338", "1.12.1");
        replacements.put("335", "1.12");
        replacements.put("316", "1.11.2");
        replacements.put("315", "1.11/1.11.1");
        replacements.put("210", "1.10/1.10.1/1.10.2");
        replacements.put("110", "1.9.2/1.9.3/1.9.4");
        replacements.put("108", "1.9/1.9.1");
        replacements.put("47", "1.8.X");
        replacements.put("5", "1.7.6/1.7.7/1.7.8/1.7.9/1.7.10");
        replacements.put("4", "1.7.2/1.7.3/1.7.4/1.7.5");
        return replacements;
    }

    private void loadConfig() {
        File configFile = dataDirectory.resolve("config.json").toFile();
        try (FileReader reader = new FileReader(configFile)) {
            Map<String, Object> config = GSON.fromJson(reader, Map.class);
            customVersion = (String) config.getOrDefault("custom-version", "Custom Velocity");
            List<Double> protocolList = (List<Double>) config.getOrDefault("custom-version-protocol", Collections.singletonList(999.0));
            customVersionProtocols = new ArrayList<>();
            for (Double protocol : protocolList) {
                customVersionProtocols.add(protocol.intValue());
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
            customVersion = "Custom Velocity";
            customVersionProtocols = Collections.singletonList(999);
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
            defaultConfig.put("custom-version-protocol", Collections.singletonList(999));
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
                            invocation.source().sendMessage(Component.text("当前设置的协议号: " + customVersionProtocols));
                        } else {
                            String[] protocolStrings = args[1].split(",");
                            List<Integer> newProtocols = new ArrayList<>();
                            for (String protocolString : protocolStrings) {
                                try {
                                    int newProtocol = Integer.parseInt(protocolString.trim());
                                    newProtocols.add(newProtocol);
                                } catch (NumberFormatException e) {
                                    invocation.source().sendMessage(Component.text("协议号必须是有效的整数，使用半角逗号分隔。"));
                                    return;
                                }
                            }
                            customVersionProtocols = newProtocols;
                            saveConfig();
                            invocation.source().sendMessage(Component.text("已将协议号设置为: " + newProtocols));
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
                            return Collections.singletonList("<协议号1,协议号2,...>");
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
        invocation.source().sendMessage(Component.text("/velocityversionmodifier protocol <协议号1,协议号2,...> - 设置新的协议号"));
        invocation.source().sendMessage(Component.text("/velocityversionmodifier reload - 重新加载配置文件"));
    }

    private void saveConfig() {
        File configFile = dataDirectory.resolve("config.json").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            Map<String, Object> config = new HashMap<>();
            config.put("custom-version", customVersion);
            config.put("custom-version-protocol", customVersionProtocols);
            GSON.toJson(config, writer);
            logger.info("已保存配置文件");
        } catch (IOException e) {
            logger.error("保存配置文件失败", e);
        }
    }
}
