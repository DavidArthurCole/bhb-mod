package com.davidarthurcole.bhb;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;

import java.util.Collection;

public class CodeArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = ImmutableList.of("FFAAFF", "00FFEE");
    private static final SimpleCommandExceptionType INVALID_HEX_CHARS_EXCEPTION = new SimpleCommandExceptionType(() -> "Invalid hex characters in code.");
    private static final SimpleCommandExceptionType INVALID_HEX_LENGTH_EXCEPTION = new SimpleCommandExceptionType(() -> "Invalid length for hex string.");


    public static CodeArgument code(){
        return new CodeArgument();
    }

    public static String getString(final CommandContext<FabricClientCommandSource> context, final String name){
        return context.getArgument(name, String.class);
    }

    public String parse(final StringReader reader) throws CommandSyntaxException {
        final String code = reader.readString();

        if(!Blend.isHexOk(code)){
            if(code.length() == 6) throw INVALID_HEX_CHARS_EXCEPTION.create();
            else throw INVALID_HEX_LENGTH_EXCEPTION.create();
        }
        else{
            return code;
        }
    }

    public Collection<String> getExamples(){
        return EXAMPLES;
    }

}
