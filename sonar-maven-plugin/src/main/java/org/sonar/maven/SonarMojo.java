/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.StateDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.RunnerProperties;
import org.sonar.runner.api.ScanProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * @goal sonar
 * @aggregator
 * @requiresDependencyResolution test
 * @deprecated Only kept for backward compatibility with old version of SQ Maven plugin
 */
@Deprecated
public final class SonarMojo extends AbstractMojo {

  /**
   * @parameter property="session"
   * @required
   * @readonly
   */
  private MavenSession session;

  /**
   * @parameter property="project"
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
   * @parameter property="localRepository"
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
   * @VisibleForTesting
   */
  RuntimeInformation runtimeInformation;

  @Override
  public void execute() throws MojoExecutionException {
    ArtifactVersion mavenVersion = getMavenVersion();
    if (mavenVersion.getMajorVersion() == 2 && mavenVersion.getMinorVersion() < 2) {
      ExceptionHandling.handle("Please use at least Maven 2.2.x to perform SonarQube analysis (current version is " + mavenVersion.toString() + ")", getLog());
    }

    try {
      EmbeddedRunner runner = EmbeddedRunner.create()
        .setApp("Maven", mavenVersion.toString())
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
        .setProperty(ScanProperties.PROJECT_SOURCE_DIRS, ".")
        .setProperty("sonar.maven.projectDependencies", dependenciesToJson(collectProjectDependencies()));
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
      runner.addExtensions(session, getLog(), lifecycleExecutor, projectBuilder);
      if (getLog().isDebugEnabled()) {
        runner.setProperty("sonar.verbose", "true");
      }
      runner.execute();
    } catch (Exception e) {
      throw ExceptionHandling.handle(e, getLog());
    }
  }

  private static class Dependency {

    private final String key;
    private final String version;
    private String scope;
    List<Dependency> dependencies = new ArrayList<SonarMojo.Dependency>();

    public Dependency(String key, String version) {
      this.key = key;
      this.version = version;
    }

    public String key() {
      return key;
    }

    public String version() {
      return version;
    }

    public String scope() {
      return scope;
    }

    public Dependency setScope(String scope) {
      this.scope = scope;
      return this;
    }

    public List<Dependency> dependencies() {
      return dependencies;
    }
  }

  private List<Dependency> collectProjectDependencies() {
    final List<Dependency> result = new ArrayList<SonarMojo.Dependency>();
    try {
      DependencyNode root = dependencyTreeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);

      DependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor(new DependencyNodeVisitor() {

        private Stack<Dependency> stack = new Stack<SonarMojo.Dependency>();

        public boolean visit(DependencyNode node) {
          if (node.getParent() != null && node.getParent() != node) {
            Dependency dependency = toDependency(node);
            if (stack.isEmpty()) {
              result.add(dependency);
            }
            else {
              stack.peek().dependencies().add(dependency);
            }
            stack.push(dependency);
          }
          return true;
        }

        public boolean endVisit(DependencyNode node) {
          if (!stack.isEmpty()) {
            stack.pop();
          }
          return true;
        }
      });

      // mode verbose OFF : do not show the same lib many times
      DependencyNodeFilter filter = StateDependencyNodeFilter.INCLUDED;

      CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
      DependencyNodeVisitor firstPassVisitor = new FilteringDependencyNodeVisitor(collectingVisitor, filter);
      root.accept(firstPassVisitor);

      DependencyNodeFilter secondPassFilter = new AncestorOrSelfDependencyNodeFilter(collectingVisitor.getNodes());
      visitor = new FilteringDependencyNodeVisitor(visitor, secondPassFilter);

      root.accept(visitor);

    } catch (DependencyTreeBuilderException e) {
      throw new IllegalStateException("Can not load the graph of dependencies of the project " + getSonarKey(project), e);
    }
    return result;
  }

  private Dependency toDependency(DependencyNode node) {
    String key = String.format("%s:%s", node.getArtifact().getGroupId(), node.getArtifact().getArtifactId());
    String version = node.getArtifact().getBaseVersion();
    return new Dependency(key, version).setScope(node.getArtifact().getScope());
  }

  private String dependenciesToJson(List<Dependency> deps) {
    StringBuilder json = new StringBuilder();
    json.append('[');
    serializeDeps(json, deps);
    json.append(']');
    return json.toString();
  }

  private void serializeDeps(StringBuilder json, List<Dependency> deps) {
    for (Iterator<Dependency> dependencyIt = deps.iterator(); dependencyIt.hasNext();) {
      serializeDep(json, dependencyIt.next());
      if (dependencyIt.hasNext()) {
        json.append(',');
      }
    }
  }

  private void serializeDep(StringBuilder json, Dependency dependency) {
    json.append("{");
    json.append("\"k\":\"");
    json.append(dependency.key());
    json.append("\",\"v\":\"");
    json.append(dependency.version());
    json.append("\",\"s\":\"");
    json.append(dependency.scope());
    json.append("\",\"d\":[");
    serializeDeps(json, dependency.dependencies());
    json.append("]");
    json.append("}");
  }

  private ArtifactVersion getMavenVersion() {
    return runtimeInformation.getApplicationVersion();
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
