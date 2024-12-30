/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.measures.Metrics;
import org.sonar.api.resources.Languages;
import org.sonar.core.config.ScannerProperties;
import org.sonar.core.language.LanguagesProvider;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.ExtensionMatcher;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.PostJobExtensionDictionary;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdExecutor;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.issue.IssueFilterExtensionDictionary;
import org.sonar.scanner.issue.IssueFilters;
import org.sonar.scanner.mediumtest.AnalysisObservers;
import org.sonar.scanner.postjob.DefaultPostJobContext;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.postjob.PostJobsExecutor;
import org.sonar.scanner.qualitygate.QualityGateCheck;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.scan.filesystem.FileIndexer;
import org.sonar.scanner.scan.filesystem.InputFileFilterRepository;
import org.sonar.scanner.scan.filesystem.LanguageDetection;
import org.sonar.scanner.scan.filesystem.ProjectFileIndexer;
import org.sonar.scanner.scm.ScmPublisher;
import org.sonar.scanner.sensor.ProjectSensorContext;
import org.sonar.scanner.sensor.ProjectSensorExtensionDictionary;
import org.sonar.scanner.sensor.ProjectSensorOptimizer;
import org.sonar.scanner.sensor.ProjectSensorsExecutor;

import static org.sonar.api.batch.InstantiationStrategy.PER_BATCH;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isDeprecatedScannerSide;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isScannerSide;

@Priority(2)
public class SpringProjectScanContainer extends SpringComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(SpringProjectScanContainer.class);

  public SpringProjectScanContainer(SpringComponentContainer parentContainer) {
    super(parentContainer);
  }

  @Override
  protected void doBeforeStart() {
    Set<String> languages = getParentComponentByType(LanguageDetection.class).getDetectedLanguages();
    installPluginsForLanguages(languages);
    addScannerComponents();
  }

  private void installPluginsForLanguages(Set<String> languageKeys) {
    ScannerPluginRepository pluginRepository = getParentComponentByType(ScannerPluginRepository.class);
    Collection<PluginInfo> languagePlugins = pluginRepository.installPluginsForLanguages(languageKeys);
    for (PluginInfo pluginInfo : languagePlugins) {
      Plugin instance = pluginRepository.getPluginInstance(pluginInfo.getKey());
      addExtension(pluginInfo, instance);
    }
    getParentComponentByType(ExtensionInstaller.class)
      .installExtensionsForPlugins(this, getScannerProjectExtensionsFilter(), languagePlugins);
  }

  private void addScannerComponents() {
    add(
      ProjectConfigurationProvider.class,

      ScanProperties.class,

      // lang
      LanguagesProvider.class,

      // rules
      QProfileVerifier.class,

      ScannerProperties.class,

      // QualityGate check
      QualityGateCheck.class,

      // PostJobs
      PostJobsExecutor.class,
      PostJobOptimizer.class,
      DefaultPostJobContext.class,
      PostJobExtensionDictionary.class,

      // Sensors
      SensorStrategy.class,
      ProjectSensorContext.class,
      ProjectSensorExtensionDictionary.class,
      ProjectSensorsExecutor.class,
      ProjectSensorOptimizer.class,

      // Issue filters
      IssueFilterExtensionDictionary.class,

      AnalysisObservers.class,

      // file system
      InputFileFilterRepository.class,
      FileIndexer.class,
      ProjectFileIndexer.class);
  }

  static ExtensionMatcher getScannerProjectExtensionsFilter() {
    return extension -> {
      if (isDeprecatedScannerSide(extension)) {
        return isInstantiationStrategy(extension, PER_BATCH);
      }
      return isScannerSide(extension);
    };
  }

  @Override
  protected void doAfterStart() {
    getParentComponentByType(ScannerMetrics.class).addPluginMetrics(getComponentsByType(Metrics.class));
    getParentComponentByType(IssueFilters.class).registerFilters(getComponentByType(IssueFilterExtensionDictionary.class).selectIssueFilters());

    getComponentByType(ProjectLock.class).tryLock();

    // NOTE: ProjectBuilders executed here will have any changes they make to the ProjectReactor discarded.
    ProjectBuilder[] phase2ProjectBuilders = getComponentsByType(ProjectBuilder.class).toArray(new ProjectBuilder[0]);
    getComponentByType(ProjectBuildersExecutor.class).executeProjectBuilders(phase2ProjectBuilders, getComponentByType(ProjectReactor.class),
      "Executing phase 2 project builders");

    getComponentByType(ProjectFileIndexer.class).index();
    GlobalAnalysisMode analysisMode = getComponentByType(GlobalAnalysisMode.class);
    InputModuleHierarchy tree = getComponentByType(InputModuleHierarchy.class);
    ScanProperties properties = getComponentByType(ScanProperties.class);

    if (getComponentByType(Languages.class).all().length == 0) {
      LOG.warn("No language plugins are installed.");
    }

    // Log detected languages and their profiles after FS is indexed and languages detected
    getComponentByType(QProfileVerifier.class).execute();

    scanRecursively(tree, tree.root());

    LOG.info("------------- Run sensors on project");
    getComponentByType(ProjectSensorsExecutor.class).execute();

    getComponentByType(ScmPublisher.class).publish();

    getComponentByType(CpdExecutor.class).execute();
    getComponentByType(ReportPublisher.class).execute();

    if (properties.shouldWaitForQualityGate()) {
      LOG.info("------------- Check Quality Gate status");
      getComponentByType(QualityGateCheck.class).await();
    }

    getComponentByType(PostJobsExecutor.class).execute();

    if (analysisMode.isMediumTest()) {
      getComponentByType(AnalysisObservers.class).notifyEndOfScanTask();
    }
  }

  private void scanRecursively(InputModuleHierarchy tree, DefaultInputModule module) {
    for (DefaultInputModule child : tree.children(module)) {
      scanRecursively(tree, child);
    }
    LOG.info("------------- Run sensors on module {}", module.definition().getName());
    scan(module);
  }

  void scan(DefaultInputModule module) {
    new SpringModuleScanContainer(this, module).execute();
  }

}
