package ch.zhaw.pm2.life.parser;

import ch.zhaw.pm2.life.exception.LifeException;
import ch.zhaw.pm2.life.model.GameObject;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigParser {

    private static final String CONFIG_PATH = "config";
    private static final String FILE_NAME = "config.properties";
    private static final URL templateFile = ConfigParser.class.getClassLoader()
            .getResource(CONFIG_PATH + File.separator + FILE_NAME);
    private static final String DELIMITER = ".";
    private static File configFile = new File(CONFIG_PATH + File.separator + FILE_NAME);
    private static Properties config = new Properties();

    private static ConfigParser instance = null;

    private ConfigParser() throws LifeException {
        try {
            if (Files.notExists(configFile.toPath())) {
                copyConfig();
            }
            config.load(new FileReader(configFile));
        } catch (IOException | URISyntaxException e) {
            throw new LifeException(e);
        }

    }

    public List<GameObject> parseObjects() throws LifeException {
        List<GameObject> parsedObjects = new ArrayList<>();

        Set<String> lifeForms = new HashSet<>();
        Enumeration<Object> property = config.keys();
        while (property.hasMoreElements()) {
            lifeForms.add(property.nextElement().toString().split("\\.")[0]);
        }

        try {
            for (String lifeForm : lifeForms) {
                Class<?> clazz = null;
                Type type = Type.getType(getConfigValue(lifeForm, Options.TYPE.name()));
                if (type != null) {
                    switch (type) {
                        case PLANT_EATER:
                            clazz = Class.forName(Type.PLANT_EATER.value);
                            break;

                        case MEAT_EATER:
                            clazz = Class.forName(Type.MEAT_EATER.value);
                            break;

                        case PLANT:
                            clazz = Class.forName(Type.PLANT.value);
                            break;

                        default:
                            break;
                    }
                }

                if (clazz == null) {
                    throw new LifeException("Could not parse the config file");
                }

                int energy = Integer.parseInt(getConfigValue(lifeForm, Options.ENERGY.name()));
                Color color = Color.valueOf(getConfigValue(lifeForm, Options.COLOR.name()));
                GameObject gameObject = (GameObject) clazz.getConstructor().newInstance();
                gameObject.setColor(color);
                gameObject.setEnergy(energy);

                parsedObjects.add(gameObject);
            }
        } catch (ReflectiveOperationException e) {
            throw new LifeException(e);
        }

        return parsedObjects;
    }

    private String getConfigValue(String lifeForm, String property) {
        return config.get(String.join(DELIMITER, lifeForm, property.toLowerCase())).toString();
    }

    private void copyConfig() throws LifeException, IOException, URISyntaxException {
        File configFolder = new File(CONFIG_PATH);


        if (Files.notExists(configFolder.toPath())) {
            Files.createDirectory(configFolder.toPath());
            Files.copy(Path.of(templateFile.toURI()),
                    new File(configFolder + File.separator + FILE_NAME).toPath());
        } else {
            throw new LifeException("Could not create the config folder");
        }
    }

    public static ConfigParser getInstance() throws LifeException {
        if (instance == null) {
            instance = new ConfigParser();
        }
        return instance;
    }

    private enum Options {
        ENERGY,
        TYPE,
        COLOR
    }

    private enum Type {
        MEAT_EATER("ch.zhaw.pm2.life.model.lifeform.animal.MeatEater"),
        PLANT_EATER("ch.zhaw.pm2.life.model.lifeform.animal.PlantEater"),
        PLANT("ch.zhaw.pm2.life.model.lifeform.plant.FirstPlant");

        private final String value;
        Type(final String v) {
            value = v;
        }

        public static Type getType(String val) {
            for (Type type : Type.values()) {
                String[] values = type.value.split("\\.");
                if (values[values.length - 1].equalsIgnoreCase(val)) {
                    return type;
                }
            }
            return null;
        }

    }
}
