/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch.maven;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collection;

/**
 * An utility class to manipulate Maven concepts
 * 
 * @since 1.10
 */
public final class MavenUtils {

  public static final String GROUP_ID_APACHE_MAVEN = "org.apache.maven.plugins";
  public static final String GROUP_ID_CODEHAUS_MOJO = "org.codehaus.mojo";

  private MavenUtils() {
    // utility class with only static methods
  }

  /**
   * Returns the version of Java used by the maven compiler plugin
   * 
   * @param pom the project pom
   * @return the java version
   */
  public static String getJavaVersion(MavenProject pom) {
    MavenPlugin compilerPlugin = MavenPlugin.getPlugin(pom, GROUP_ID_APACHE_MAVEN, "maven-compiler-plugin");
    if (compilerPlugin != null) {
      return compilerPlugin.getParameter("target");
    }
    return null;
  }

  public static String getJavaSourceVersion(MavenProject pom) {
    MavenPlugin compilerPlugin = MavenPlugin.getPlugin(pom, GROUP_ID_APACHE_MAVEN, "maven-compiler-plugin");
    if (compilerPlugin != null) {
      return compilerPlugin.getParameter("source");
    }
    return null;
  }

  /**
   * Queries a collection of plugins based on a group id and an artifact id and returns the plugin if it exists
   * 
   * @param plugins the plugins collection
   * @param groupId the group id
   * @param artifactId the artifact id
   * @return the corresponding plugin if it exists, null otherwise
   */
  public static Plugin getPlugin(Collection<Plugin> plugins, String groupId, String artifactId) {
    if (plugins != null) {
      for (Plugin plugin : plugins) {
        if (equals(plugin, groupId, artifactId)) {
          return plugin;
        }
      }
    }
    return null;
  }

  /**
   * Tests whether a plugin has got a given artifact id and group id
   * 
   * @param plugin the plugin to test
   * @param groupId the group id
   * @param artifactId the artifact id
   * @return whether the plugin has got group + artifact ids
   */
  public static boolean equals(Plugin plugin, String groupId, String artifactId) {
    if (plugin != null && plugin.getArtifactId().equals(artifactId)) {
      if (plugin.getGroupId() == null) {
        return groupId == null || groupId.equals(MavenUtils.GROUP_ID_APACHE_MAVEN) || groupId.equals(MavenUtils.GROUP_ID_CODEHAUS_MOJO);
      }
      return plugin.getGroupId().equals(groupId);
    }
    return false;
  }

  /**
   * Tests whether a ReportPlugin has got a given artifact id and group id
   * 
   * @param plugin the ReportPlugin to test
   * @param groupId the group id
   * @param artifactId the artifact id
   * @return whether the ReportPlugin has got group + artifact ids
   */
  public static boolean equals(ReportPlugin plugin, String groupId, String artifactId) {
    if (plugin != null && plugin.getArtifactId().equals(artifactId)) {
      if (plugin.getGroupId() == null) {
        return groupId == null || groupId.equals(MavenUtils.GROUP_ID_APACHE_MAVEN) || groupId.equals(MavenUtils.GROUP_ID_CODEHAUS_MOJO);
      }
      return plugin.getGroupId().equals(groupId);
    }
    return false;
  }

  /**
   * @return source encoding
   */
  public static String getSourceEncoding(MavenProject pom) {
    return pom.getProperties().getProperty("project.build.sourceEncoding");
  }

  /**
   * Returns the charset of a pom
   * 
   * @param pom the project pom
   * @return the charset
   */
  public static Charset getSourceCharset(MavenProject pom) {
    String encoding = getSourceEncoding(pom);
    if (StringUtils.isNotEmpty(encoding)) {
      try {
        return Charset.forName(encoding);

      } catch (Exception e) {
        LoggerFactory.getLogger(MavenUtils.class).warn("Can not get project charset", e);
      }
    }
    return Charset.defaultCharset();
  }
}
