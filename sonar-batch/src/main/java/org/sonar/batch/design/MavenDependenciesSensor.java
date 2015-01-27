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
package org.sonar.batch.design;

import org.sonar.api.batch.RequiresDB;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.config.Settings;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourcePersister;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SupportedEnvironment("maven")
@RequiresDB
public class MavenDependenciesSensor implements Sensor {

  private static final String SONAR_MAVEN_PROJECT_DEPENDENCY = "sonar.maven.projectDependencies";

  private static final Logger LOG = LoggerFactory.getLogger(MavenDependenciesSensor.class);

  private final ArtifactRepository localRepository;
  private final ArtifactFactory artifactFactory;
  private final ArtifactMetadataSource artifactMetadataSource;
  private final ArtifactCollector artifactCollector;
  private final DependencyTreeBuilder treeBuilder;
  private final SonarIndex index;
  private final Settings settings;
  private final ResourcePersister resourcePersister;

  public MavenDependenciesSensor(Settings settings, ArtifactRepository localRepository, ArtifactFactory artifactFactory, ArtifactMetadataSource artifactMetadataSource,
    ArtifactCollector artifactCollector, DependencyTreeBuilder treeBuilder, SonarIndex index, ResourcePersister resourcePersister) {
    this.settings = settings;
    this.localRepository = localRepository;
    this.artifactFactory = artifactFactory;
    this.artifactMetadataSource = artifactMetadataSource;
    this.artifactCollector = artifactCollector;
    this.index = index;
    this.treeBuilder = treeBuilder;
    this.resourcePersister = resourcePersister;
  }

  /**
   * Used with SQ Maven plugin 2.5+
   */
  public MavenDependenciesSensor(Settings settings, SonarIndex index, ResourcePersister resourcePersister) {
    this(settings, null, null, null, null, null, index, resourcePersister);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  private static class InputDependency {

    private final String key;

    private final String version;

    private String scope;

    List<InputDependency> dependencies = new ArrayList<InputDependency>();

    public InputDependency(String key, String version) {
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

    public InputDependency setScope(String scope) {
      this.scope = scope;
      return this;
    }

    public List<InputDependency> dependencies() {
      return dependencies;
    }
  }

  private static class DependencyDeserializer implements JsonDeserializer<InputDependency> {

    @Override
    public InputDependency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

      JsonObject dep = json.getAsJsonObject();
      String key = dep.get("k").getAsString();
      String version = dep.get("v").getAsString();
      InputDependency result = new InputDependency(key, version);
      result.setScope(dep.get("s").getAsString());
      JsonElement subDeps = dep.get("d");
      if (subDeps != null) {
        JsonArray arrayOfSubDeps = subDeps.getAsJsonArray();
        for (JsonElement e : arrayOfSubDeps) {
          result.dependencies().add(deserialize(e, typeOfT, context));
        }
      }
      return result;
    }

  }

  @Override
  public void analyse(final Project project, final SensorContext context) {
    if (settings.hasKey(SONAR_MAVEN_PROJECT_DEPENDENCY)) {
      LOG.debug("Using dependency provided by property " + SONAR_MAVEN_PROJECT_DEPENDENCY);
      String depsAsJson = settings.getString(SONAR_MAVEN_PROJECT_DEPENDENCY);
      Collection<InputDependency> deps;
      try {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(InputDependency.class, new DependencyDeserializer());
        Gson gson = gsonBuilder.create();

        Type collectionType = new TypeToken<Collection<InputDependency>>() {
        }.getType();
        deps = gson.fromJson(depsAsJson, collectionType);
        saveDependencies(project, project, deps, context);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to deserialize dependency information: " + depsAsJson, e);
      }
    } else if (treeBuilder != null) {
      computeDependencyTree(project, context);
    }
  }

  private void computeDependencyTree(final Project project, final SensorContext context) {
    LOG.warn("Computation of Maven dependencies by SonarQube is deprecated. Please update the version of SonarQube Maven plugin to 2.5+");
    try {
      DependencyNode root = treeBuilder.buildDependencyTree(project.getPom(), localRepository, artifactFactory, artifactMetadataSource, null, artifactCollector);

      DependencyNodeVisitor visitor = new BuildingDependencyNodeVisitor(new DependencyNodeVisitor() {
        @Override
        public boolean visit(DependencyNode node) {
          return true;
        }

        @Override
        public boolean endVisit(DependencyNode node) {
          if (node.getParent() != null && node.getParent() != node) {
            saveDependency(project, node, context);
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

  private void saveDependencies(Project project, Resource from, Collection<InputDependency> deps, SensorContext context) {
    for (InputDependency inputDep : deps) {
      Resource to = toResource(project, inputDep, context);
      Dependency dependency = new Dependency(from, to);
      dependency.setUsage(inputDep.scope());
      dependency.setWeight(1);
      context.saveDependency(dependency);
      if (!inputDep.dependencies().isEmpty()) {
        saveDependencies(project, to, inputDep.dependencies(), context);
      }
    }
  }

  private Resource toResource(Project project, InputDependency dependency, SensorContext context) {
    Project depProject = new Project(dependency.key(), project.getBranch(), dependency.key());
    Resource result = context.getResource(depProject);
    if (result == null || !((Project) result).getAnalysisVersion().equals(dependency.version())) {
      Library lib = new Library(dependency.key(), dependency.version());
      index.addResource(lib);
      // Temporary hack since we need snapshot id to persist dependencies
      resourcePersister.persist();
      result = context.getResource(lib);
    }
    return result;
  }

  protected void saveDependency(final Project project, DependencyNode node, SensorContext context) {
    Resource from = (node.getParent().getParent() == null) ? index.getProject() : toResource(project, node.getParent().getArtifact(), context);
    Resource to = toResource(project, node.getArtifact(), context);
    Dependency dependency = new Dependency(from, to);
    dependency.setUsage(node.getArtifact().getScope());
    dependency.setWeight(1);
    context.saveDependency(dependency);
  }

  protected Resource toResource(final Project project, Artifact artifact, SensorContext context) {
    Project depWithBranch = Project.createFromMavenIds(artifact.getGroupId(), artifact.getArtifactId(), project.getBranch());
    Resource result = context.getResource(depWithBranch);
    if (result == null || !((Project) result).getAnalysisVersion().equals(artifact.getBaseVersion())) {
      Library lib = Library.createFromMavenIds(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
      index.addResource(lib);
      // Temporary hack since we need snapshot id to persist dependencies
      resourcePersister.persist();
      result = context.getResource(lib);
    }
    return result;
  }

  @Override
  public String toString() {
    return "Maven dependencies";
  }
}
