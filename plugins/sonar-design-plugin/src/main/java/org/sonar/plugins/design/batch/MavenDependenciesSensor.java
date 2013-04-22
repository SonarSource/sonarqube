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
package org.sonar.plugins.design.batch;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
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
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

@SupportedEnvironment("maven")
public class MavenDependenciesSensor implements Sensor {

  private ArtifactRepository localRepository;
  private ArtifactFactory artifactFactory;
  private ArtifactMetadataSource artifactMetadataSource;
  private ArtifactCollector artifactCollector;
  private DependencyTreeBuilder treeBuilder;
  private SonarIndex index;

  public MavenDependenciesSensor(ArtifactRepository localRepository, ArtifactFactory artifactFactory, ArtifactMetadataSource artifactMetadataSource,
                                 ArtifactCollector artifactCollector, DependencyTreeBuilder treeBuilder, SonarIndex index) {
    this.localRepository = localRepository;
    this.artifactFactory = artifactFactory;
    this.artifactMetadataSource = artifactMetadataSource;
    this.artifactCollector = artifactCollector;
    this.index = index;
    this.treeBuilder = treeBuilder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(final Project project, final SensorContext context) {
    try {
      DependencyNode root = treeBuilder.buildDependencyTree(project.getPom(), localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);

      DependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor(new DependencyNodeVisitor() {
        public boolean visit(DependencyNode node) {
          return true;
        }

        public boolean endVisit(DependencyNode node) {
          if (node.getParent() != null && node.getParent() != node) {
            saveDependency(node, context);
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
      throw new SonarException("Can not load the graph of dependencies of the project " + project.getKey(), e);
    }
  }

  protected void saveDependency(DependencyNode node, SensorContext context) {
    Resource from = (node.getParent().getParent() == null) ? index.getProject() : toResource(node.getParent().getArtifact(), context);
    Resource to = toResource(node.getArtifact(), context);
    Dependency dependency = new Dependency(from, to);
    dependency.setUsage(node.getArtifact().getScope());
    dependency.setWeight(1);
    context.saveDependency(dependency);
  }

  protected static Resource toResource(Artifact artifact, SensorContext context) {
    Project project = Project.createFromMavenIds(artifact.getGroupId(), artifact.getArtifactId());
    Resource result = context.getResource(project);
    if (result == null || !((Project) result).getAnalysisVersion().equals(artifact.getBaseVersion())) {
      Library lib = new Library(project.getKey(), artifact.getBaseVersion());
      context.saveResource(lib);
      result = context.getResource(lib);
    }
    return result;
  }

  @Override
  public String toString() {
    return "Maven dependencies";
  }
}
