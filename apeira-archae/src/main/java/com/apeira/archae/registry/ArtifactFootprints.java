package com.apeira.archae.registry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.apeira.decor.multiblock.Footprint;

/**
 * Where each artifact's cubes are as it is drawn, for apeira-decor's multiblock {@link Footprint}: pixel boxes
 * relative to the artifact's own block (0-16 is that block, so a box may reach into the blocks around it), at rotation
 * 0 (for a wall piece, facing south) and at rotation 1, from the model's cubes: a cube lying along the axes is one box
 * exactly, a turned one is cut into 2 pixel steps. Floor pieces stop at the floor, hanging pieces at the ceiling and
 * wall pieces at the wall; flat parts are a pixel thick. Each artifact also has its outline: the edges of its cubes,
 * turned as drawn, where they are a crease of the model's outside.
 *
 * <p>The table is {@code apeira_archae/artifact_footprints.txt} in this mod's resources, written by
 * {@code scripts/artifact_footprints.py} from the artifacts' models, which repeats the renderers' transforms; run it
 * again after changing a model. Each line is {@code name|straight|diagonal|outline}, each list {@code x1 y1 z1 x2 y2 z2}
 * boxes (or lines) separated by commas. Lines are read when the class loads and parsed when an artifact first asks for its footprint.
 */
public final class ArtifactFootprints {
    private static final String TABLE = "/apeira_archae/artifact_footprints.txt";
    private static final Map<String, String[]> LINES = read();
    private static final Map<String, Footprint> BY_NAME = new ConcurrentHashMap<>();

    private ArtifactFootprints() {}

    /** The footprint of the artifact {@code name}, or a single full block for one not in the table. */
    public static Footprint of(String name) {
        return BY_NAME.computeIfAbsent(name, key -> {
            String[] boxes = LINES.get(key);
            return boxes == null ? Footprint.BLOCK
                    : Footprint.of(Footprint.parse(boxes[0]), Footprint.parse(boxes[1]), Footprint.parseLines(boxes[2]));
        });
    }

    private static Map<String, String[]> read() {
        Map<String, String[]> lines = new HashMap<>();
        try (InputStream stream = ArtifactFootprints.class.getResourceAsStream(TABLE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing the artifact footprint table " + TABLE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                if (fields.length != 3 && fields.length != 4) {
                    throw new IllegalStateException("A footprint line needs a name, two box lists and an outline: " + line);
                }
                lines.put(fields[0], new String[] {fields[1], fields[2], fields.length == 4 ? fields[3] : ""});
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + TABLE, e);
        }
        return lines;
    }
}
