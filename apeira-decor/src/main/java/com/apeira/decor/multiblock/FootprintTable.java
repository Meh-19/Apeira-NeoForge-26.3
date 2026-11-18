package com.apeira.decor.multiblock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A table of {@link Footprint}s by block name, in the format {@code scripts/artifact_footprints.py} writes: one
 * {@code name|straight|diagonal|outline} line per piece, pixel boxes relative to the piece's own block at rotation 0 and
 * 1, then the outline's lines; {@code #} lines are comments. Lines are read when the table is made and parsed when a
 * piece first asks for its footprint.
 */
public final class FootprintTable {
    private final String resource;
    private final Map<String, String[]> lines;
    private final Map<String, Footprint> byName = new ConcurrentHashMap<>();

    private FootprintTable(String resource, Map<String, String[]> lines) {
        this.resource = resource;
        this.lines = lines;
    }

    /** Reads the table at the class path {@code resource}; a missing table is an empty one. */
    public static FootprintTable read(String resource) {
        Map<String, String[]> lines = new HashMap<>();
        try (InputStream stream = FootprintTable.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return new FootprintTable(resource, Map.of());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("[|]", -1);
                if (fields.length != 3 && fields.length != 4) {
                    throw new IllegalStateException(resource + ": a footprint line needs a name, two box lists and an outline: " + line);
                }
                lines.put(fields[0], new String[] {fields[1], fields[2], fields.length == 4 ? fields[3] : ""});
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
        return new FootprintTable(resource, Map.copyOf(lines));
    }

    /** The footprint of {@code name}, if the table has it. */
    public Optional<Footprint> get(String name) {
        if (!lines.containsKey(name)) {
            return Optional.empty();
        }
        return Optional.of(byName.computeIfAbsent(name, key -> {
            String[] boxes = lines.get(key);
            return Footprint.of(Footprint.parse(boxes[0]), Footprint.parse(boxes[1]), Footprint.parseLines(boxes[2]));
        }));
    }

    @Override
    public String toString() {
        return "FootprintTable[" + resource + ", " + lines.size() + " pieces]";
    }
}
