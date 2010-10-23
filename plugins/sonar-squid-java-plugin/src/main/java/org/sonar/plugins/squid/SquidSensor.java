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
package org.sonar.plugins.squid;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.java.bytecode.check.BytecodeChecks;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Phase(name = Phase.Name.PRE)
/* TODO is the flag still used ? */
@DependedUpon(value = Sensor.FLAG_SQUID_ANALYSIS, classes = NoSonarFilter.class)
public class SquidSensor implements Sensor {

  private SquidSearchProxy proxy;
  private NoSonarFilter noSonarFilter;
  private RulesProfile profile;

  public SquidSensor(RulesProfile profile, SquidSearchProxy proxy, NoSonarFilter noSonarFilter) {
    this.proxy = proxy;
    this.noSonarFilter = noSonarFilter;
    this.profile = profile;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }

  @SuppressWarnings("unchecked")
  public void analyse(Project project, SensorContext context) {
    boolean analyzePropertyAccessors = project.getConfiguration().getBoolean(SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_PROPERTY,
        SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE);
    String fieldNamesToExcludeFromLcom4Computation = project.getConfiguration().getString(
        SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
        SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE);
    Charset charset = project.getFileSystem().getSourceCharset();

    AnnotationCheckFactory factory = AnnotationCheckFactory.create(profile, SquidConstants.REPOSITORY_KEY, BytecodeChecks.getCheckClasses());

    SquidExecutor squidExecutor = new SquidExecutor(analyzePropertyAccessors, fieldNamesToExcludeFromLcom4Computation, factory, charset);
    squidExecutor.scan(getSourceFiles(project), getBytecodeFiles(project));
    squidExecutor.save(project, context, noSonarFilter);
    squidExecutor.initSonarProxy(proxy);
  }

  private List<File> getSourceFiles(Project project) {
    return project.getFileSystem().getJavaSourceFiles();
  }

  /**
   * TODO replace this code by org.sonar.api.batch.ProjectClasspath
   * 
   * @return Collection of java.util.File
   */
  private Collection<File> getBytecodeFiles(Project project) {
    try {
      Collection<File> bytecodeFiles = new ArrayList<File>();
      if (hasProjectBytecodeFiles(project)) {
        File classesDir = project.getFileSystem().getBuildOutputDir();
        if (classesDir != null && classesDir.exists()) {
          bytecodeFiles.add(classesDir);
        }

        MavenProject mavenProject = project.getPom();
        FilterArtifacts filters = new FilterArtifacts();
        filters.addFilter(new ProjectTransitivityFilter(mavenProject.getDependencyArtifacts(), false));

        // IMPORTANT : the following annotation must be aded to BatchMojo : @requiresDependencyResolution test
        // => Include scopes compile and provided, exclude scopes test, system and runtime
        filters.addFilter(new ScopeFilter("compile", ""));
        Set<Artifact> artifacts = mavenProject.getArtifacts();
        artifacts = filters.filter(artifacts);
        for (Artifact a : artifacts) {
          bytecodeFiles.add(a.getFile());
        }
      }
      return bytecodeFiles;

    } catch (Exception e) {
      throw new SonarException(e);
    }
  }

  private boolean hasProjectBytecodeFiles(Project project) {
    if ( !project.getConfiguration()
        .getBoolean(CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY, CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE)) {
      File classesDir = project.getFileSystem().getBuildOutputDir();
      if (classesDir != null && classesDir.exists()) {
        return !FileUtils.listFiles(classesDir, new String[] { "class" }, true).isEmpty();
      }
    }
    return false;

  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
