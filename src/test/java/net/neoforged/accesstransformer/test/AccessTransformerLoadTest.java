package net.neoforged.accesstransformer.test;

import com.google.gson.*;
import com.google.gson.reflect.*;

import net.neoforged.accesstransformer.AccessTransformer;
import net.neoforged.accesstransformer.AccessTransformerEngine;
import net.neoforged.accesstransformer.service.*;

import java.nio.charset.*;

import net.neoforged.accesstransformer.parser.AccessTransformerList;
import net.neoforged.accesstransformer.service.AccessTransformerService;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.powermock.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

public class AccessTransformerLoadTest {
    @AfterEach
    public void cleanUp() {
        Whitebox.setInternalState(AccessTransformerEngine.INSTANCE, "masterList", new AccessTransformerList());
    }

    @Test
    public void testLoadForgeAT() throws IOException, URISyntaxException {
        final AccessTransformerList atLoader = new AccessTransformerList();
        atLoader.loadFromResource("forge_at.cfg");
        final Map<String, List<AccessTransformer>> accessTransformers = atLoader.getAccessTransformers();
        testText(accessTransformers);
    }

    @Test
    public void testLoadATFromJar() throws IOException, URISyntaxException {
        final AccessTransformerService mls = new AccessTransformerService();
        try (final FileSystem jarFS = FileSystems.newFileSystem(FileSystems.getDefault().getPath("src","test","resources","testatmod.jar"), getClass().getClassLoader())) {
            final Path atPath = jarFS.getPath("META-INF", "forge_at.cfg");
            mls.offerResource(atPath,"forge_at.cfg");
            final AccessTransformerList list = Whitebox.getInternalState(AccessTransformerEngine.INSTANCE, "masterList");
            final Map<String, List<AccessTransformer>> accessTransformers = list.getAccessTransformers();
            testText(accessTransformers);
        }
    }

    private static void testText(final Map<String, List<AccessTransformer>> accessTransformers) throws URISyntaxException, IOException {
        accessTransformers.forEach((k,v) -> System.out.printf("Got %d ATs for %s:\n\t%s\n", v.size(), k, v.stream().map(Object::toString).collect(Collectors.joining("\n\t"))));

        final TreeMap<String, List<String>> testOutput = accessTransformers.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(AccessTransformer::toString).sorted().collect(Collectors.toList()),
                        (l1, l2) -> { throw new RuntimeException("duplicate keys"); },
                        TreeMap::new
                )
        );

        final String text = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemClassLoader().getResource("forge_at.cfg.json").toURI())), StandardCharsets.UTF_8);
        final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        final TreeMap<String, List<String>> expectation = GSON.fromJson(text, new TypeToken<TreeMap<String, List<String>>>() {}.getType());

        final String output = GSON.toJson(testOutput);
        final String expect = GSON.toJson(expectation);

        assertEquals(expect, output);
    }
}
