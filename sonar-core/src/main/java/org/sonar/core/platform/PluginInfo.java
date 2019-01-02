/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.Version;

import static java.util.Objects.requireNonNull;

public class PluginInfo implements Comparable<PluginInfo> {

  private static final Joiner SLASH_JOINER = Joiner.on(" / ").skipNulls();

  public static class RequiredPlugin {

    private static final Pattern PARSER = Pattern.compile("\\w+:.+");

    private final String key;
    private final Version minimalVersion;

    public RequiredPlugin(String key, Version minimalVersion) {
      this.key = key;
      this.minimalVersion = minimalVersion;
    }

    public String getKey() {
      return key;
    }

    public Version getMinimalVersion() {
      return minimalVersion;
    }

    public static RequiredPlugin parse(String s) {
      if (!PARSER.matcher(s).matches()) {
        throw new IllegalArgumentException("Manifest field does not have correct format: " + s);
      }
      String[] fields = StringUtils.split(s, ':');
      return new RequiredPlugin(fields[0], Version.create(fields[1]).removeQualifier());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RequiredPlugin that = (RequiredPlugin) o;
      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public String toString() {
      return new StringBuilder().append(key).append(':').append(minimalVersion.getName()).toString();
    }
  }

  private final String key;
  private String name;

  @CheckForNull
  private File jarFile;

  @CheckForNull
  private String mainClass;

  @CheckForNull
  private Version version;

  private String displayVersion;

  @CheckForNull
  private Version minimalSqVersion;

  @CheckForNull
  private String description;

  @CheckForNull
  private String organizationName;

  @CheckForNull
  private String organizationUrl;

  @CheckForNull
  private String license;

  @CheckForNull
  private String homepageUrl;

  @CheckForNull
  private String issueTrackerUrl;

  private boolean useChildFirstClassLoader;

  @CheckForNull
  private String basePlugin;

  @CheckForNull
  private String implementationBuild;

  @CheckForNull
  private boolean sonarLintSupported;

  private final Set<RequiredPlugin> requiredPlugins = new HashSet<>();

  public PluginInfo(String key) {
    requireNonNull(key, "Plugin key is missing from manifest");
    this.key = key;
    this.name = key;
  }

  public PluginInfo setJarFile(@Nullable File f) {
    this.jarFile = f;
    return this;
  }

  @CheckForNull
  public File getJarFile() {
    return jarFile;
  }

