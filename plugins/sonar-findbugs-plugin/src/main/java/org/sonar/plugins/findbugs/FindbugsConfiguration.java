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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.findbugs.xml.ClassFilter;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;
import org.sonar.plugins.findbugs.xml.Match;

/**
 * @since 2.4
 */
public class FindbugsConfiguration implements BatchExtension {

  private Project project;
  private RulesProfile profile;
  private FindbugsProfileExporter exporter;
  private ProjectClasspath projectClasspath;

  public FindbugsConfiguration(Project project, RulesProfile profile, FindbugsProfileExporter exporter, ProjectClasspath classpath) {
    this.project = project;
    this.profile = profile;
    this.exporter = exporter;
    this.projectClasspath = classpath;
  }

  public File getTargetXMLReport() {
    if (project.getConfiguration().getBoolean(FindbugsConstants.GENERATE_XML_KEY, FindbugsConstants.GENERATE_XML_DEFAULT_VALUE)) {
      return new File(project.getFileSystem().getSonarWorkingDirectory(), "findbugs-result.xml");
    }
    return null;
  }

  public edu.umd.cs.findbugs.Project getFindbugsProject() {
    File classesDir = project.getFileSystem().getBuildOutputDir();
    if (classesDir == null || !classesDir.exists()) {
      throw new SonarException("Findbugs needs sources to be compiled. "
          + "Please build project or edit pom.xml to set the <outputDirectory> property before executing sonar.");
    }

    edu.umd.cs.findbugs.Project findbugsProject = new edu.umd.cs.findbugs.Project();
    for (File dir : project.getFileSystem().getSourceDirs()) {
      findbugsProject.addSourceDir(dir.getAbsolutePath());
    }
    findbugsProject.addFile(classesDir.getAbsolutePath());
    for (File file : projectClasspath.getElements()) {
      if ( !file.equals(classesDir)) {
        findbugsProject.addAuxClasspathEntry(file.getAbsolutePath());
      }
    }
    findbugsProject.addAuxClasspathEntry(copyLib("/annotations-" + FindbugsVersion.getVersion() + ".jar").getAbsolutePath());
    findbugsProject.addAuxClasspathEntry(copyLib("/jsr305-" + FindbugsVersion.getVersion() + ".jar").getAbsolutePath());
    findbugsProject.setCurrentWorkingDirectory(project.getFileSystem().getBuildDir());
    return findbugsProject;
  }

  public File saveIncludeConfigXml() throws IOException {
    StringWriter conf = new StringWriter();
    exporter.exportProfile(profile, conf);
    return project.getFileSystem().writeToWorkingDirectory(conf.toString(), "findbugs-include.xml");
  }

  public File saveExcludeConfigXml() throws IOException {
    FindBugsFilter findBugsFilter = new FindBugsFilter();
    if (project.getExclusionPatterns() != null) {
      for (String exclusion : project.getExclusionPatterns()) {
        ClassFilter classFilter = new ClassFilter(FindbugsAntConverter.antToJavaRegexpConvertor(exclusion));
        findBugsFilter.addMatch(new Match(classFilter));
      }
    }
    return project.getFileSystem().writeToWorkingDirectory(findBugsFilter.toXml(), "findbugs-exclude.xml");
  }

  public String getEffort() {
    return StringUtils.lowerCase(project.getConfiguration().getString(CoreProperties.FINDBUGS_EFFORT_PROPERTY,
        CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE));
  }

  public long getTimeout() {
    return project.getConfiguration().getLong(CoreProperties.FINDBUGS_TIMEOUT_PROPERTY, CoreProperties.FINDBUGS_TIMEOUT_DEFAULT_VALUE);
  }

  private File copyLib(String name) {
    try {
      InputStream is = getClass().getResourceAsStream(name);
      File temp = File.createTempFile("findbugs", ".jar");
      FileUtils.forceDeleteOnExit(temp);
      OutputStream os = FileUtils.openOutputStream(temp);
      IOUtils.copy(is, os);
      return temp;
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }
}
