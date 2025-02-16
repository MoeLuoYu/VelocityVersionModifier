# VelocityVersionModifier

## 项目概述
VelocityVersionModifier 是一个 Velocity 插件，用于修改 MC 客户端遥测中的服务器版本信息。通过这个插件，你可以自定义服务器版本和协议号，并且可以通过命令动态地修改这些信息。

## 功能特性
- **自定义服务器版本信息**：可以设置自定义的服务器版本名称和协议号。
- **配置文件支持**：配置信息保存在 `config.json` 文件中，方便管理和修改。
- **命令行操作**：提供了简单的命令行操作，方便你查看和修改版本信息，以及重新加载配置文件。

## 安装步骤
1. **下载插件**：从发布页面下载 `velocity-version-modifier-{version}.jar` 文件。
2. **放置插件**：将下载的 `jar` 文件放置到 Velocity 服务器的 `plugins` 目录下。
3. **启动服务器**：启动 Velocity 服务器，插件会自动加载并创建必要的配置文件。

## 配置文件
本插件默认配置文件已在代码中，不需要在 resources 中创建
配置文件位于插件的数据目录下的 `config.json` 文件中，默认路径为 `plugins/velocity-version-modifier/config.json`。

```json
{
    "custom-version": "Custom Velocity",
    "custom-version-protocol": 999
}
```
## 兼容性
本插件不兼容 Bungeecord 和 Bukkit，且由于工作原因不会适配，当然你也可以 Fork 代码并自行适配。

## 问题反馈与支持
如果你在使用过程中遇到任何问题或有改进建议，请提交 Issues，我们将尽快处理。

## 贡献代码
欢迎开发者为该插件贡献代码。如果你有好的想法或改进方案，请提交 Pull Request，我们会认真审核并合并优秀的贡献。

## 联系方式
如果在使用过程中遇到任何问题或有任何建议，欢迎联系插件开发者：
- **QQ**：1498640871

## 许可证
本插件遵循 MIT 许可证进行发布，具体内容请查看 LICENSE 文件。
