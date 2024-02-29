package io.kineticedge.tools;


import io.kineticedge.tools.exception.CommandException;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public abstract class Subcommand implements Runnable {

    @CommandLine.Option(names = { "--help" }, usageHelp = true, hidden = true, description = "this help message.")
    protected boolean helpRequested = false;

}
