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
package org.sonar.batch.scan2;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectBootstrapper;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Languages;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.ExtensionInstaller;
import org.sonar.batch.bootstrap.ExtensionMatcher;
import org.sonar.batch.bootstrap.ExtensionUtils;
import org.sonar.batch.duplication.BlockCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.languages.DefaultLanguagesReferential;
import org.sonar.batch.profiling.PhasesSumUpTimeProfiler;
import org.sonar.batch.referential.DefaultProjectReferentialsLoader;
import org.sonar.batch.referential.ProjectReferentialsLoader;
import org.sonar.batch.referential.ProjectReferentialsProvider;
import org.sonar.batch.scan.ProjectReactorBuilder;
import org.sonar.batch.scan.ProjectSettings;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.maven.FakeMavenPluginExecutor;
import org.sonar.batch.scan.maven.MavenPluginExecutor;
import org.sonar.batch.test.CoveragePerTestCache;
import org.sonar.batch.test.TestCaseCache;

public class ProjectScanContainer extends ComponentContainer {
  public ProjectScanContainer(ComponentContainer taskContainer) {
    super(taskContainer);
  }

  @Override
  protected void doBeforeStart() {
    projectBootstrap();
    addBatchComponents();
    fixMavenExecutor();
    addBatchExtensions();
    Settings settings = getComponentByType(Settings.class);
    if (settings != null && settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)) {
      add(PhasesSumUpTimeProfiler.class);
    }
  }

  private void projectBootstrap() {
    ProjectReactor reactor;
    ProjectBootstrapper bootstrapper = getComponentByType(ProjectBootstrapper.class);
    Settings settings = getComponentByType(Settings.class);
    if (bootstrapper == null
      // Starting from Maven plugin 2.3 then only DefaultProjectBootstrapper should be used.
      || "true".equals(settings.getString("sonar.mojoUseRunner"))) {
      // Use default SonarRunner project bootstrapper
      ProjectReactorBuilder builder = getComponentByType(ProjectReactorBuilder.class);
      reactor = builder.execute();
    } else {
      reactor = bootstrapper.bootstrap();
    }
    if (reactor == null) {
      throw new IllegalStateException(bootstrapper + " has returned null as ProjectReactor");
    }
    add(reactor);
    if (getComponentByType(ProjectReferentialsLoader.class) == null) {
      add(DefaultProjectReferentialsLoader.class);
    }
  }

  private void addBatchComponents() {
    add(
      new ProjectReferentialsProvider(),
      ProjectSettings.class,
      Caches.class,

      // lang
      Languages.class,
      DefaultLanguagesReferential.class,

      // Measures
      AnalyzerMeasureCache.class,

      // file system
      InputPathCache.class,
      PathResolver.class,

      // issues
      AnalyzerIssueCache.class,

      // Syntax highlighting and symbols
      ComponentDataCache.class,

      // Duplications
      BlockCache.class,
      DuplicationCache.class,

      // Tests
      TestCaseCache.class,
      CoveragePerTestCache.class,

      ScanTaskObservers.class);
  }

  private void fixMavenExecutor() {
    if (getComponentByType(MavenPluginExecutor.class) == null) {
      add(FakeMavenPluginExecutor.class);
    }
  }

  private void addBatchExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new BatchExtensionFilter());
  }

  @Override
  protected void doAfterStart() {
    ProjectReactor tree = getComponentByType(ProjectReactor.class);
    scanRecursively(tree.getRoot());

    getComponentByType(ScanTaskObservers.class).notifyEndOfScanTask();
  }

  private void scanRecursively(ProjectDefinition module) {
    for (ProjectDefinition subModules : module.getSubProjects()) {
      scanRecursively(subModules);
    }
    scan(module);
  }

  @VisibleForTesting
  void scan(ProjectDefinition module) {
    new ModuleScanContainer(this, module).execute();
  }

  static class BatchExtensionFilter implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return ExtensionUtils.isType(extension, BatchComponent.class)
        && ExtensionUtils.isInstantiationStrategy(extension, InstantiationStrategy.PER_BATCH);
    }
  }
}
