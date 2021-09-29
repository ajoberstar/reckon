package org.ajoberstar.reckon.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ajoberstar.reckon.core.Version;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ParameterException;

@Command(
    name = "parse",
    description = "Parses a reckon compatible version into data.")
public class ParseCommand implements Callable<Integer> {
  @Option(names = "--format", paramLabel = "{human|json|ini|properties}", description = "The format to output in. (default ${DEFAULT-VALUE})")
  String format = "human";

  @Option(names = "--to-file", paramLabel = "PATH", description = "Output data to a file.")
  Path outputFile;

  @Parameters(index = "0", paramLabel = "VERSION", description = "The version to parse.")
  String version;

  @Spec
  CommandSpec spec;

  @Override
  public Integer call() throws Exception {
    Set validFormats = Stream.of("human", "json", "ini", "properties").collect(Collectors.toSet());

    if (!validFormats.contains(format)) {
      throw new ParameterException(spec.commandLine(), "Invalid option: format must be human, json, ini, or properties");
    }

    Optional<Version> parsed = Version.parse(version);
    if (parsed.isPresent()) {
      Version v = parsed.get();
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("version", v.toString());
      data.put("normal", v.getNormal().toString());
      data.put("stage-name", v.getStage().map(Version.Stage::getName).orElse("final"));
      data.put("stage-num", v.getStage().map(Version.Stage::getNum).orElse(0));
      data.put("significant", v.isSignificant());

      String output = format(data);

      if (outputFile == null) {
        System.out.println(output);
      } else {
        Files.write(outputFile, output.getBytes(StandardCharsets.UTF_8));
      }
      return 0;
    } else {
      System.err.println("ERROR: Version \"" + version + "\" is not semver compliant.");
      System.err.println("       Refer to the spec https://semver.org/spec/v2.0.0.html");
      return 1;
    }
  }

  private String format(Map<String, Object> data) throws Exception {
    if ("human".equals(format)) {
      return formatHuman(data);
    } else if ("json".equals(format)) {
      return formatJson(data);
    } else if ("properties".equals(format) || "ini".equals(format)) {
      return formatProperties(data);
    } else {
      // TODO better error
      throw new RuntimeException("Invalid format: " + format);
    }
  }

  private String formatHuman(Map<String, Object> data) {
    return data.entrySet().stream()
        .map(entry -> {
          return String.format("%-12s %s", entry.getKey(), entry.getValue());
        }).collect(Collectors.joining(System.lineSeparator()));
  }

  private String formatJson(Map<String, Object> data) throws Exception {
    return new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .writeValueAsString(data);
  }

  private String formatProperties(Map<String, Object> data) {
    // not using java.util.Properties so that it normalizes EOL and doesn't include a comment
    return data.entrySet().stream()
        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("\n"));
  }
}
