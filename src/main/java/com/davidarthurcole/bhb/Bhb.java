package com.davidarthurcole.bhb;

import com.google.common.base.Suppliers;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import java.util.List;
import java.util.function.Supplier;

public class Bhb implements ModInitializer {

    public static final String MOD_ID = "bhb";

    //BlendScheme control
    public static List<Scheme> loadedBlendSchemes = new ArrayList<>();
    private final static ConfigManager BlendSchemeConfigManager = new ConfigManager("_blend_schemes.json", loadedBlendSchemes, SchemeType.BLEND);
    private static Scheme activeBlendScheme;

    //ColorScheme control
    public static List<Scheme> loadedColorSchemes = new ArrayList<>();
    private final static ConfigManager ColorSchemeConfigManager = new ConfigManager("_color_schemes.json", loadedColorSchemes, SchemeType.COLOR);
    private static Scheme activeColorScheme;

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

    static void colorCommon(List<String> codesArray, CommandContext<FabricClientCommandSource> commandContext) {
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
    }

    static void initHelp(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:help command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> helpNode =  dispatcher.register(ClientCommandManager.literal("bhb:help")
                    .then(ClientCommandManager.literal("colors")
                            .executes(Bhb::colorCodes)
                            .then(ClientCommandManager.literal("copy")
                                    .executes(Bhb::colorCodesCopy))));
        });
    }

    static void initColorscheme(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:colorscheme command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> colorSchemeNode = dispatcher.register(ClientCommandManager.literal("bhb:colorscheme")
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
                                            .executes(Bhb::saveColorScheme)))));

            //Alias the bhb:colorscheme command to colorscheme and cs
            dispatcher.register(ClientCommandManager.literal("colorscheme").redirect(colorSchemeNode));
            dispatcher.register(ClientCommandManager.literal("cs").redirect(colorSchemeNode));
        });
    }

    static void initColor(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            //Dispatch the bhb:color command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> colorNode = dispatcher.register(ClientCommandManager.literal("bhb:color")
                    .then(ClientCommandManager.literal("scheme")
                            .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                                    .executes(Bhb::schemeColoredMessage)))
                    .then(ClientCommandManager.argument("Colors", SimpleCodeArgument.code())
                            .then(ClientCommandManager.argument("Input", StringArgumentType.string())
                                    .executes(Bhb::sendColorSchemedMessage))));

            //Alias the bhb:color command to color
            dispatcher.register(ClientCommandManager.literal("color").redirect(colorNode));
        });
    }

    static void initBlendScheme(){

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            //Dispatch the bhb:blendscheme command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> blendschemeNode = dispatcher.register(ClientCommandManager.literal("bhb:blendscheme")
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
                    ));

            //Alias the bhb:blendscheme command to blendscheme and bs
            dispatcher.register(ClientCommandManager.literal("blendscheme").redirect(blendschemeNode));
            dispatcher.register(ClientCommandManager.literal("bs").redirect(blendschemeNode));
        });
    }

    static void initBlend(){

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            //Dispatch the bhb:blend command, and store it in a node for aliasing
            LiteralCommandNode<FabricClientCommandSource> blendNode = dispatcher.register(ClientCommandManager.literal("bhb:blend")
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
                                                                                    .executes(context -> (sendBlendedName(6, context))))))))))));

            //Alias the bhb:blend command to blend
            dispatcher.register(ClientCommandManager.literal("blend").redirect(blendNode));
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

    protected static int listSchemes(CommandContext<FabricClientCommandSource> commandContext, SchemeType caller){

        List<Scheme> toList = (caller.equals(SchemeType.BLEND) ? BlendSchemeConfigManager.loadFile() : ColorSchemeConfigManager.loadFile());

        if(caller.equals(SchemeType.BLEND)) System.out.println("Blend type caller");
        else System.out.println("Color type caller");

        MutableText available = Text.translatable("\247e\247lAvailable " + (caller.equals(SchemeType.BLEND) ? "Blend" : "Color") +  " Schemes:\n\n");
        if(toList.size() == 0) {
            commandContext.getSource().getPlayer().sendMessage(Text.of("No " + (caller.equals(SchemeType.BLEND) ? "Blend" : "Color") + "Schemes are available."), false);
            return 0;
        }
        for(Scheme s : toList) {
            String commandOnClick = (caller.equals(SchemeType.BLEND) ? "/bhb:blendscheme load " : "/bhb:colorscheme load ") + s.getName();

            MutableText colors = Text.translatable("");
            List<String> codes = new ArrayList<>(s.getSchemeCodes());
            for(String c : codes){
                colors.append(Text.translatable(caller.equals(SchemeType.COLOR) ? "\247" + c + c.toLowerCase(Locale.ROOT)
                        : Integer.toString(codes.indexOf(c) + 1)).setStyle(Style.EMPTY.withColor(java.awt.Color.decode("#" + c).getRGB())));
                if(codes.indexOf(c) != (codes.size() - 1)) colors.append(Text.translatable(" "));
            }
            MutableText schemeColorInfo = Text.translatable(" \2477\247l(").append(colors).append("\2477\247l)");
            MutableText availableScheme = Text.translatable(s.getName()).append(schemeColorInfo).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  commandOnClick)));

            available.append(Text.translatable("\247" + (toList.indexOf(s) % 2 == 0 ? 'a' : 'b'))).append(availableScheme).append("\n");
        }
        commandContext.getSource().getPlayer().sendMessage(available, false);
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
        List<String> codesArray = List.of(StringArgumentType.getString(commandContext, "Colors").split(""));
        loadedColorSchemes.add(new Scheme(name, codesArray, SchemeType.COLOR));
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

        //Create a new list
        List<String> codesArray = new ArrayList<>(numberOfCodes);

        //Fill the list
        for(int cv = 0; cv <= 5; ++cv){
            if(numberOfCodes >= cv+1){
                codesArray.set(cv, CodeArgument.getString(commandContext, "Color " + (cv + 1)));
            }
        }

        loadedBlendSchemes.add(new Scheme(name, codesArray, SchemeType.BLEND));
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
                    commandContext.getSource().getPlayer().sendMessage(Text.of("ColorScheme \"" + s.getName() + "\" loaded. Use \247a/color scheme [message] \247fto use it."), false);
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
                    commandContext.getSource().getPlayer().sendMessage(Text.of("BlendScheme \"" + s.getName() + "\" loaded. Use \247a/blend scheme [nickname] \247fto use it."), false);
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
            colorCommon(List.of(codesString.split("")), commandContext);
            return 1;
        }
        else{
            return 0;
        }
    }

    static int sendBlendedName(int numberOfCodes, CommandContext<FabricClientCommandSource> commandContext) {


        List<String> codesArray = new ArrayList<>(numberOfCodes);

        for(int cv = 0; cv <= 5; cv++){
            if(numberOfCodes >= cv+1){
                codesArray.set(cv, CodeArgument.getString(commandContext, "Color " + cv + 1));
            }
        }


        //Actual blending
        blendCommon(numberOfCodes, codesArray, commandContext);
        return 1;
    }
}
