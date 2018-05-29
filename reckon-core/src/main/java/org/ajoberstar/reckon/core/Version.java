package org.ajoberstar.reckon.core;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.zafarkhaja.semver.ParseException;

public final class Version implements Comparable<Version> {
  public static final Version IDENTITY = new Version(com.github.zafarkhaja.semver.Version.forIntegers(0, 0, 0));

  private final com.github.zafarkhaja.semver.Version version;
  private final Version normal;
  private final Stage stage;

  private Version(com.github.zafarkhaja.semver.Version version) {
    this.version = version;
    if (version.getPreReleaseVersion().isEmpty()) {
      this.normal = this;
    } else {
      this.normal = new Version(com.github.zafarkhaja.semver.Version.forIntegers(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion()));
    }
    this.stage = Stage.valueOf(version);
  }

  com.github.zafarkhaja.semver.Version getVersion() {
    return version;
  }

  public Version getNormal() {
    return normal;
  }

  public Optional<Stage> getStage() {
    return Optional.ofNullable(stage);
  }

  public boolean isFinal() {
    return version.getPreReleaseVersion().isEmpty();
  }

  public boolean isSignificant() {
    return isFinal() || getStage()
        .filter(stage -> !"SNAPSHOT".equals(stage.getName()))
        .filter(stage -> version.getBuildMetadata().isEmpty())
        .isPresent();
  }

  public Version incrementNormal(Scope scope) {
    switch (scope) {
      case MAJOR:
        return new Version(version.incrementMajorVersion());
      case MINOR:
        return new Version(version.incrementMinorVersion());
      case PATCH:
        return new Version(version.incrementPatchVersion());
      default:
        throw new AssertionError("Invalid scope: " + scope);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    } else if (obj instanceof Version) {
      return Objects.equals(this.version, ((Version) obj).version);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }

  @Override
  public int compareTo(Version that) {
    return this.version.compareTo(that.version);
  }

  @Override
  public String toString() {
    return version.toString();
  }

  public static final class Stage {
    private static final Pattern STAGE_REGEX = Pattern.compile("^(?<name>\\w+)(?:\\.(?<num>\\d+))?");

    private final String name;
    private final int num;

    private Stage(String name, int num) {
      this.name = name;
      this.num = num;
    }

    public String getName() {
      return name;
    }

    public int getNum() {
      return num;
    }

    private static Stage valueOf(com.github.zafarkhaja.semver.Version version) {
      Matcher matcher = STAGE_REGEX.matcher(version.getPreReleaseVersion());
      if (matcher.find()) {
        String name = matcher.group("name");
        int num = Optional.ofNullable(matcher.group("num")).map(Integer::parseInt).orElse(0);
        return new Stage(name, num);
      } else {
        return null;
      }
    }
  }

  public static Version valueOf(String versionString) {
    return new Version(com.github.zafarkhaja.semver.Version.valueOf(versionString));
  }

  public static Optional<Version> parse(String versionString) {
    try {
      return Optional.of(new Version(com.github.zafarkhaja.semver.Version.valueOf(versionString)));
    } catch (IllegalArgumentException | ParseException e) {
      return Optional.empty();
    }
  }
}
