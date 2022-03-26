package com.davidarthurcole.bhb;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;

import java.util.Collection;

public class SimpleCodeArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = ImmutableList.of("abc123", "c4cadecd");
    private static final SimpleCommandExceptionType INVALID_CHARS_EXCEPTION = new SimpleCommandExceptionType(() -> "Invalid characters in code.");

    public static SimpleCodeArgument code(){
        return new SimpleCodeArgument();
    }

    public static String getString(final CommandContext<FabricClientCommandSource> context, final String name){
        String argument = context.getArgument(name, String.class);
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final String code = reader.readString();

        if(!isOkayMinecraftColorCode(code)) throw INVALID_CHARS_EXCEPTION.create();
        else return code;
    }

    public boolean isOkayMinecraftColorCode(String check){
        return(check.matches("^[0-9A-Fa-f]+$"));
    }

    public Collection<String> getExamples(){
        return EXAMPLES;
    }

}
