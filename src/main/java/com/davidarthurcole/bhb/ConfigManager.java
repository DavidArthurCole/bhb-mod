package com.davidarthurcole.bhb;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private File configFile;
    private final String ender;
    private List<Scheme> passBy;

    public ConfigManager(String ender, List<Scheme> passBy){
        this.passBy = passBy;
        this.ender = ender;
    }

    public void prepareFile(){
        if(configFile == null) configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), Bhb.MOD_ID + this.ender);
    }

    public int deleteScheme(String name){
        prepareFile();
        int flagIndex = -1;

        for(Scheme s : this.passBy){
            if(s.getName().equals(name)){
                flagIndex = this.passBy.indexOf(s);
            }
        }

        if(flagIndex != -1) {
            this.passBy.remove(flagIndex);
            this.saveFile();
            return 1;
        }
        else return 0;
    }

    protected List<Scheme> loadFile(){
        this.prepareFile();
        try{
            this.passBy = new ArrayList<>();
            if(!configFile.exists()) this.saveFile();
            else{
                BufferedReader br = new BufferedReader(new FileReader(configFile));
                JsonObject schemeJson = new JsonParser().parse(br).getAsJsonObject();

                JsonArray jsonArray = schemeJson.get("schemes").getAsJsonArray();

                for (JsonElement jsonElement : jsonArray) {
                    JsonObject scheme = jsonElement.getAsJsonObject();
                    String name = scheme.get("name").getAsString();

                    JsonArray codesAsJson = scheme.get("codes").getAsJsonArray();
                    List<String> codesAsList = new ArrayList<>();
                    for(JsonElement s : codesAsJson) codesAsList.add(s.getAsString().replace("\"", ""));
                    String[] codes = codesAsList.toArray(new String[0]);
                    passBy.add(new Scheme(name, codes));
                }
            }
        }
        catch(FileNotFoundException e){
            System.err.println("[BHB] Could not load schemes configuration file.");
        }

        return passBy;
    }

    protected void saveFile(){
        this.prepareFile();
        JsonObject schemeConfig = new JsonObject();
        JsonArray schemesToSave = new JsonArray();

        if(this.passBy != null){
            for(Scheme s : this.passBy){
                JsonObject scheme = new JsonObject();
                scheme.addProperty("name", s.getName());
                JsonArray codeArray = new Gson().toJsonTree(s.getSchemeCodes()).getAsJsonArray();
                scheme.add("codes", codeArray);
                schemesToSave.add(scheme);
            }
        }
        schemeConfig.add("schemes", schemesToSave);

        String jsonString = new Gson().toJson(schemeConfig);

        try(FileWriter fileWriter = new FileWriter(configFile, false)){
            fileWriter.write(jsonString);
        }
        catch(IOException e){
            System.err.println("[BHB] Could not save to schemes json file.");
        }
    }
}
