package org.ajoberstar.reckon.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "reckon",
    description = "Infer a project's version from your Git repository.",
    versionProvider = ReckonVersionProvider.class,
    mixinStandardHelpOptions = true,
    subcommands = {InferCommand.class, ParseCommand.class})
public class Main implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    System.out.println("Hello Graal!");
    return 0;
  }

  public static void main(String[] args) {
    CommandLine cli = new CommandLine(new Main());
    int exitCode = cli.execute(args);
    System.exit(exitCode);
  }
}
