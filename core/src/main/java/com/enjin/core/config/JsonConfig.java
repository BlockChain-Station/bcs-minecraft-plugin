package com.enjin.core.config;

import com.enjin.core.Enjin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class JsonConfig {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static <T extends JsonConfig> T load(File file, Class<T> clazz) {
        JsonConfig config;

        try {
            try {
                if (!file.exists()) {
                    config = clazz.newInstance();
                    config.save(file);
                } else {
                    config = gson.fromJson(new FileReader(file), clazz);
                }
            } catch (IOException e) {
                return clazz.newInstance();
            }
        } catch (ReflectiveOperationException e) {
            return null;
        }

        return config == null ? null : clazz.cast(config);
    }

    public boolean save(File file) {
        try {
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file);
            fw.write(gson.toJson(this));
            fw.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean update(File file, Object data) {
        JsonElement old = gson.toJsonTree(this);
        JsonElement updates = gson.toJsonTree(data);

        if (!old.isJsonObject() && !updates.isJsonObject()) {
            Enjin.getPlugin().debug("Config or data is not a json object.");
            return false;
        }

        JsonObject oldObj = old.getAsJsonObject();
        JsonObject updatesObj = updates.getAsJsonObject();

        update(oldObj, updatesObj);

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(gson.toJson(oldObj));
            fw.close();
        } catch (IOException e) {
            Enjin.getPlugin().debug("Could not write the config.");
            return false;
        }

        return true;
    }

    private void update(JsonObject oldObj, JsonObject update) {
        for (Map.Entry<String, JsonElement> entry : update.getAsJsonObject().entrySet()) {
            if (!oldObj.has(entry.getKey())) {
                Enjin.getPlugin().debug(entry.getKey() + " does not exists, updating value.");
                oldObj.add(entry.getKey(), entry.getValue());
                continue;
            }

            JsonElement element = oldObj.get(entry.getKey());
            if (entry.getValue().isJsonObject()) {
                Enjin.getPlugin().debug(entry.getKey() + " is an object, processing object fields.");
                update(element.getAsJsonObject(), element.getAsJsonObject());
            } else {
                Enjin.getPlugin().debug("Setting " + entry.getKey() + " to " + entry.getValue().toString());
                oldObj.add(entry.getKey(), entry.getValue());
            }
        }
    }
}