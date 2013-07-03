/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.maven;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.RunnerProperties;
import org.sonar.runner.api.ScanProperties;

import java.io.File;
import java.io.IOException;

/**
 * @goal sonar
 * @aggregator
 * @requiresDependencyResolution test
 */
public final class SonarMojo extends AbstractMojo {

  /**
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  private MavenSession session;

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * @component
   * @required
   */
  private LifecycleExecutor lifecycleExecutor;

  /**
   * The artifact factory to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactFactory artifactFactory;

  /**
   * The artifact repository to use.
   *
   * @parameter expression="${localRepository}"
   * @required
   * @readonly
   */
  private ArtifactRepository localRepository;

  /**
   * The artifact metadata source to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactMetadataSource artifactMetadataSource;

  /**
   * The artifact collector to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactCollector artifactCollector;

  /**
   * The dependency tree builder to use.
   *
   * @component
   * @required
   * @readonly
   */
  private DependencyTreeBuilder dependencyTreeBuilder;

  /**
   * @component
   * @required
   * @readonly
   */
  private MavenProjectBuilder projectBuilder;

  /**
   * @component
   * @required
   * @readonly
   */
  private RuntimeInformation runtimeInformation;

  public void execute() throws MojoExecutionException, MojoFailureException {

    EmbeddedRunner runner = EmbeddedRunner.create()
        .setApp("Maven", getMavenVersion())
        .addProperties(session.getExecutionProperties())
        .addProperties(project.getModel().getProperties())
        // Add user properties (ie command line arguments -Dsonar.xxx=yyyy) in last position to override all other
        .addProperties(session.getUserProperties());
    String encoding = getSourceEncoding(project);
    if (encoding != null) {
      runner.setProperty(ScanProperties.PROJECT_SOURCE_ENCODING, encoding);
    }
    runner
        .setProperty(ScanProperties.PROJECT_KEY, getSonarKey(project))
        .setProperty(RunnerProperties.WORK_DIR, getSonarWorkDir(project).getAbsolutePath())
        .setProperty(ScanProperties.PROJECT_BASEDIR, project.getBasedir().getAbsolutePath())
        .setProperty(ScanProperties.PROJECT_VERSION, toString(project.getVersion()))
        .setProperty(ScanProperties.PROJECT_NAME, toString(project.getName()))
        .setProperty(ScanProperties.PROJECT_DESCRIPTION, toString(project.getDescription()))
        .setProperty(ScanProperties.PROJECT_SOURCE_DIRS, ".");
    // Exclude log implementation to not conflict with Maven 3.1 logging impl
    runner.mask("org.slf4j.LoggerFactory")
        // Include slf4j Logger that is exposed by some Sonar components
        .unmask("org.slf4j.Logger")
        .unmask("org.slf4j.ILoggerFactory")
        // Exclude other slf4j classes
        // .unmask("org.slf4j.impl.")
        .mask("org.slf4j.")
        // Exclude logback
        .mask("ch.qos.logback.")
        .mask("org.sonar.")
        // Include everything else
        .unmask("");
    runner.addExtensions(session, getLog(), lifecycleExecutor, artifactFactory, localRepository, artifactMetadataSource, artifactCollector,
            dependencyTreeBuilder, projectBuilder);
    if (getLog().isDebugEnabled()) {
      runner.setProperty("sonar.verbose", "true");
    }
    runner.execute();
  }

  private String getMavenVersion() {
    return runtimeInformation.getApplicationVersion().toString();
  }

  public static String toString(Object obj) {
    return obj == null ? "" : obj.toString();
  }

  public static String getSourceEncoding(MavenProject pom) {
    return pom.getProperties().getProperty("project.build.sourceEncoding");
  }

  public static String getSonarKey(MavenProject pom) {
    return new StringBuilder().append(pom.getGroupId()).append(":").append(pom.getArtifactId()).toString();
  }

  public static File getSonarWorkDir(MavenProject pom) {
    return new File(getBuildDir(pom), "sonar");
  }

  private static File getBuildDir(MavenProject pom) {
    return resolvePath(pom.getBuild().getDirectory(), pom.getBasedir());
  }

  static File resolvePath(String path, File basedir) {
    if (path != null) {
      File file = new File(path);
      if (!file.isAbsolute()) {
        try {
          file = new File(basedir, path).getCanonicalFile();
        } catch (IOException e) {
          throw new IllegalStateException("Unable to resolve path '" + path + "'", e);
        }
      }
      return file;
    }
    return null;
  }
}
