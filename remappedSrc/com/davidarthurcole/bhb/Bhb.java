package com.davidarthurcole.bhb;

import com.google.common.base.Suppliers;
import com.google.gson.*;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.*;
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

    private static LiteralArgumentBuilder<FabricClientCommandSource> regLit(String str){return ClientCommandManager.literal(str);}
    static RequiredArgumentBuilder<FabricClientCommandSource,String> regArg(String str, ArgumentType<String> type){return ClientCommandManager.argument(str, type);}

    private final static LiteralArgumentBuilder<FabricClientCommandSource> deleteC = regLit("delete");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> deleteB = regLit("delete");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> listC = regLit("list");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> listB = regLit("list");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> saveC = regLit("save");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> saveB = regLit("save");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> loadC = regLit("load");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> loadB = regLit("load");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> colorLit = regLit("color");
    private final static LiteralArgumentBuilder<FabricClientCommandSource> schemeLit = regLit("scheme");

    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> schemeNameArg = regArg("Scheme Name", StringArgumentType.string());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> colorsArg = regArg("Colors", StringArgumentType.string());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> inputArgB = regArg("Input", StringArgumentType.string());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> inputArgC = regArg("Input", StringArgumentType.string());

    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c1 = regArg("Color 1", CodeArgument.code());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c2 = regArg("Color 2", CodeArgument.code());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c3 = regArg("Color 3", CodeArgument.code());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c4 = regArg("Color 4", CodeArgument.code());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c5 = regArg("Color 5", CodeArgument.code());
    private final static RequiredArgumentBuilder<FabricClientCommandSource,String> c6 = regArg("Color 6", CodeArgument.code());

    @Override
    public void onInitialize() {

        //Initialize the colorscheme command and its aliases
        initColorscheme();

        //Initialize the color command and its aliases
        initColor();

        //Initialize the blendscheme command and its aliases
        initBlendScheme();

        //Initialize the blend command and its aliases
        initBlend();

        //Initialize help
        initHelp();
    }

    public static final Supplier<List<Color>> KNOWN_COLORS = Suppliers.memoize(() -> {
        List<Color> knownColorsList = new ArrayList<>();
        try{
            Optional<Resource> res = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("bhb:colors.json"));

            if(res.isPresent()){
                JsonObject jsonObject =  JsonParser.parseReader(new InputStreamReader(res.get().getInputStream())).getAsJsonObject();
                JsonArray jsonArray = jsonObject.get("knowncolors").getAsJsonArray();

                for (JsonElement jsonElement : jsonArray) {
                    JsonObject color = jsonElement.getAsJsonObject();
                    String name = color.get("name").getAsString();
                    String hex = color.get("hex").getAsString();
                    String alias = color.get("alias").getAsString();

                    System.out.println("[BHB] Registering color: " + name + " Hex: " + hex + " Alias: " + alias);
                    knownColorsList.add(new Color(name, hex, alias));
                }
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
    static void blendCommon(int numberOfCodes, List<String> codesArray, CommandContext<FabricClientCommandSource> commandContext) {
        String input = StringArgumentType.getString(commandContext, "Input");
        List<String> blendStrings = Blend.blendMain(numberOfCodes, input, codesArray, true);
        StringBuilder blendStringFormatted = new StringBuilder("\247r\247f");
        StringBuilder blendString = new StringBuilder();

        for(String s : blendStrings){
            blendString.append(s);
            blendStringFormatted.append(s);
        }

        //Compile the literal text objects to send to the client (with RGB text formatting)
        MutableText compiledLiteralText = Text.translatable("");
        List<String> Literals = new ArrayList<>(List.of(blendStringFormatted.toString().replace("§r", "").replace("§f", "").split("&")));
        Literals.removeAll(Collections.singleton(""));

        for(String l : Literals){
            System.out.println("[BHB] Compiling literal: " + l);
            //Create a new literal text object with only the character
            MutableText newLT = Text.translatable(Character.toString(l.charAt(7)));
            //Decode the color from hex
            java.awt.Color color = java.awt.Color.decode(l.substring(0, 7));
            //Set the color of the new literal text object
            newLT.setStyle(Style.EMPTY.withColor(color.getRGB()));
            //Append the new literal text object to the compiled literal text object
            compiledLiteralText.append(newLT);
        }

        //Copy compiled name to clipboard
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(blendString.toString()), null);
        MutableText returnLiteralText = Text.translatable("\n\247nBlended Name:\n\n").append(compiledLiteralText).append(Text.of("\n\n\247a(Data copied to clipboard)\n"));
        commandContext.getSource().getPlayer().sendMessage(returnLiteralText, false);
    }

    static int colorCommon(List<String> codesArray, CommandContext<FabricClientCommandSource> commandContext) {

        //Error catching
        if(!SimpleCodeArgument.code().isOkayMinecraftColorCode(String.join("", codesArray))) return 0;

        StringBuilder returnMessageFormatted = new StringBuilder();
        StringBuilder returnMessage = new StringBuilder();
        String validChars = "0123456789abcdef ";

        String message = StringArgumentType.getString(commandContext, "Input");
        int counter = 0;
        for(int j = 0; j < message.length(); ++j, ++counter){
            //Account for the fact that spaces should not be colored
            if(message.charAt(j) == ' '){
                returnMessage.append(" ");
                returnMessageFormatted.append(" ");
                counter--;
            }
            else{
                if(counter >= codesArray.size()) counter = 0;
                if(activeColorScheme.toString().equals("Random")){
                    int newRandom = new Random().nextInt(validChars.length());
                    returnMessage.append("&").append(validChars.charAt(newRandom)).append(message.charAt(j));
                    returnMessageFormatted.append("\247").append(validChars.charAt(newRandom)).append(message.charAt(j));
                }
                else{
                    returnMessage.append("&").append(codesArray.get(counter)).append(message.charAt(j));
                    returnMessageFormatted.append("\247").append(codesArray.get(counter)).append(message.charAt(j));
                }
            }
        }

        String returnString = "\n\247nColored Message:\n\n" + returnMessageFormatted  + "\n\n\247a(Data copied to clipboard)\n";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(returnMessage.toString()), null);
        commandContext.getSource().getPlayer().sendMessage(Text.of(returnString), false);
        return 1;
    }

    static void initHelp(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:help command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> helpNode =  dispatcher.register(ClientCommandManager.literal("bhb:help")
                .then(colorLit.executes(Bhb::colorCodes).then(ClientCommandManager.literal("copy").executes(Bhb::colorCodesCopy)))
            );
        });
    }

    static void initColorscheme(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:colorscheme command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> colorSchemeNode = dispatcher.register(ClientCommandManager.literal("bhb:colorscheme")
                .then(deleteC.then(schemeNameArg.executes(Bhb::deleteScheme)))
                .then(listC.executes(Bhb::listSchemes))
                .then(loadC.then(schemeNameArg.executes(Bhb::loadColorScheme)))
                .then(saveC.then(schemeNameArg.then(colorsArg.executes(Bhb::saveColorScheme))))
            );

            //Alias the bhb:colorscheme command to colorscheme and cs
            dispatcher.register(regLit("colorscheme").redirect(colorSchemeNode));
            dispatcher.register(regLit("cs").redirect(colorSchemeNode));
        });
    }

    static void initColor(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:color command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> colorNode = dispatcher.register(regLit("bhb:color")
                .then(inputArgC
                        .then(schemeLit.executes(Bhb::schemeColoredMessage))
                        .then(colorsArg.executes(Bhb::sendColorSchemedMessage))
                )
            );

            //Alias the bhb:color command to color
            dispatcher.register(regLit("color").redirect(colorNode));
        });
    }

    static void initBlendScheme(){
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> blendschemeNode = dispatcher.register(regLit("bhb:blendscheme")
                .then(deleteB.then(schemeNameArg.executes(Bhb::deleteScheme)))
                .then(listB.executes(Bhb::listSchemes))
                .then(loadB.then(schemeNameArg.executes(Bhb::loadBlendScheme)))
                .then(saveB.then(schemeNameArg.then(c1.then(c2.then(c3.then(c4.then(c5.then(c6
                    .executes(Bhb::saveBlendScheme))
                    .executes(Bhb::saveBlendScheme))
                    .executes(Bhb::saveBlendScheme))
                    .executes(Bhb::saveBlendScheme))
                    .executes(Bhb::saveBlendScheme))
                ))));
            //Alias the bhb:blendscheme command to blendscheme and bs
            dispatcher.register(regLit("blendscheme").redirect(blendschemeNode));
            dispatcher.register(regLit("bs").redirect(blendschemeNode));
        }));
    }

    static void initBlend(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:blend command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> blendNode = dispatcher.register(regLit("bhb:blend")
                .then(inputArgB
                    .then(schemeLit.executes(Bhb::schemeBlendedName))
                    .then(c1.then(c2.then(c3.then(c4.then(c5.then(c6
                        .executes(Bhb::sendBlendedName))
                        .executes(Bhb::sendBlendedName))
                        .executes(Bhb::sendBlendedName))
                        .executes(Bhb::sendBlendedName))
                        .executes(Bhb::sendBlendedName))
                    )));
            //Alias the bhb:blend command to blend
            dispatcher.register(regLit("blend").redirect(blendNode));
        });
    }

    static int colorCodes(CommandContext<FabricClientCommandSource> commandContext){
        MutableText colors = Text.translatable("");
        for(String c : "0123456789abcdef".split("")) colors.append(Text.of("\247" + c + c.toLowerCase(Locale.ROOT)));

        MutableText message = Text.translatable("\247nColor codes:\247r ").append(colors).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bhb:help colors copy")));
        commandContext.getSource().getPlayer().sendMessage(message, false);
        return 0;
    }

    static int colorCodesCopy(CommandContext<FabricClientCommandSource> commandContext){

        StringBuilder colorsCopyable = new StringBuilder();
        for(String c : "0123456789abcdef".split("")) colorsCopyable.append("&").append(c).append("&&").append(c);

        //Copy to clipboard
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(colorsCopyable.toString()), null);

        commandContext.getSource().getPlayer().sendMessage(Text.translatable("Color codes copied to clipboard."), false);
        return 0;
    }

    protected static int listSchemes(CommandContext<FabricClientCommandSource> commandContext){

        //Whether the call was of Blend type
        boolean isBlend = (commandContext.getInput().split(" ")[0].replace("bhb:", "").equalsIgnoreCase("blendscheme"));

        List<Scheme> toList = (isBlend ? BlendSchemeConfigManager.loadFile() : ColorSchemeConfigManager.loadFile());

        if(isBlend) System.out.println("Blend type caller");
        else System.out.println("Color type caller");

        MutableText available = Text.translatable("\247e\247lAvailable " + (isBlend ? "Blend" : "Color") +  " Schemes:\n\n");
        if(toList.size() == 0) {
            commandContext.getSource().getPlayer().sendMessage(Text.of("No " + (isBlend ? "Blend" : "Color") + "Schemes are available."), false);
            return 0;
        }
        for(Scheme s : toList) {
            String commandOnClick = (isBlend ? "/bhb:blendscheme load " : "/bhb:colorscheme load ") + s.getName();

            MutableText colors = Text.translatable("");
            List<String> codes = new ArrayList<>(s.getSchemeCodes());

            for(int i = 0; i < codes.size(); i++) {
                String code = codes.get(i);
                colors.append(Text.translatable(!isBlend ? "\247" + code + code.toLowerCase(Locale.ROOT)
                        : Integer.toString(i + 1)).setStyle(Style.EMPTY.withColor(java.awt.Color.decode("#" + code).getRGB())));
                if(i != codes.size() - 1) colors.append(Text.translatable(" "));
            }
            MutableText schemeColorInfo = Text.translatable(" \2477\247l(").append(colors).append("\2477\247l)");
            MutableText availableScheme = Text.translatable(s.getName()).append(schemeColorInfo).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  commandOnClick)));

            available.append(Text.translatable("\247" + (toList.indexOf(s) % 2 == 0 ? 'a' : 'b'))).append(availableScheme).append("\n");
        }
        commandContext.getSource().getPlayer().sendMessage(available, false);
        return 1;
    }

    public static int deleteScheme(CommandContext<FabricClientCommandSource> commandContext){

        boolean isBlend = (commandContext.getInput().split(" ")[0].replace("bhb:", "").equalsIgnoreCase("blendscheme"));

        String name = StringArgumentType.getString(commandContext, "Scheme Name");
        int returnVal;

        if(isBlend) returnVal = BlendSchemeConfigManager.deleteScheme(name);
        else returnVal = ColorSchemeConfigManager.deleteScheme(name);

        if(returnVal == 0) commandContext.getSource().getPlayer().sendMessage(Text.of("A " + (isBlend ? "Blend" : "Color") + "Scheme with this name does not exist."), false);
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
        List<String> codesArray = List.of(StringArgumentType.getString(commandContext, "Colors").split(""));
        loadedColorSchemes.add(new Scheme(name, codesArray));
        ColorSchemeConfigManager.saveFile();
        commandContext.getSource().getPlayer().sendMessage(Text.of("ColorScheme saved to file."), false);
        return 1;
    }

    public static int saveBlendScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedBlendSchemes = BlendSchemeConfigManager.loadFile();
        List<Scheme> toList = new ArrayList<>(loadedBlendSchemes);
        String name = StringArgumentType.getString(commandContext, "Scheme Name");
        for(Scheme s : toList){
            if(s.getName().equals(name)){
                commandContext.getSource().getPlayer().sendMessage(Text.of("A BlendScheme with this name already exists. Please choose another name."), false);
                return 0;
            }
        }

        List<String> codesArray = new ArrayList<>();

        //Get the number of codes passed as an integer
        for(int j = 0; j < 6; ++j){
            try{codesArray.add(j, CodeArgument.getString(commandContext, "Color " + (j + 1)));}
            catch(Exception ex){

                loadedBlendSchemes.add(new Scheme(name, codesArray));
                BlendSchemeConfigManager.saveFile();

                commandContext.getSource().getPlayer().sendMessage(Text.of("BlendScheme saved to file."), false);
                return 1;
            }
        }
        //Shouldn't be reached
        return 0;
    }

    static int loadColorScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedColorSchemes = ColorSchemeConfigManager.loadFile();

        if(!loadedColorSchemes.isEmpty()){
            for(Scheme s : loadedColorSchemes){
                if(s.getName().equals(StringArgumentType.getString(commandContext, "Scheme Name"))){
                    activeColorScheme = s;
                    commandContext.getSource().getPlayer().sendMessage(Text.of("ColorScheme \"" + s.getName() + "\" loaded. Use \247a/color [message] scheme \247fto use it."), false);
                    return 1;
                }
            }
        }
        //If we get here, the scheme was not found.
        commandContext.getSource().getPlayer().sendMessage(Text.of("A ColorScheme with this name does not exist. Please choose a new name and try again."), false);
        return 0;
    }

    static int loadBlendScheme(CommandContext<FabricClientCommandSource> commandContext){

        loadedBlendSchemes = BlendSchemeConfigManager.loadFile();

        if(!loadedBlendSchemes.isEmpty()){
            for(Scheme s : loadedBlendSchemes){
                if(s.getName().equals(StringArgumentType.getString(commandContext, "Scheme Name"))){
                    activeBlendScheme = s;
                    commandContext.getSource().getPlayer().sendMessage(Text.of("BlendScheme \"" + s.getName() + "\" loaded. Use \247a/blend [nickname] scheme \247fto use it."), false);
                    return 1;
                }
            }
        }
        //If it makes it out of the loop, the scheme did not exist
        commandContext.getSource().getPlayer().sendMessage(Text.of("A BlendScheme with this name does not exist. Please choose a new name and try again."), false);
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
            List<String> codes = activeBlendScheme.getSchemeCodes();
            blendCommon(numberOfCodes, codes, commandContext);
            return 1;
        }
    }

    static int sendBlendedName(CommandContext<FabricClientCommandSource> commandContext) {

        List<String> codesArray = new ArrayList<>();

        //Get the number of codes passed as an integer
        for(int j = 0; j < 6; ++j){
            try{codesArray.add(j, CodeArgument.getString(commandContext, "Color " + (j + 1)));}
            catch(Exception ex){
                //Actual blending
                blendCommon(j-1, codesArray, commandContext);
                return 1;
            }
        }
        //Should never be reached
        return 0;
    }

    static int schemeColoredMessage(CommandContext<FabricClientCommandSource> commandContext){
        if(activeColorScheme == null){
            //If there is not an active scheme loaded, return an error
            commandContext.getSource().getPlayer().sendMessage(Text.of("No scheme loaded. Please use \247a/bhb:colorscheme load [Scheme Name] \247f to load a scheme."), false);
            return 0;
        }
        //Do the coloring and send the message, return 1 as success
        else return colorCommon(activeColorScheme.getSchemeCodes(), commandContext);
    }

    static int sendColorSchemedMessage(CommandContext<FabricClientCommandSource> commandContext){
        //Get the string from the args
        String codesString = StringArgumentType.getString(commandContext, "Color Codes");

        //Do the coloring and send the message, return 1 as success
        return colorCommon(List.of(codesString.split("")), commandContext);
    }
}
