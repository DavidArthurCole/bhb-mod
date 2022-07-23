package com.davidarthurcole.bhb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scheme {

    private final String name;
    private final List<String> schemeCodes;
    private final int length;

    public Scheme(String name, List<String> codes){
        this.name = name;
        this.schemeCodes = new ArrayList<>(codes);
        this.length = schemeCodes.size();
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
