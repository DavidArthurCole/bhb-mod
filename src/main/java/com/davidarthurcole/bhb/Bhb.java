package com.davidarthurcole.bhb;

import com.google.common.base.Suppliers;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class Bhb implements ModInitializer {

    public static final String MOD_ID = "bhb";

    //BlendScheme control
    public static List<Scheme> loadedBlendSchemes = new ArrayList<>();
    private final static ConfigManager BlendSchemeConfigManager = new ConfigManager("_blend_schemes.json", loadedBlendSchemes);
    private static Scheme activeBlendScheme;

    //ColorScheme control
    public static List<Scheme> loadedColorSchemes = new ArrayList<>();
    private final static ConfigManager ColorSchemeConfigManager = new ConfigManager("_color_schemes.json", loadedColorSchemes);
    private static Scheme activeColorScheme;

    public static final Supplier<List<Color>> KNOWN_COLORS = Suppliers.memoize(() -> {
        List<Color> knownColorsList = new ArrayList<>();
        try{
            JsonObject jsonObject =  new JsonParser().parse(new InputStreamReader(MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("bhb:colors.json")).getInputStream())).getAsJsonObject();
            JsonArray jsonArray = jsonObject.get("knowncolors").getAsJsonArray();

            for (JsonElement jsonElement : jsonArray) {
                JsonObject color = jsonElement.getAsJsonObject();
                String name = color.get("name").getAsString();
                String hex = color.get("hex").getAsString();
                String alias = color.get("alias").getAsString();

                System.out.println("[BHB] Registering color: " + name + " Hex: " + hex + " Alias: " + alias);
                knownColorsList.add(new Color(name, hex, alias));
            }
        } catch(IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
        return knownColorsList;
    });

    //Return hex code representation of certain colors
    public static String lookupColor(String input){
        for(Color c : KNOWN_COLORS.get()) if(input.equals(c.toString())) return c.hexCode;
        return null;
    }

    //Common blending code used by all implementations
    static void blendCommon(int numberOfCodes, String[] codesArray, CommandContext<FabricClientCommandSource> commandContext) {
        String input = StringArgumentType.getString(commandContext, "Input");
        List<String> blendStrings = Blend.blendMain(numberOfCodes, input, codesArray, true);
        StringBuilder blendString = new StringBuilder("\247r\247f");

        for(String s : blendStrings){
            blendString.append(s);
        }

        String returnString = "\247nBlended Name:\n\n" + blendString  + "\n\247a(Data copied to clipboard)";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(blendString.toString()), null);
        commandContext.getSource().getPlayer().sendMessage(Text.of(returnString), false);
    }

    static void colorCommon(String[] codesArray, CommandContext<FabricClientCommandSource> commandContext) {
        StringBuilder returnMessageFormatted = new StringBuilder();
        StringBuilder returnMessage = new StringBuilder();
        String validChars = "0123456789abcdef";

        String message = StringArgumentType.getString(commandContext, "Input");
        int counter = 0;
        for(int j = 0; j < message.length(); ++j, ++counter){
            if(counter >= codesArray.length) counter = 0;
            if(activeColorScheme.toString().equals("Random")){
                int newRandom = new Random().nextInt(validChars.length());
                returnMessage.append("&").append(validChars.charAt(newRandom)).append(message.charAt(j));
                returnMessageFormatted.append("\247").append(validChars.charAt(newRandom)).append(message.charAt(j));
            }
            else{
                returnMessage.append("&").append(codesArray[counter]).append(message.charAt(j));
                returnMessageFormatted.append("\247").append(codesArray[counter]).append(message.charAt(j));
            }
        }

        String returnString = "\247nColored Message:\n\n" + returnMessageFormatted  + "\n\247a(Data copied to clipboard)";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(returnMessage.toString()), null);
        commandContext.getSource().getPlayer().sendMessage(Text.of(returnString), false);
    }

    @Override
    public void onInitialize() {

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("bhb:colorscheme")
                .then(ClientCommandManager.literal("delete")
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                                .executes(context -> deleteScheme(context, SchemeType.COLOR))))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> listSchemes(context, SchemeType.COLOR)))
                .then(ClientCommandManager.literal("load")
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                                .executes(Bhb::loadColorScheme)))
                .then(ClientCommandManager.literal("save")
                        .then(ClientCommandManager.argument("Colors", SimpleCodeArgument.code())
                                .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                                        .executes(Bhb::saveColorScheme))))
        );

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("bhb:color")
                .then(ClientCommandManager.literal("scheme")
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(Bhb::schemeColoredMessage)))
                .then(ClientCommandManager.argument("Colors", SimpleCodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(Bhb::sendColorSchemedMessage)))
        );

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("bhb:blendscheme")
                .then(ClientCommandManager.literal("delete")
                    .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                        .executes(context -> deleteScheme(context, SchemeType.BLEND))))

                .then(ClientCommandManager.literal("list")
                    .executes(context -> listSchemes(context, SchemeType.BLEND)))

                .then(ClientCommandManager.literal("load") //Load a blend from json
                    .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                        .executes(Bhb::loadBlendScheme)))

                .then(ClientCommandManager.literal("save") //Save a blend to json
                    .then(ClientCommandManager.literal("2")
                        .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                            .executes(context -> saveBlendScheme(2, context))))))

                    .then(ClientCommandManager.literal("3")
                        .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                            .executes(context -> saveBlendScheme(3, context)))))))

                    .then(ClientCommandManager.literal("4")
                        .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                            .executes(context -> saveBlendScheme(4, context))))))))

                    .then(ClientCommandManager.literal("5")
                        .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 5", CodeArgument.code())
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                            .executes(context -> saveBlendScheme(5, context)))))))))

                    .then(ClientCommandManager.literal("6")
                        .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 5", CodeArgument.code())
                        .then(ClientCommandManager.argument("Color 6", CodeArgument.code())
                        .then(ClientCommandManager.argument("Scheme Name", StringArgumentType.string())
                            .executes(context -> saveBlendScheme(6, context))))))))))
                )

        );

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("bhb:blend")
                .then(ClientCommandManager.literal("scheme")
                .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(Bhb::schemeBlendedName)))

                .then(ClientCommandManager.literal("2")
                    .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(2, context)))))))

                .then(ClientCommandManager.literal("3")
                    .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(3, context))))))))

                .then(ClientCommandManager.literal("4")
                    .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(4, context)))))))))

                .then(ClientCommandManager.literal("5")
                    .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 5", CodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(5, context))))))))))

                .then(ClientCommandManager.literal("6")
                    .then(ClientCommandManager.argument("Color 1", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 2", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 3", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 4", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 5", CodeArgument.code())
                    .then(ClientCommandManager.argument("Color 6", CodeArgument.code())
                    .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(6, context)))))))))))
            );
    }

    protected static int listSchemes(CommandContext<FabricClientCommandSource> commandContext, SchemeType caller){

        List<Scheme> toList = (caller.equals(SchemeType.BLEND) ? BlendSchemeConfigManager.loadFile() : ColorSchemeConfigManager.loadFile());

        if(caller.equals(SchemeType.BLEND)) System.out.println("Blend type caller");
        else System.out.println("Color type caller");

        StringBuilder available = new StringBuilder("Available " + (caller.equals(SchemeType.BLEND) ? "Blend" : "Color") +  " Schemes: ");
        if(toList.size() == 0) {
            commandContext.getSource().getPlayer().sendMessage(Text.of("No " + (caller.equals(SchemeType.BLEND) ? "Blend" : "Color") + "Schemes are available."), false);
            return 0;
        }
        for(Scheme s : toList) available.append("\247").append(toList.indexOf(s) % 2 == 0 ? 'a' : 'b').append(s.getName()).append(" ");
        commandContext.getSource().getPlayer().sendMessage(Text.of(available.toString()), false);
        return 1;
    }

    public static int deleteScheme(CommandContext<FabricClientCommandSource> commandContext, SchemeType caller){

        String name = StringArgumentType.getString(commandContext, "Scheme Name");
        int returnVal;

        if(caller.equals(SchemeType.BLEND)) returnVal = BlendSchemeConfigManager.deleteScheme(name);
        else returnVal = ColorSchemeConfigManager.deleteScheme(name);

        if(returnVal == 0) commandContext.getSource().getPlayer().sendMessage(Text.of("A " + (caller.equals(SchemeType.BLEND) ? "Blend" : "Color") + "Scheme with this name does not exist."), false);
        else commandContext.getSource().getPlayer().sendMessage(Text.of("Scheme '" + name +  "' deleted."), false);

        return returnVal;
    }

    public static int saveColorScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedColorSchemes = ColorSchemeConfigManager.loadFile();
        List<Scheme> toList = new ArrayList<>(loadedColorSchemes);
        String name = StringArgumentType.getString(commandContext, "Scheme Name");
        for(Scheme s : toList){
            if(s.getName().equals(name)){
                commandContext.getSource().getPlayer().sendMessage(Text.of("A ColorScheme with this name already exists. Please choose another name."), false);
                return 0;
            }
        }
        String[] codesArray = StringArgumentType.getString(commandContext, "Colors").split("");
        loadedColorSchemes.add(new Scheme(name, codesArray));
        ColorSchemeConfigManager.saveFile();
        commandContext.getSource().getPlayer().sendMessage(Text.of("ColorScheme saved to file."), false);
        return 1;
    }

    public static int saveBlendScheme(int numberOfCodes, CommandContext<FabricClientCommandSource> commandContext){

        loadedBlendSchemes = BlendSchemeConfigManager.loadFile();
        List<Scheme> toList = new ArrayList<>(loadedBlendSchemes);
        String name = StringArgumentType.getString(commandContext, "Scheme Name");
        for(Scheme s : toList){
            if(s.getName().equals(name)){
                commandContext.getSource().getPlayer().sendMessage(Text.of("A BlendScheme with this name already exists. Please choose another name."), false);
                return 0;
            }
        }

        String[] codesArray = new String[numberOfCodes];

        String code1 = CodeArgument.getString(commandContext, "Color 1");
        codesArray[0] = code1;
        String code2 = CodeArgument.getString(commandContext, "Color 2");
        codesArray[1] = code2;

        if(numberOfCodes >=3) {
            String code3 = CodeArgument.getString(commandContext, "Color 3");
            codesArray[2] = code3;
        }

        if(numberOfCodes >=4) {
            String code4 = CodeArgument.getString(commandContext, "Color 4");
            codesArray[3] = code4;
        }

        if(numberOfCodes >=5) {
            String code5 = CodeArgument.getString(commandContext, "Color 5");
            codesArray[4] = code5;
        }

        if(numberOfCodes >=6) {
            String code6 = CodeArgument.getString(commandContext, "Color 6");
            codesArray[5] = code6;
        }

        loadedBlendSchemes.add(new Scheme(name, codesArray));
        BlendSchemeConfigManager.saveFile();

        commandContext.getSource().getPlayer().sendMessage(Text.of("BlendScheme saved to file."), false);
        return 1;
    }

    static int loadColorScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedColorSchemes = ColorSchemeConfigManager.loadFile();

        if(!loadedColorSchemes.isEmpty()){
            for(Scheme s : loadedColorSchemes){
                if(s.getName().equals(StringArgumentType.getString(commandContext, "Scheme Name"))){
                    activeColorScheme = s;
                    commandContext.getSource().getPlayer().sendMessage(Text.of("ColorScheme loaded. Use \247a/bhb:color scheme [message] \247fto use it."), false);
                    return 1;
                }
            }
        }
        //If we get here, the scheme was not found.
        commandContext.getSource().getPlayer().sendMessage(Text.of("A ColorScheme with this name does not exist. Please check the name and try again."), false);
        return 0;
    }

    static int loadBlendScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedBlendSchemes = BlendSchemeConfigManager.loadFile();

        if(!loadedBlendSchemes.isEmpty()){
            for(Scheme s : loadedBlendSchemes){
                if(s.getName().equals(StringArgumentType.getString(commandContext, "Scheme Name"))){
                    activeBlendScheme = s;
                    commandContext.getSource().getPlayer().sendMessage(Text.of("BlendScheme loaded. Use \247a/bhb:blend scheme [nickname] \247fto use it."), false);
                    return 1;
                }
            }
        }
        //If it makes it out of the loop, the scheme did not exist
        commandContext.getSource().getPlayer().sendMessage(Text.of("A BlendScheme with this name does not exist. Please check the name and try again."), false);
        return 0;

    }

    static int schemeBlendedName(CommandContext<FabricClientCommandSource> commandContext){

        if(activeBlendScheme == null){
            //If there is not an active scheme loaded, return an error
            commandContext.getSource().getPlayer().sendMessage(Text.of("No scheme loaded. Please use \247a/bhb:blendscheme load [Scheme Name] \247f to load a scheme."), false);
            return 0;
        }
        else{
            int numberOfCodes = activeBlendScheme.getLength();
            String[] codes = activeBlendScheme.getSchemeCodes();
            blendCommon(numberOfCodes, codes, commandContext);
            return 1;
        }
    }

    static int schemeColoredMessage(CommandContext<FabricClientCommandSource> commandContext){
        if(activeColorScheme == null){
            //If there is not an active scheme loaded, return an error
            commandContext.getSource().getPlayer().sendMessage(Text.of("No scheme loaded. Please use \247a/bhb:colorscheme load [Scheme Name] \247f to load a scheme."), false);
            return 0;
        }
        else{
            colorCommon(activeColorScheme.getSchemeCodes(), commandContext);
            return 1;
        }
    }

    static int sendColorSchemedMessage(CommandContext<FabricClientCommandSource> commandContext){

        String codesString = StringArgumentType.getString(commandContext, "Color Codes");
        if(SimpleCodeArgument.code().isOkayMinecraftColorCode(codesString)){
            colorCommon(codesString.split(""), commandContext);
            return 1;
        }
        else{
            return 0;
        }
    }

    static int sendBlendedName(int numberOfCodes, CommandContext<FabricClientCommandSource> commandContext) {

        String[] codesArray = new String[numberOfCodes];

        String code1 = CodeArgument.getString(commandContext, "Color 1");
        codesArray[0] = code1;
        String code2 = CodeArgument.getString(commandContext, "Color 2");
        codesArray[1] = code2;

        if(numberOfCodes >=3) {
            String code3 = CodeArgument.getString(commandContext, "Color 3");
            codesArray[2] = code3;
        }

        if(numberOfCodes >=4) {
            String code4 = CodeArgument.getString(commandContext, "Color 4");
            codesArray[3] = code4;
        }

        if(numberOfCodes >=5) {
            String code5 = CodeArgument.getString(commandContext, "Color 5");
            codesArray[4] = code5;
        }

        if(numberOfCodes >=6) {
            String code6 = CodeArgument.getString(commandContext, "Color 6");
            codesArray[5] = code6;
        }

        //Actual blending
        blendCommon(numberOfCodes, codesArray, commandContext);
        return 1;
    }
}
