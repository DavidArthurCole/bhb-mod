package com.davidarthurcole.bhb;

import java.util.Arrays;

public class Scheme {

    private final String name;
    private final String[] schemeCodes;
    private final int length;

    public Scheme(String name, String[] codes){
        this.name = name;
        this.schemeCodes = codes.clone();
        this.length = schemeCodes.length;
    }

    @Override
    public String toString(){
        return("{S, Name:" + name + " Codes: " + Arrays.toString(schemeCodes) + "}");
    }

    public String[] getSchemeCodes(){
        return schemeCodes;
    }

    public int getLength(){
        return length;
    }

    public String getName() {
        return name;
    }

}
