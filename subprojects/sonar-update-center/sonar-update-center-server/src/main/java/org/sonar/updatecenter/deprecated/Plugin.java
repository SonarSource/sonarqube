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
package org.sonar.updatecenter.deprecated;

import org.apache.maven.model.Developer;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Information about Sonar Plugin.
 *
 * @author Evgeny Mandrikov
 */
public class Plugin implements Versioned {
  private String key;
  private String name;
  private String description;
  private String version;
  private String downloadUrl;
  private String requiredSonarVersion;
  private String homepage;
  private long timestamp;

  private String pluginClass;
  private String issueTracker;
  private String sources;
  private String license;

  private List<Developer> developers;

  public Plugin(String pluginKey) {
    this.key = pluginKey;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return name
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return version
   */
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getReleaseDate() {
    return (new SimpleDateFormat("d MMM yyyy")).format(new Date(timestamp));
  }

  private void setDate(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * @return URL for downloading
   */
  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  /**
   * @return minimal Sonar version to run this plugin
   */
  public String getRequiredSonarVersion() {
    // TODO Sonar-Version from MANIFEST.MF
    return requiredSonarVersion;
  }

  public void setRequiredSonarVersion(String sonarVersion) {
    this.requiredSonarVersion = sonarVersion;
  }

  /**
   * @return homepage
   */
  public String getHomepage() {
    // TODO Plugin-Homepage from MANIFEST.MF
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public String getIssueTracker() {
    return issueTracker;
  }

  public void setIssueTracker(String url) {
    this.issueTracker = url;
  }

  public String getSources() {
    return sources;
  }

  public void setSources(String sources) {
    this.sources = sources;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public List<Developer> getDevelopers() {
    return developers;
  }

  public void setDevelopers(List<Developer> developers) {
    this.developers = developers;
  }

  public JSONObject toJsonObject() {
    JSONObject obj = new JSONObject();
    obj.put("id", getKey());
    obj.put("name", getName());
    obj.put("version", getVersion());
    obj.put("sonarVersion", getRequiredSonarVersion());
    if (getDownloadUrl() != null) {
      obj.put("downloadUrl", getDownloadUrl());
    }
    if (getHomepage() != null) {
      obj.put("homepage", getHomepage());
    }
    return obj;
  }

  public static Plugin extractMetadata(File file) throws IOException {
    JarFile jar = new JarFile(file);
    ZipEntry entry = jar.getEntry("META-INF/MANIFEST.MF");
    long timestamp = entry.getTime();
    Manifest manifest = jar.getManifest();
    jar.close();

    Attributes attributes = manifest.getMainAttributes();
    String pluginKey = attributes.getValue("Plugin-Key");
    Plugin plugin = new Plugin(pluginKey);
    plugin.setName(attributes.getValue("Plugin-Name"));
    plugin.setVersion(attributes.getValue("Plugin-Version"));
    plugin.setRequiredSonarVersion(attributes.getValue("Sonar-Version"));
    plugin.setHomepage(attributes.getValue("Plugin-Homepage"));
    plugin.setDate(timestamp);
    return plugin;
  }
}
