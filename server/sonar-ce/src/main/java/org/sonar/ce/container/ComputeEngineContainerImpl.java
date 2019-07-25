/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.container;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.server.profile.BuiltInQualityProfileAnnotationLoader;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.CeConfigurationModule;
import org.sonar.ce.CeDistributedInformationImpl;
import org.sonar.ce.CeHttpModule;
import org.sonar.ce.CeQueueModule;
import org.sonar.ce.CeTaskCommonsModule;
import org.sonar.ce.StandaloneCeDistributedInformation;
import org.sonar.ce.async.SynchronousAsyncExecution;
import org.sonar.ce.cleaning.CeCleaningModule;
import org.sonar.ce.cleaning.NoopCeCleaningSchedulerImpl;
import org.sonar.ce.db.ReadOnlyPropertiesDao;
import org.sonar.ce.logging.CeProcessLogging;
import org.sonar.ce.monitoring.CEQueueStatusImpl;
import org.sonar.ce.monitoring.DistributedCEQueueStatusImpl;
import org.sonar.ce.platform.CECoreExtensionsInstaller;
import org.sonar.ce.platform.ComputeEngineExtensionInstaller;
import org.sonar.ce.platform.DatabaseCompatibility;
import org.sonar.ce.queue.PurgeCeActivities;
import org.sonar.ce.task.projectanalysis.ProjectAnalysisTaskModule;
import org.sonar.ce.task.projectanalysis.analysis.ProjectConfigurationFactory;
import org.sonar.ce.task.projectanalysis.issue.AdHocRuleCreator;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationModule;
import org.sonar.ce.taskprocessor.CeProcessingScheduler;
import org.sonar.ce.taskprocessor.CeTaskProcessorModule;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.extension.CoreExtensionRepositoryImpl;
import org.sonar.core.extension.CoreExtensionsLoader;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.MyBatis;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.Props;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.config.ConfigurationProvider;
import org.sonar.server.es.EsModule;
import org.sonar.server.es.ProjectIndexersImpl;
import org.sonar.server.extension.CoreExtensionBootstraper;
import org.sonar.server.extension.CoreExtensionStopper;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotificationModule;
import org.sonar.server.issue.notification.MyNewIssuesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesNotificationHandler;
import org.sonar.server.issue.notification.NewIssuesEmailTemplate;
import org.sonar.server.issue.notification.NewIssuesNotificationHandler;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.l18n.ServerI18n;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.metric.CoreCustomMetrics;
import org.sonar.server.metric.DefaultMetricFinder;
import org.sonar.server.notification.DefaultNotificationManager;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.organization.BillingValidationsProxyImpl;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.ServerFileSystemImpl;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.platform.StartupMetadataProvider;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.platform.UrlSettings;
import org.sonar.server.platform.WebServerImpl;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.monitoring.DbSection;
import org.sonar.server.platform.monitoring.cluster.ProcessInfoProvider;
import org.sonar.server.platform.serverid.JdbcUrlSanitizer;
import org.sonar.server.platform.serverid.ServerIdChecksum;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.property.InternalPropertiesImpl;
import org.sonar.server.qualitygate.QualityGateEvaluatorImpl;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.notification.QGChangeEmailTemplate;
import org.sonar.server.qualitygate.notification.QGChangeNotificationHandler;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.setting.DatabaseSettingLoader;
import org.sonar.server.setting.DatabaseSettingsEnabler;
import org.sonar.server.setting.ThreadLocalSettings;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.util.OkHttpClientProvider;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.webhook.WebhookModule;
import org.sonarqube.ws.Rules;

import static java.util.Objects.requireNonNull;
import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel4OrNone;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;

public class ComputeEngineContainerImpl implements ComputeEngineContainer {

  private ComputeEngineStatus computeEngineStatus;
  @CheckForNull
  private ComponentContainer level1;
  @CheckForNull
  private ComponentContainer level4;

  @Override
  public void setComputeEngineStatus(ComputeEngineStatus computeEngineStatus) {
    this.computeEngineStatus = computeEngineStatus;
  }

  @Override
  public ComputeEngineContainer start(Props props) {
    this.level1 = new ComponentContainer();
    populateLevel1(this.level1, props, requireNonNull(computeEngineStatus));
    configureFromModules(this.level1);
    startLevel1(this.level1);

    ComponentContainer level2 = this.level1.createChild();
    populateLevel2(level2);
    configureFromModules(level2);
    startLevel2(level2);

    ComponentContainer level3 = level2.createChild();
    populateLevel3(level3);
    configureFromModules(level3);
    startLevel3(level3);

    this.level4 = level3.createChild();
    populateLevel4(this.level4, props);
    configureFromModules(this.level4);
    startLevel4(this.level4);

    startupTasks();

    return this;
  }

