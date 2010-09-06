/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.updatecenter.common.PluginManifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.2
 */
public class PluginMetadata {

  private File sourceFile;
  private String key;
  private String version;
  private String name;
  private String mainClass;
  private String description;
  private String organization;
  private String organizationUrl;
  private String license;
  private String homepage;
  private boolean core;
  private String[] dependencyPaths = new String[0];
  public List<File> deployedFiles = new ArrayList<File>();

  public PluginMetadata() {
  }

  public PluginMetadata(String key, File sourceFile) {
    this.key = key;
    this.sourceFile = sourceFile;
  }

  public File getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(File f) {
    this.sourceFile = f;
  }

  public String getFilename() {
    return sourceFile.getName();
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isCore() {
    return core;
  }

  public PluginMetadata setCore(boolean b) {
    this.core = b;
    return this;
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public void setOrganizationUrl(String organizationUrl) {
    this.organizationUrl = organizationUrl;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public boolean hasKey() {
    return StringUtils.isNotBlank(key);
  }

  public boolean hasMainClass() {
    return StringUtils.isNotBlank(mainClass);
  }


  public void setDependencyPaths(String[] paths) {
    this.dependencyPaths = paths;
  }

  public String[] getDependencyPaths() {
    return dependencyPaths;
  }

  public List<File> getDeployedFiles() {
    return deployedFiles;
  }

  public void addDeployedFile(File file) {
    this.deployedFiles.add(file);
  }

  public boolean isOldManifest() {
    return !hasKey() && hasMainClass();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PluginMetadata that = (PluginMetadata) o;
    return !(key != null ? !key.equals(that.key) : that.key != null);

  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("key", key)
        .append("version", StringUtils.defaultIfEmpty(version, "-"))
        .toString();
  }

  public static PluginMetadata createFromJar(File file, boolean corePlugin) throws IOException {
    PluginManifest manifest = new PluginManifest(file);
    PluginMetadata metadata = new PluginMetadata();
    metadata.setSourceFile(file);
    metadata.setKey(manifest.getKey());
    metadata.setName(manifest.getName());
    metadata.setDescription(manifest.getDescription());
    metadata.setLicense(manifest.getLicense());
    metadata.setOrganization(manifest.getOrganization());
    metadata.setOrganizationUrl(manifest.getOrganizationUrl());
    metadata.setMainClass(manifest.getMainClass());
    metadata.setVersion(manifest.getVersion());
    metadata.setHomepage(manifest.getHomepage());
    metadata.setDependencyPaths(manifest.getDependencies());
    metadata.setCore(corePlugin);
    return metadata;
  }

  public void copyTo(JpaPlugin jpaPlugin) {
    jpaPlugin.setName(getName());
    jpaPlugin.setDescription(getDescription());
    jpaPlugin.setLicense(getLicense());
    jpaPlugin.setOrganization(getOrganization());
    jpaPlugin.setOrganizationUrl(getOrganizationUrl());
    jpaPlugin.setPluginClass(getMainClass());
    jpaPlugin.setVersion(getVersion());
    jpaPlugin.setHomepage(getHomepage());
    jpaPlugin.setCore(isCore());
    jpaPlugin.removeFiles();
    for (File file : getDeployedFiles()) {
      jpaPlugin.createFile(file.getName());
    }
  }
}
