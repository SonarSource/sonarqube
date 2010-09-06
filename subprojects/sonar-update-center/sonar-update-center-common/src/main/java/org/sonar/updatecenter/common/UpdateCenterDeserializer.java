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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import static org.sonar.updatecenter.common.FormatUtils.toDate;

public final class UpdateCenterDeserializer {

  private UpdateCenterDeserializer() {
    // only static methods
  }

  public static UpdateCenter fromProperties(File file) throws IOException {
    FileInputStream in = FileUtils.openInputStream(file);
    try {
      Properties props = new Properties();
      props.load(in);
      UpdateCenter center = fromProperties(props);
      center.setDate(new Date(file.lastModified()));
      return center;

    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static UpdateCenter fromProperties(Properties p) {
    UpdateCenter center = new UpdateCenter();
    center.setDate(FormatUtils.toDate(p.getProperty("date"), true));
    String[] sonarVersions = getArray(p, "sonar.versions");
    for (String sonarVersion : sonarVersions) {
      Release release = new Release(center.getSonar(), sonarVersion);
      release.setChangelogUrl(get(p, "sonar." + sonarVersion + ".changelogUrl"));
      release.setDescription(get(p, "sonar." + sonarVersion + ".description"));
      release.setDownloadUrl(get(p, "sonar." + sonarVersion + ".downloadUrl"));
      release.setDate(FormatUtils.toDate(get(p, "sonar." + sonarVersion + ".date"), true));
      center.getSonar().addRelease(release);
    }

    String[] pluginKeys = getArray(p, "plugins");
    for (String pluginKey : pluginKeys) {
      Plugin plugin = new Plugin(pluginKey);
      center.addPlugin(plugin);
      plugin.setName(get(p, pluginKey, "name"));
      plugin.setDescription(get(p, pluginKey, "description"));
      plugin.setCategory(get(p, pluginKey, "category"));
      plugin.setHomepageUrl(get(p, pluginKey, "homepageUrl"));
      plugin.setLicense(get(p, pluginKey, "license"));
      plugin.setOrganization(get(p, pluginKey, "organization"));
      plugin.setOrganizationUrl(get(p, pluginKey, "organizationUrl"));
      plugin.setTermsConditionsUrl(get(p, pluginKey, "termsConditionsUrl"));
      plugin.setIssueTrackerUrl(get(p, pluginKey, "issueTrackerUrl"));

      String[] pluginReleases = StringUtils.split(StringUtils.defaultIfEmpty(get(p, pluginKey, "versions"), ""), ",");
      for (String pluginVersion : pluginReleases) {
        Release release = new Release(plugin, pluginVersion);
        plugin.addRelease(release);
        release.setDownloadUrl(get(p, pluginKey, pluginVersion + ".downloadUrl"));
        release.setChangelogUrl(get(p, pluginKey, pluginVersion + ".changelogUrl"));
        release.setDescription(get(p, pluginKey, pluginVersion + ".description"));
        release.setDate(toDate(get(p, pluginKey, pluginVersion + ".date"), true));
        String[] requiredSonarVersions = StringUtils.split(StringUtils.defaultIfEmpty(get(p, pluginKey, pluginVersion + ".requiredSonarVersions"), ""), ",");
        for (String requiredSonarVersion : requiredSonarVersions) {
          release.addRequiredSonarVersions(Version.create(requiredSonarVersion));
        }
      }
    }

    return center;
  }

  private static String get(Properties props, String key) {
    return StringUtils.defaultIfEmpty(props.getProperty(key), null);
  }

  private static String[] getArray(Properties props, String key) {
    return StringUtils.split(StringUtils.defaultIfEmpty(props.getProperty(key), ""), ",");
  }

  private static String get(Properties p, String pluginKey, String field) {
    return get(p, pluginKey + "." + field);
  }

}