  private static void startLevel1(ComponentContainer level1) {
    level1.getComponentByType(CoreExtensionsLoader.class)
      .load();
    level1.getComponentByType(CECoreExtensionsInstaller.class)
      .install(level1, hasPlatformLevel(1), noAdditionalSideFilter());

    level1.startComponents();
  }

  private static void startLevel2(ComponentContainer level2) {
    level2.getComponentByType(CECoreExtensionsInstaller.class)
      .install(level2, hasPlatformLevel(2), noAdditionalSideFilter());

    level2.startComponents();
  }

  private static void startLevel3(ComponentContainer level3) {
    level3.getComponentByType(CECoreExtensionsInstaller.class)
      .install(level3, hasPlatformLevel(3), noAdditionalSideFilter());

    level3.startComponents();
  }

  private static void startLevel4(ComponentContainer level4) {
    level4.getComponentByType(CECoreExtensionsInstaller.class)
      .install(level4, hasPlatformLevel4OrNone(), noAdditionalSideFilter());
    level4.getComponentByType(ServerExtensionInstaller.class)
      .installExtensions(level4);

    level4.startComponents();

    PlatformEditionProvider editionProvider = level4.getComponentByType(PlatformEditionProvider.class);
    Loggers.get(ComputeEngineContainerImpl.class)
      .info("Running {} edition", editionProvider.get().map(EditionProvider.Edition::getLabel).orElse(""));
  }

  private void startupTasks() {
    ComponentContainer startupLevel = this.level4.createChild();
    startupLevel.add(startupComponents());
    startupLevel.startComponents();
    // done in PlatformLevelStartup
    ServerLifecycleNotifier serverLifecycleNotifier = startupLevel.getComponentByType(ServerLifecycleNotifier.class);
    if (serverLifecycleNotifier != null) {
      serverLifecycleNotifier.notifyStart();
    }
    startupLevel.stopComponents();
  }

  @Override
  public ComputeEngineContainer stopWorkers() {
    if (level4 != null) {
      // try to graceful stop in-progress tasks
      CeProcessingScheduler ceProcessingScheduler = level4.getComponentByType(CeProcessingScheduler.class);
      ceProcessingScheduler.gracefulStopScheduling();
    }
    return this;
  }

  @Override
  public ComputeEngineContainer stop() {
    if (level4 != null) {
      // try to graceful but quick stop in-progress tasks
      CeProcessingScheduler ceProcessingScheduler = level4.getComponentByType(CeProcessingScheduler.class);
      ceProcessingScheduler.hardStopScheduling();
    }
    this.level1.stopComponents();
    return this;
  }

  @VisibleForTesting
  protected ComponentContainer getComponentContainer() {
    return level4;
  }

  private static void populateLevel1(ComponentContainer container, Props props, ComputeEngineStatus computeEngineStatus) {
    Version apiVersion = MetadataLoader.loadVersion(System2.INSTANCE);
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);
    container.add(
      props.rawProperties(),
      ThreadLocalSettings.class,
      new ConfigurationProvider(),
      new SonarQubeVersion(apiVersion),
      SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.COMPUTE_ENGINE, edition),
      CeProcessLogging.class,
      UuidFactoryImpl.INSTANCE,
      NetworkUtilsImpl.INSTANCE,
      WebServerImpl.class,
      LogbackHelper.class,
      DefaultDatabase.class,
      MyBatis.class,
      PurgeProfiler.class,
      ServerFileSystemImpl.class,
      new TempFolderProvider(),
      System2.INSTANCE,
      Clock.systemDefaultZone(),

      // DB
      DaoModule.class,
      ReadOnlyPropertiesDao.class,
      DBSessionsImpl.class,
      DbClient.class,

      // Elasticsearch
      EsModule.class,

      // rules/qprofiles
      RuleIndex.class,

      new OkHttpClientProvider(),
      computeEngineStatus,

