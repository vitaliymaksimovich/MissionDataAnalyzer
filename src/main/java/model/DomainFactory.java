package model;

import parser.block.util.BlockDataAccessor;

/**
 * Централизованное создание доменных объектов из BlockDataAccessor.
 * Если изменится конструктор или добавится поле — правим только здесь.
 */
public final class DomainFactory {

    private DomainFactory() {}

    public static Curse createCurse(BlockDataAccessor data) {
        String name = data.getString("name");
        if (name == null) name = data.getString("n"); // alias в XML
        return new Curse(name, data.getString("threatLevel"));
    }

    public static Sorcerer createSorcerer(BlockDataAccessor data) {
        String name = data.getString("name");
        if (name == null) name = data.getString("n"); // alias в XML
        return new Sorcerer(name, data.getString("rank"));
    }

    public static Technique createTechnique(BlockDataAccessor data) {
        String name = data.getString("name");
        if (name == null) name = data.getString("n"); // alias в XML
        return new Technique(name, data.getString("type"), data.getString("owner"), data.getDouble("damage"));
    }
}
