package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class BlnsTest {
    private static final ObjectMapper parser;
    private static final ObjectMapper mapper;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    static {
        // use same serialization mapper as `LocalEncryptedRecord`
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        parser = new ObjectMapper();
        parser.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    }

    private File loadFile(String filename) throws IOException {
        URL resource = getClass().getResource(filename);
        return new File(resource.getPath());
    }

    static List<String> compareBlnsResults(List<Map<String, String>> results1, List<Map<String, String>> results2) {
        List<String> failedTests = new ArrayList<>();
        int index = 0;
        for (Map<String, String> t1 : results1) {
            Map<String, String> t2 = results2.get(index);
            String serialized1 = t1.get("serialized");
            String serialized2 = t2.get("serialized");

            if (!Objects.equals(serialized1, serialized2)) {
                failedTests.add(t1.get("index"));
            }
            ++index;
        }
        return failedTests;
    }

    List<Map<String, String>> loadBlnsResults(String jsonFilename) throws IOException {
        File file = loadFile(jsonFilename);
        return parser.readValue(file, new TypeReference<List<Map<String, String>>>(){});
    }

    List<Map<String, String>> serializeBlns() throws IOException {
        File file = loadFile("/com/tozny/e3db/blns.json");
        List<String> strings = parser.readValue(file, new TypeReference<List<String>>(){});

        List<Map<String, String>> results = new ArrayList<>();
        int index = 0;
        for (String element : strings) {
            String elementIdx = String.valueOf(index);
            Map<String, String> plain = new HashMap<>();
            plain.put(elementIdx, element);

            String serialized = mapper.writeValueAsString(plain);
            String b64encoded = Base64.encode(serialized.getBytes(UTF8));

            Map<String, String> result = new HashMap<>();
            result.put("index", elementIdx);
            result.put("element", element);
            result.put("serialized", serialized);
            result.put("b64Encoded", b64encoded);

            results.add(result);
            ++index;
        }

        // uncomment to print to system out
//        String output = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
//        System.out.println(output);

        return results;
    }
}
