package com.davidarthurcole.bhb;

public class Color {

    private String mainName;
    private String alias;
    public String hexCode;

    public Color(String mainName, String hexCode, String alias){
        this.mainName = mainName;
        this.alias = alias;
        this.hexCode = hexCode;
    }

    @Override
    public String toString(){
        return mainName;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof String) return (alias.equals(o) || o.equals(mainName));
        System.out.println("Instance checked against color was not a String");
        return false;
    }

}
