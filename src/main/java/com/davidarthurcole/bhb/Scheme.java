package com.davidarthurcole.bhb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scheme {

    private final String name;
    private final List<String> schemeCodes;
    private final int length;
    private final SchemeType type;

    public Scheme(String name, List<String> codes, SchemeType type){
        this.name = name;
        this.schemeCodes = new ArrayList<>(codes);
        this.length = schemeCodes.size();
        this.type = type;
    }

    @Override
    public String toString(){
        return("{S, Name:" + name + " Codes: " + Arrays.toString(schemeCodes.toArray()) + "}");
    }

    public List<String> getSchemeCodes(){
        return schemeCodes;
    }

    public int getLength(){
        return length;
    }

    public String getName() {
        return name;
    }

}
