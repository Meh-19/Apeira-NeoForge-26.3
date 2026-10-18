package com.apeira.core.datagen;

import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Base for one module's language file, written to {@code assets/<namespace>/lang/<locale>.json}.
 *
 * <p>Defaults to {@link #EN_US}; pass another locale to the constructor for each further language.
 */
public abstract class ApeiraLanguageProvider extends LanguageProvider {
    public static final String EN_US = "en_us";

    private final String namespace;

    protected ApeiraLanguageProvider(PackOutput output, String namespace, String locale) {
        super(output, namespace, locale);
        this.namespace = namespace;
    }

    /** Creates an {@code en_us} provider. */
    protected ApeiraLanguageProvider(PackOutput output, String namespace) {
        this(output, namespace, EN_US);
    }

    public String namespace() {
        return namespace;
    }

    /** Adds {@code <type>.<namespace>.<path>}, the key shape vanilla uses for most registry objects. */
    protected void addId(String type, String path, String value) {
        add(ApeiraIds.id(namespace, path).toLanguageKey(type), value);
    }

    /** Adds a block's name. Unlike {@link #addBlock}, accepts any supplier of a subtype, such as a {@code DeferredBlock}. */
    protected void block(Supplier<? extends Block> block, String name) {
        add(block.get(), name);
    }

    protected void item(Supplier<? extends Item> item, String name) {
        add(item.get(), name);
    }

    /** Adds the title of a creative tab, whose key is {@code itemGroup.<namespace>.<path>}. */
    protected void creativeTab(String path, String name) {
        addId("itemGroup", path, name);
    }

    /** Adds a tooltip line, keyed {@code tooltip.<namespace>.<path>}. */
    protected void tooltip(String path, String value) {
        addId("tooltip", path, value);
    }

    /** Adds the title of a screen or menu, keyed {@code container.<namespace>.<path>}. */
    protected void container(String path, String value) {
        addId("container", path, value);
    }

    /** Adds a chat or HUD message, keyed {@code message.<namespace>.<path>}. */
    protected void message(String path, String value) {
        addId("message", path, value);
    }
}
