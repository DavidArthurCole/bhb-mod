package com.davidarthurcole.bhb;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class Bhb implements ModInitializer {

    public static final String MOD_ID = "bhb";

    @Override
    public void onInitialize() {

        ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("blend")
                .then(ClientCommandManager.literal("2")
                    .then(ClientCommandManager.argument("code1", CodeArgument.code())
                    .then(ClientCommandManager.argument("code2", CodeArgument.code())
                    .then(ClientCommandManager.argument("input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(2, context)))))))

                .then(ClientCommandManager.literal("3")
                    .then(ClientCommandManager.argument("code1", CodeArgument.code())
                    .then(ClientCommandManager.argument("code2", CodeArgument.code())
                    .then(ClientCommandManager.argument("code3", CodeArgument.code())
                    .then(ClientCommandManager.argument("input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(3, context))))))))

                .then(ClientCommandManager.literal("4")
                    .then(ClientCommandManager.argument("code1", CodeArgument.code())
                    .then(ClientCommandManager.argument("code2", CodeArgument.code())
                    .then(ClientCommandManager.argument("code3", CodeArgument.code())
                    .then(ClientCommandManager.argument("code4", CodeArgument.code())
                    .then(ClientCommandManager.argument("input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(4, context)))))))))

                .then(ClientCommandManager.literal("5")
                    .then(ClientCommandManager.argument("code1", CodeArgument.code())
                    .then(ClientCommandManager.argument("code2", CodeArgument.code())
                    .then(ClientCommandManager.argument("code3", CodeArgument.code())
                    .then(ClientCommandManager.argument("code4", CodeArgument.code())
                    .then(ClientCommandManager.argument("code5", CodeArgument.code())
                    .then(ClientCommandManager.argument("input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(5, context))))))))))

                .then(ClientCommandManager.literal("6")
                    .then(ClientCommandManager.argument("code1", CodeArgument.code())
                    .then(ClientCommandManager.argument("code2", CodeArgument.code())
                    .then(ClientCommandManager.argument("code3", CodeArgument.code())
                    .then(ClientCommandManager.argument("code4", CodeArgument.code())
                    .then(ClientCommandManager.argument("code5", CodeArgument.code())
                    .then(ClientCommandManager.argument("code6", CodeArgument.code())
                    .then(ClientCommandManager.argument("input", StringArgumentType.string())
                        .executes(context -> (sendBlendedName(6, context)))))))))))
            );
    }

    static int sendBlendedName(int numberOfCodes, CommandContext<FabricClientCommandSource> commandContext) {

        String[] codesArray = new String[numberOfCodes];

        String code1 = CodeArgument.getString(commandContext, "code1");
        codesArray[0] = code1;
        String code2 = CodeArgument.getString(commandContext, "code2");
        codesArray[1] = code2;

        if(numberOfCodes >=3) {
            String code3 = CodeArgument.getString(commandContext, "code3");
            codesArray[2] = code3;
        }

        if(numberOfCodes >=4) {
            String code4 = CodeArgument.getString(commandContext, "code4");
            codesArray[3] = code4;
        }

        if(numberOfCodes >=5) {
            String code5 = CodeArgument.getString(commandContext, "code5");
            codesArray[4] = code5;
        }

        if(numberOfCodes >=6) {
            String code6 = CodeArgument.getString(commandContext, "code6");
            codesArray[5] = code6;
        }

        //Actual blending
        String input = StringArgumentType.getString(commandContext, "input");
        String blendString = Blend.blendMain(numberOfCodes, input, codesArray, true);
        String returnString = "\247nBlended Name:\n\n\2477" + blendString  + "\n\247a(Data copied to clipboard)";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(blendString), null);
        commandContext.getSource().getPlayer().sendMessage(Text.of(returnString), false);

        return 1;
    }
}
