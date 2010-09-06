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
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Release implements Comparable<Release> {

  private Artifact artifact;
  private Version version;
  private String description;
  private String downloadUrl;
  private String changelogUrl;

  /** from oldest to newest sonar versions */
  private SortedSet<Version> requiredSonarVersions = new TreeSet<Version>();
  private Date date;

  public Release(Artifact artifact, Version version) {
    this.artifact = artifact;
    this.version = version;
  }

  public Release(Artifact artifact, String version) {
    this.artifact = artifact;
    this.version = Version.create(version);
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public Version getVersion() {
    return version;
  }

  public Release setVersion(Version version) {
    this.version = version;
    return this;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public Release setDownloadUrl(String s) {
    this.downloadUrl = s;
    return this;
  }

  public String getFilename() {
    return StringUtils.substringAfterLast(downloadUrl, "/");
  }

  public SortedSet<Version> getRequiredSonarVersions() {
    return requiredSonarVersions;
  }

  public boolean supportSonarVersion(Version version) {
    return requiredSonarVersions.contains(version);
  }

  public Release addRequiredSonarVersions(Version... versions) {
    if (versions!=null) {
      requiredSonarVersions.addAll(Arrays.asList(versions));
    }
    return this;
  }

  public Release addRequiredSonarVersions(String... versions) {
    if (versions!=null) {
      for (String v : versions) {
        requiredSonarVersions.add(Version.create(v));
      }
    }
    return this;
  }

  public Version getLastRequiredSonarVersion() {
    if (requiredSonarVersions!=null && !requiredSonarVersions.isEmpty()) {
      return requiredSonarVersions.last();
    }
    return null;
  }

  public Date getDate() {
    return date;
  }

  public Release setDate(Date date) {
    this.date = date;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getChangelogUrl() {
    return changelogUrl;
  }

  public void setChangelogUrl(String changelogUrl) {
    this.changelogUrl = changelogUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Release that = (Release) o;
    return version.equals(that.version);

  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("version", version)
        .append("downloadUrl", downloadUrl)
        .append("changelogUrl", changelogUrl)
        .append("description", description)
        .toString();
  }

  public int compareTo(Release o) {
    return getVersion().compareTo(o.getVersion());
  }
}
