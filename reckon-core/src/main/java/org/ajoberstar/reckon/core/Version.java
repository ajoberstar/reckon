package org.ajoberstar.reckon.core;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.zafarkhaja.semver.ParseException;

/**
 * A SemVer-compliant version.
 */
public final class Version implements Comparable<Version> {
  /**
   * A base version for use as a default in cases where you don't have an existing version.
   */
  public static final Version IDENTITY = new Version(com.github.zafarkhaja.semver.Version.forIntegers(0, 0, 0));

  private final com.github.zafarkhaja.semver.Version version;
  private final Version normal;
  private final Stage stage;

  private Version(com.github.zafarkhaja.semver.Version version) {
    this.version = version;
    // need this if logic to avoid stack overflow
    if (version.getPreReleaseVersion().isEmpty()) {
      this.normal = this;
    } else {
      this.normal = new Version(com.github.zafarkhaja.semver.Version.forIntegers(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion()));
    }
    this.stage = Stage.valueOf(version);
  }

  /**
   * This is intentionally package private.
   *
   * @return the internal JSemver version
   */
  com.github.zafarkhaja.semver.Version getVersion() {
    return version;
  }

  /**
   * @return the normal component of the version.
   */
  public Version getNormal() {
    return normal;
  }

  /**
   * @return the stage of this version, if any.
   */
  public Optional<Stage> getStage() {
    return Optional.ofNullable(stage);
  }

  /**
   * @return {@code true} if this is a final version (i.e. doesn't have pre-release information),
   *         {@code false} otherwise
   */
  public boolean isFinal() {
    return version.getPreReleaseVersion().isEmpty();
  }

  /**
   * @return {@code true} if the version is final or any other significant stage, {@code false} if
   *         insignficant or snapshot
   */
  public boolean isSignificant() {
    return isFinal() || getStage()
        .filter(stage -> !"SNAPSHOT".equals(stage.getName()))
        .filter(stage -> version.getBuildMetadata().isEmpty())
        .isPresent();
  }

  /**
   * Increments this version using the given scope to get a new normal version.
   *
   * @param scope the scope to increment the version by
   * @return incremented version, with only the normal component
   */
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

  /**
   * Stage of development.
   */
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
      var matcher = STAGE_REGEX.matcher(version.getPreReleaseVersion());
      if (matcher.find()) {
        var name = matcher.group("name");
        int num = Optional.ofNullable(matcher.group("num")).map(Integer::parseInt).orElse(0);
        return new Stage(name, num);
      } else {
        return null;
      }
    }
  }

  /**
   * Gets the version represented by the given string. If the version is not SemVer compliant, an
   * exception will be thrown. Use {@code parse} if you don't trust that your input is valid.
   *
   * @param versionString version to parse
   * @return the version
   */
  public static Version valueOf(String versionString) {
    try {
      return new Version(com.github.zafarkhaja.semver.Version.valueOf(versionString));
    } catch (IllegalArgumentException | ParseException e) {
      var message = String.format("Invalid version \"%s\": %s", versionString, e.getMessage());
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Gets the version represented by the given string, if it's SemVer compliant. If not, an empty
   * Optional will be returned.
   *
   * @param versionString version to parse
   * @return the version or an empty optional, if the string wasn't SemVer compliant
   */
  public static Optional<Version> parse(String versionString) {
    try {
      return Optional.of(new Version(com.github.zafarkhaja.semver.Version.valueOf(versionString)));
    } catch (IllegalArgumentException | ParseException e) {
      return Optional.empty();
    }
  }
}
