/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.squid;

import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.*;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.java.api.JavaUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

@Phase(name = Phase.Name.PRE)
@DependsUpon(JavaUtils.BARRIER_BEFORE_SQUID)
@DependedUpon(value = JavaUtils.BARRIER_AFTER_SQUID, classes = NoSonarFilter.class)
public class SquidSensor implements Sensor {

  private NoSonarFilter noSonarFilter;
  private RulesProfile profile;
  private ProjectClasspath projectClasspath;
  private ResourceCreationLock lock;

  public SquidSensor(RulesProfile profile, NoSonarFilter noSonarFilter, ProjectClasspath projectClasspath, ResourceCreationLock lock) {
    this.noSonarFilter = noSonarFilter;
    this.profile = profile;
    this.projectClasspath = projectClasspath;
    this.lock = lock;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

  @SuppressWarnings("unchecked")
  public void analyse(Project project, SensorContext context) {
    analyzeMainSources(project, context);
    browseTestSources(project, context);
    lock.lock();
  }

  private void analyzeMainSources(Project project, SensorContext context) {
    boolean analyzePropertyAccessors = project.getConfiguration().getBoolean(SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_PROPERTY,
        SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE);
    String fieldNamesToExcludeFromLcom4Computation = project.getConfiguration().getString(
        SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
        SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE);
    Charset charset = project.getFileSystem().getSourceCharset();

    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, SquidConstants.REPOSITORY_KEY, SquidRuleRepository.getCheckClasses());

    SquidExecutor squidExecutor = new SquidExecutor(analyzePropertyAccessors, fieldNamesToExcludeFromLcom4Computation, factory, charset);
    squidExecutor.scan(getMainSourceFiles(project), getMainBytecodeFiles(project));
    squidExecutor.save(project, context, noSonarFilter);
    squidExecutor.flush();
  }

  private void browseTestSources(Project project, SensorContext context) {
    for (InputFile testFile : project.getFileSystem().testFiles(Java.KEY)) {
      context.index(JavaFile.fromRelativePath(testFile.getRelativePath(), true));
    }
  }

  private List<File> getMainSourceFiles(Project project) {
    return project.getFileSystem().getJavaSourceFiles();
  }

  private Collection<File> getMainBytecodeFiles(Project project) {
    Collection<File> bytecodeFiles = projectClasspath.getElements();
    if (!hasProjectBytecodeFiles(project)) {
      File classesDir = project.getFileSystem().getBuildOutputDir();
      if (classesDir != null && classesDir.exists()) {
        bytecodeFiles.remove(classesDir);
      }
    }
    return bytecodeFiles;
  }

  private boolean hasProjectBytecodeFiles(Project project) {
    if (!project.getConfiguration()
        .getBoolean(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE)) {
      File classesDir = project.getFileSystem().getBuildOutputDir();
      if (classesDir != null && classesDir.exists()) {
        return !FileUtils.listFiles(classesDir, new String[]{"class"}, true).isEmpty();
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
