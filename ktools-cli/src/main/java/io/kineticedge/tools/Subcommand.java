package io.kineticedge.tools;


import io.kineticedge.tools.exception.CommandException;
import io.kineticedge.tools.util.PropertiesUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public abstract class Subcommand implements Runnable {


    @CommandLine.Option(names = { "--help" }, usageHelp = true, hidden = true, description = "this help message.")
    protected boolean helpRequested = false;

}
