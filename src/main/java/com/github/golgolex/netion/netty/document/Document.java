package com.github.golgolex.netion.netty.document;

import com.google.gson.*;
import com.github.golgolex.netion.gson.FileTypeAdapter;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Tareko on 21.05.2017.
 */
public class Document implements DocumentAbstract {

    public static Gson GSON = new GsonBuilder()
                                                .registerTypeAdapter(File.class, new FileTypeAdapter())
                                                .serializeNulls()
                                                .disableHtmlEscaping()
                                                .create();
    public static Gson PRETTY_PRINTING_GSON = new GsonBuilder()
                                                                .registerTypeAdapter(File.class, new FileTypeAdapter())
                                                                .serializeNulls()
                                                                .setPrettyPrinting()
                                                                .disableHtmlEscaping()
                                                                .create();

    @Getter
    @Setter
    protected String name;
    protected JsonObject dataCatcher;
    @Getter
    @Setter
    private File file;

    public Document(String name) {
        this.name = name;
        this.dataCatcher = new JsonObject();
    }

    public Document(String name, JsonObject source) {
        this.name = name;
        this.dataCatcher = source;
    }

    public Document(File file, JsonObject jsonObject) {
        this.file = file;
        this.dataCatcher = jsonObject;
    }

    public Document(String key, String value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public Document append(String key, String value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    public Document append(String key, Number value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    public Document append(String key, Boolean value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    @Override
    public Document append(String key, JsonElement value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.add(key, value);
        return this;
    }

    @Deprecated
    public Document append(String key, Object value) {
        if (value == null) {
            return this;
        }
        if (value instanceof Document) {
            this.append(key, (Document) value);
            return this;
        }
        this.dataCatcher.add(key, PRETTY_PRINTING_GSON.toJsonTree(value));
        return this;
    }

    @Override
    public Document remove(String key) {
        this.dataCatcher.remove(key);
        return this;
    }

    public Set<String> keys() {
        Set<String> c = new HashSet<>();

        for (Map.Entry<String, JsonElement> x : dataCatcher.entrySet()) {
            c.add(x.getKey());
        }

        return c;
    }

    public String getString(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return dataCatcher.get(key).getAsString();
    }

    public int getInt(String key) {
        if (!dataCatcher.has(key)) {
            return 0;
        }
        return dataCatcher.get(key).getAsInt();
    }

    public long getLong(String key) {
        if (!dataCatcher.has(key)) {
            return 0L;
        }
        return dataCatcher.get(key).getAsLong();
    }

    public double getDouble(String key) {
        if (!dataCatcher.has(key)) {
            return 0D;
        }
        return dataCatcher.get(key).getAsDouble();
    }

    public boolean getBoolean(String key) {
        if (!dataCatcher.has(key)) {
            return false;
        }
        return dataCatcher.get(key).getAsBoolean();
    }

    public float getFloat(String key) {
        if (!dataCatcher.has(key)) {
            return 0F;
        }
        return dataCatcher.get(key).getAsFloat();
    }

    public short getShort(String key) {
        if (!dataCatcher.has(key)) {
            return 0;
        }
        return dataCatcher.get(key).getAsShort();
    }

    public String convertToJson() {
        return PRETTY_PRINTING_GSON.toJson(dataCatcher);
    }

    public boolean saveAsConfig(File backend) {
        if (backend == null) {
            return false;
        }

        if (backend.exists()) {
            backend.delete();
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(backend), "UTF-8")) {
            PRETTY_PRINTING_GSON.toJson(dataCatcher, (writer));
            return true;
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return false;
    }

    public boolean saveAsConfig(String path) {
        return saveAsConfig(Paths.get(path));
    }

    public Document getDocument(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return new Document(dataCatcher.get(key).getAsJsonObject());
    }

    public boolean saveAsConfig(Path path) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(path), "UTF-8")) {
            PRETTY_PRINTING_GSON.toJson(dataCatcher, outputStreamWriter);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Document(String key, Object value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public Document append(String key, Document value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.add(key, value.dataCatcher);
        return this;
    }

    public Document(String key, Number value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public Document(Document defaults) {
        this.dataCatcher = defaults.dataCatcher;
    }

    public Document(Document defaults, String name) {
        this.dataCatcher = defaults.dataCatcher;
        this.name = name;
    }

    public Document() {
        this.dataCatcher = new JsonObject();
    }

    public Document(JsonObject source) {
        this.dataCatcher = source;
    }

    public static Document loadDocument(File backend) {
        return loadDocument(backend.toPath());
    }

    public static Document loadDocument(Path backend) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(backend),
                StandardCharsets.UTF_8); BufferedReader bufferedReader = new BufferedReader(reader)) {
            JsonObject object = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            return new Document(object);
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new Document();
    }

    public static Document $loadDocument(File backend) throws Exception {
        try {
            return new Document(JsonParser.parseString(Files.readString(backend.toPath())).getAsJsonObject());
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    public static Document load(String input) {
        try (InputStreamReader reader = new InputStreamReader(new StringBufferInputStream(input), StandardCharsets.UTF_8)) {
            return new Document(JsonParser.parseReader(new BufferedReader(reader)).getAsJsonObject());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Document();
    }

    public static Document load(JsonObject input) {
        return new Document(input);
    }

    public JsonObject obj() {
        return dataCatcher;
    }

    public Document append(String key, List<String> value) {
        if (value == null) {
            return this;
        }
        JsonArray jsonElements = new JsonArray();

        for (String b : value) {
            jsonElements.add(b);
        }

        this.dataCatcher.add(key, jsonElements);
        return this;
    }

    public Document appendValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> valuess : values.entrySet()) {
            append(valuess.getKey(), valuess.getValue());
        }
        return this;
    }

    public JsonElement get(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return dataCatcher.get(key);
    }

    public <T> T getObject(String key, Class<T> class_) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        JsonElement element = dataCatcher.get(key);
        return PRETTY_PRINTING_GSON.fromJson(element, class_);
    }

    public Document clear() {
        for (String key : keys()) {
            remove(key);
        }
        return this;
    }

    public int size() {
        return this.dataCatcher.size();
    }

    public Document loadProperties(Properties properties) {
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            Object x = enumeration.nextElement();
            this.append(x.toString(), properties.getProperty(x.toString()));
        }
        return this;
    }

    public boolean isEmpty() {
        return this.dataCatcher.isEmpty();
    }

    public JsonArray getArray(String key) {
        return dataCatcher.get(key).getAsJsonArray();
    }

    @Deprecated
    public boolean saveAsConfig0(File backend) {
        if (backend == null) {
            return false;
        }

        if (backend.exists()) {
            backend.delete();
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(backend), StandardCharsets.UTF_8)) {
            GSON.toJson(dataCatcher, (writer));
            return true;
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return false;
    }

    public Document loadToExistingDocument(File backend) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(backend), StandardCharsets.UTF_8)) {

            this.dataCatcher = JsonParser.parseReader(reader).getAsJsonObject();
            this.file = backend;
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new Document();
    }

    public Document loadToExistingDocument(Path path) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {

            this.dataCatcher = JsonParser.parseReader(reader).getAsJsonObject();
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new Document();
    }

    @Override
    public String toString() {
        return convertToJsonString();
    }

    public String convertToJsonString() {
        return dataCatcher.toString();
    }

    public <T> T getObject(String key, Type type) {
        if (!contains(key)) {
            return null;
        }

        return PRETTY_PRINTING_GSON.fromJson(dataCatcher.get(key), type);
    }

    public boolean contains(String key) {
        return this.dataCatcher.has(key);
    }

    public byte[] toBytesAsUTF_8() {
        return convertToJsonString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toBytes() {
        return convertToJsonString().getBytes();
    }
}