      CoreExtensionRepositoryImpl.class,
      CoreExtensionsLoader.class,
      CECoreExtensionsInstaller.class);
    container.add(toArray(CorePropertyDefinitions.all()));
  }

  private static void populateLevel2(ComponentContainer container) {
    container.add(
      MigrationConfigurationModule.class,
      DatabaseVersion.class,
      DatabaseCompatibility.class,

      DatabaseSettingLoader.class,
      DatabaseSettingsEnabler.class,
      UrlSettings.class,

      // add ReadOnlyPropertiesDao at level2 again so that it shadows PropertiesDao
      ReadOnlyPropertiesDao.class,

      // plugins
      PluginClassloaderFactory.class,
      CePluginJarExploder.class,
      PluginLoader.class,
      CePluginRepository.class,
      InstalledPluginReferentialFactory.class,
      ComputeEngineExtensionInstaller.class,

      // depends on plugins
      ServerI18n.class, // used by RuleI18nManager
      RuleI18nManager.class, // used by DebtRulesXMLImporter
      Durations.class // used in Web Services and DebtCalculator
    );
  }

  private static void populateLevel3(ComponentContainer container) {
    container.add(
      new StartupMetadataProvider(),
      JdbcUrlSanitizer.class,
      ServerIdChecksum.class,
      UriReader.class,
      ServerImpl.class,
      DefaultOrganizationProviderImpl.class,
      SynchronousAsyncExecution.class,
      OrganizationFlagsImpl.class);
  }

  private static void populateLevel4(ComponentContainer container, Props props) {
    container.add(
      ResourceTypes.class,
      DefaultResourceTypes.get(),
      BillingValidationsProxyImpl.class,

      // quality profile
      ActiveRuleIndexer.class,
      XMLProfileParser.class,
      XMLProfileSerializer.class,
      AnnotationProfileParser.class,
      BuiltInQualityProfileAnnotationLoader.class,
      Rules.QProfiles.class,

      // rule
      AnnotationRuleParser.class,
      XMLRuleParser.class,
      DefaultRuleFinder.class,
      RulesDefinitionXmlLoader.class,
      AdHocRuleCreator.class,
      RuleIndexer.class,

      // languages
      Languages.class, // used by CommonRuleDefinitionsImpl

      // measure
      CoreCustomMetrics.class,
      DefaultMetricFinder.class,

      UserIndexer.class,
      UserIndex.class,

      // components,
      FavoriteUpdater.class,
      ProjectIndexersImpl.class,
      QGChangeNotificationHandler.class,
      QGChangeNotificationHandler.newMetadata(),
      ProjectMeasuresIndexer.class,
      ComponentIndexer.class,

      // views
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      IssueStorage.class,
      IssueIndexer.class,
      IssueIteratorFactory.class,
      IssueFieldsSetter.class, // used in Web Services and CE's DebtCalculator
      FunctionExecutor.class, // used by IssueWorkflow
      IssueWorkflow.class, // used in Web Services and CE's DebtCalculator
      NewIssuesEmailTemplate.class,
      MyNewIssuesEmailTemplate.class,
      NewIssuesNotificationHandler.class,
      NewIssuesNotificationHandler.newMetadata(),
      MyNewIssuesNotificationHandler.class,
      MyNewIssuesNotificationHandler.newMetadata(),
      IssuesChangesNotificationModule.class,

      // Notifications
      QGChangeEmailTemplate.class,
      EmailSettings.class,
      NotificationService.class,
      DefaultNotificationManager.class,
      EmailNotificationChannel.class,
      ReportAnalysisFailureNotificationModule.class,

      // System
      ServerLogging.class,

      // SonarSource editions
      PlatformEditionProvider.class,

      // privileged plugins
      CoreExtensionBootstraper.class,
      CoreExtensionStopper.class,

      // Compute engine (must be after Views and Developer Cockpit)
      CeConfigurationModule.class,
      CeQueueModule.class,
      CeHttpModule.class,
      CeTaskCommonsModule.class,
      ProjectAnalysisTaskModule.class,
      CeTaskProcessorModule.class,
      OfficialDistribution.class,

      InternalPropertiesImpl.class,
      ProjectConfigurationFactory.class,

      // webhooks
      WebhookModule.class,

      QualityGateFinder.class,
      QualityGateEvaluatorImpl.class

    );

    if (props.valueAsBoolean(SONARCLOUD_ENABLED.getKey())) {
      // no cleaning job on sonarcloud and no distributed information
      container.add(
        NoopCeCleaningSchedulerImpl.class,
        StandaloneCeDistributedInformation.class,
        CEQueueStatusImpl.class);
    } else if (props.valueAsBoolean(CLUSTER_ENABLED.getKey())) {
      container.add(
        CeCleaningModule.class,

        // system health
        CeDistributedInformationImpl.class,

        // system info
        DbSection.class,
        ProcessInfoProvider.class,

        DistributedCEQueueStatusImpl.class);
    } else {
      container.add(
        CeCleaningModule.class,
        StandaloneCeDistributedInformation.class,
        CEQueueStatusImpl.class);
    }
  }

  private static Object[] startupComponents() {
    return new Object[] {
      ServerLifecycleNotifier.class,
      PurgeCeActivities.class
    };
  }

  private static Object[] toArray(List<?> list) {
    return list.toArray(new Object[list.size()]);
  }

  private static void configureFromModules(ComponentContainer container) {
    List<Module> modules = container.getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(container);
    }
  }
}
