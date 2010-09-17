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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.findbugs.xml.ClassFilter;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;

public class FindbugsMavenPluginHandler implements MavenPluginHandler {

  private RulesProfile profile;
  private FindbugsProfileExporter exporter;

  public FindbugsMavenPluginHandler(RulesProfile profile, FindbugsProfileExporter exporter) {
    this.profile = profile;
    this.exporter = exporter;
  }

  public String getGroupId() {
    return MavenUtils.GROUP_ID_CODEHAUS_MOJO;
  }

  public String getArtifactId() {
    return "findbugs-maven-plugin";
  }

  public String getVersion() {
    // IMPORTANT : the version of the Findbugs lib must be also updated in the pom.xml (property findbugs.version).
    return "2.3.1";
  }

  public boolean isFixedVersion() {
    return true;
  }

  public String[] getGoals() {
    return new String[] { "findbugs" };
  }

  public void configure(Project project, MavenPlugin plugin) {
    configureClassesDir(project, plugin);
    configureBasicParameters(project, plugin);
    configureFilters(project, plugin);
  }

  private void configureBasicParameters(Project project, MavenPlugin plugin) {
    plugin.setParameter("xmlOutput", "true");
    plugin.setParameter("threshold", "Low");
    plugin.setParameter("skip", "false");
    plugin.setParameter("effort", getEffort(project), false);
    plugin.setParameter("maxHeap", "" + getMaxHeap(project), false);
    String timeout = getTimeout(project);
    if (StringUtils.isNotBlank(timeout)) {
      plugin.setParameter("timeout", timeout, false);
    }
  }

  protected void configureFilters(Project project, MavenPlugin plugin) {
    try {
      String existingIncludeFilterConfig = plugin.getParameter("includeFilterFile");
      String existingExcludeFilterConfig = plugin.getParameter("excludeFilterFile");
      boolean existingConfig = !StringUtils.isBlank(existingIncludeFilterConfig) || !StringUtils.isBlank(existingExcludeFilterConfig);
      if ( !project.getReuseExistingRulesConfig() || (project.getReuseExistingRulesConfig() && !existingConfig)) {
        File includeXmlFile = saveIncludeConfigXml(project);
        plugin.setParameter("includeFilterFile", getPath(includeXmlFile));

        File excludeXmlFile = saveExcludeConfigXml(project);
        plugin.setParameter("excludeFilterFile", getPath(excludeXmlFile));
      }

    } catch (IOException e) {
      throw new SonarException("Failed to save the findbugs XML configuration.", e);
    }
  }

  private String getPath(File file) throws IOException {
    // the findbugs maven plugin fails on windows if the path contains backslashes
    String path = file.getCanonicalPath();
    return path.replace('\\', '/');
  }

  private void configureClassesDir(Project project, MavenPlugin plugin) {
    File classesDir = project.getFileSystem().getBuildOutputDir();
    if (classesDir == null || !classesDir.exists()) {
      throw new SonarException("Findbugs needs sources to be compiled. "
          + "Please build project or edit pom.xml to set the <outputDirectory> property before executing sonar.");
    }
    try {
      plugin.setParameter("classFilesDirectory", classesDir.getCanonicalPath());
    } catch (Exception e) {
      throw new SonarException("Invalid classes directory", e);
    }
  }

  private File saveIncludeConfigXml(Project project) throws IOException {
    StringWriter conf = new StringWriter();
    exporter.exportProfile(profile, conf);
    return project.getFileSystem().writeToWorkingDirectory(conf.toString(), "findbugs-include.xml");
  }

  private File saveExcludeConfigXml(Project project) throws IOException {
    FindBugsFilter findBugsFilter = new FindBugsFilter();
    if (project.getExclusionPatterns() != null) {
      for (String exclusion : project.getExclusionPatterns()) {
        ClassFilter classFilter = new ClassFilter(FindbugsAntConverter.antToJavaRegexpConvertor(exclusion));
        findBugsFilter.addMatch(new Match(classFilter));
      }
    }
    return project.getFileSystem().writeToWorkingDirectory(findBugsFilter.toXml(), "findbugs-exclude.xml");
  }

  private String getEffort(Project project) {
    return project.getConfiguration().getString(CoreProperties.FINDBUGS_EFFORT_PROPERTY, CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE);
  }

  private int getMaxHeap(Project project) {
    return project.getConfiguration().getInt(CoreProperties.FINDBUGS_MAXHEAP_PROPERTY, CoreProperties.FINDBUGS_MAXHEAP_DEFAULT_VALUE);
  }

  private String getTimeout(Project project) {
    return project.getConfiguration().getString(CoreProperties.FINDBUGS_TIMEOUT_PROPERTY);
  }
}