  public File getNonNullJarFile() {
    requireNonNull(jarFile);
    return jarFile;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public Version getVersion() {
    return version;
  }

  @CheckForNull
  public String getDisplayVersion() {
    return displayVersion;
  }

  public PluginInfo setDisplayVersion(@Nullable String displayVersion) {
    this.displayVersion = displayVersion;
    return this;
  }

  @CheckForNull
  public Version getMinimalSqVersion() {
    return minimalSqVersion;
  }

  @CheckForNull
  public String getMainClass() {
    return mainClass;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  @CheckForNull
  public String getOrganizationName() {
    return organizationName;
  }

  @CheckForNull
  public String getOrganizationUrl() {
    return organizationUrl;
  }

  @CheckForNull
  public String getLicense() {
    return license;
  }

  @CheckForNull
  public String getHomepageUrl() {
    return homepageUrl;
  }

  @CheckForNull
  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public boolean isUseChildFirstClassLoader() {
    return useChildFirstClassLoader;
  }

  public boolean isSonarLintSupported() {
    return sonarLintSupported;
  }

  @CheckForNull
  public String getBasePlugin() {
    return basePlugin;
  }

  @CheckForNull
  public String getImplementationBuild() {
    return implementationBuild;
  }

  public Set<RequiredPlugin> getRequiredPlugins() {
    return requiredPlugins;
  }

  public PluginInfo setName(@Nullable String name) {
    this.name = (name != null ? name : this.key);
    return this;
  }

  public PluginInfo setVersion(Version version) {
    this.version = version;
    return this;
  }

  public PluginInfo setMinimalSqVersion(@Nullable Version v) {
    this.minimalSqVersion = v;
    return this;
  }

  /**
   * Required
   */
  public PluginInfo setMainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  public PluginInfo setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public PluginInfo setOrganizationName(@Nullable String s) {
    this.organizationName = s;
    return this;
  }

  public PluginInfo setOrganizationUrl(@Nullable String s) {
    this.organizationUrl = s;
    return this;
  }

  public PluginInfo setLicense(@Nullable String license) {
    this.license = license;
    return this;
  }

  public PluginInfo setHomepageUrl(@Nullable String s) {
    this.homepageUrl = s;
    return this;
  }

  public PluginInfo setIssueTrackerUrl(@Nullable String s) {
    this.issueTrackerUrl = s;
    return this;
  }

  public PluginInfo setUseChildFirstClassLoader(boolean b) {
    this.useChildFirstClassLoader = b;
    return this;
  }

  public PluginInfo setSonarLintSupported(boolean sonarLintPlugin) {
    this.sonarLintSupported = sonarLintPlugin;
    return this;
  }

  public PluginInfo setBasePlugin(@Nullable String s) {
    if ("l10nen".equals(s)) {
      Loggers.get(PluginInfo.class).info("Plugin [{}] defines 'l10nen' as base plugin. " +
        "This metadata can be removed from manifest of l10n plugins since version 5.2.", key);
      basePlugin = null;
    } else {
      basePlugin = s;
    }
    return this;
  }

  public PluginInfo setImplementationBuild(@Nullable String implementationBuild) {
    this.implementationBuild = implementationBuild;
    return this;
  }

  public PluginInfo addRequiredPlugin(RequiredPlugin p) {
    this.requiredPlugins.add(p);
    return this;
  }

  /**
   * Find out if this plugin is compatible with a given version of SonarQube.
   * The version of SQ must be greater than or equal to the minimal version
   * needed by the plugin.
   */
  public boolean isCompatibleWith(String runtimeVersion) {
    if (null == this.minimalSqVersion) {
      // no constraint defined on the plugin
      return true;
    }

    Version effectiveMin = Version.create(minimalSqVersion.getName()).removeQualifier();
    Version effectiveVersion = Version.create(runtimeVersion).removeQualifier();

    if (runtimeVersion.endsWith("-SNAPSHOT")) {
      // check only the major and minor versions (two first fields)
      effectiveMin = Version.create(effectiveMin.getMajor() + "." + effectiveMin.getMinor());
    }

    return effectiveVersion.compareTo(effectiveMin) >= 0;
  }

  @Override
  public String toString() {
    return String.format("[%s]", SLASH_JOINER.join(key, version, implementationBuild));
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PluginInfo info = (PluginInfo) o;
    if (!key.equals(info.key)) {
      return false;
    }
    return !(version != null ? !version.equals(info.version) : info.version != null);

  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  @Override
  public int compareTo(PluginInfo that) {
    return ComparisonChain.start()
      .compare(this.name, that.name)
      .compare(this.version, that.version, Ordering.natural().nullsFirst())
      .result();
  }

  public static PluginInfo create(File jarFile) {
    try {
      PluginManifest manifest = new PluginManifest(jarFile);
      return create(jarFile, manifest);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to extract plugin metadata from file: " + jarFile, e);
    }
  }

  @VisibleForTesting
  static PluginInfo create(File jarFile, PluginManifest manifest) {
    if (StringUtils.isBlank(manifest.getKey())) {
      throw MessageException.of(String.format("File is not a plugin. Please delete it and restart: %s", jarFile.getAbsolutePath()));
    }
    PluginInfo info = new PluginInfo(manifest.getKey());

    info.setJarFile(jarFile);
    info.setName(manifest.getName());
    info.setMainClass(manifest.getMainClass());
    info.setVersion(Version.create(manifest.getVersion()));

    // optional fields
    info.setDescription(manifest.getDescription());
    info.setLicense(manifest.getLicense());
    info.setOrganizationName(manifest.getOrganization());
    info.setOrganizationUrl(manifest.getOrganizationUrl());
    info.setDisplayVersion(manifest.getDisplayVersion());
    String minSqVersion = manifest.getSonarVersion();
    if (minSqVersion != null) {
      info.setMinimalSqVersion(Version.create(minSqVersion));
    }
    info.setHomepageUrl(manifest.getHomepage());
    info.setIssueTrackerUrl(manifest.getIssueTrackerUrl());
    info.setUseChildFirstClassLoader(manifest.isUseChildFirstClassLoader());
    info.setSonarLintSupported(manifest.isSonarLintSupported());
    info.setBasePlugin(manifest.getBasePlugin());
    info.setImplementationBuild(manifest.getImplementationBuild());
    String[] requiredPlugins = manifest.getRequirePlugins();
    if (requiredPlugins != null) {
      Arrays.stream(requiredPlugins)
        .map(RequiredPlugin::parse)
        .filter(t -> !"license".equals(t.key))
        .forEach(info::addRequiredPlugin);
    }
    return info;
  }
}
