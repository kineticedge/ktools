package io.kineticedge.tools;


import io.kineticedge.tools.exception.CommandException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public abstract class Subcommand implements Runnable {


    protected Map<String, Object> loadProperties(final File file) {

        if (!file.isFile() || !file.canRead()) {
            throw new CommandException("unable to access file " + file.getAbsolutePath());
        }

        try {
            final Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            }
            return properties.entrySet().stream().map(e -> Map.entry((String) e.getKey(), e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            throw new CommandException(e);
        }
    }
}
