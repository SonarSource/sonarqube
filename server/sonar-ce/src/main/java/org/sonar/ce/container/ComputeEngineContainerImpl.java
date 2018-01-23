/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.internal.ApiVersion;
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
import org.sonar.ce.CeConfigurationModule;
import org.sonar.ce.CeDistributedInformationImpl;
import org.sonar.ce.CeHttpModule;
import org.sonar.ce.CeQueueModule;
import org.sonar.ce.CeTaskCommonsModule;
import org.sonar.ce.StandaloneCeDistributedInformation;
import org.sonar.ce.async.SynchronousAsyncExecution;
import org.sonar.ce.cleaning.CeCleaningModule;
import org.sonar.ce.db.ReadOnlyPropertiesDao;
import org.sonar.ce.log.CeProcessLogging;
import org.sonar.ce.notification.ReportAnalysisFailureNotificationModule;
import org.sonar.ce.platform.ComputeEngineExtensionInstaller;
import org.sonar.ce.queue.CeQueueCleaner;
import org.sonar.ce.queue.PurgeCeActivities;
import org.sonar.ce.settings.ProjectConfigurationFactory;
import org.sonar.ce.taskprocessor.CeProcessingScheduler;
import org.sonar.ce.taskprocessor.CeTaskProcessorModule;
import org.sonar.ce.user.CeUserSession;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.server.config.ConfigurationProvider;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DatabaseChecker;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.Props;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.computation.task.projectanalysis.ProjectAnalysisTaskModule;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.event.NewAlerts;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.ChangesOnMyIssueNotificationDispatcher;
import org.sonar.server.issue.notification.DoNotFixNotificationDispatcher;
import org.sonar.server.issue.notification.IssueChangesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesNotificationDispatcher;
import org.sonar.server.issue.notification.NewIssuesEmailTemplate;
import org.sonar.server.issue.notification.NewIssuesNotificationDispatcher;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresIndexer;
import org.sonar.server.metric.CoreCustomMetrics;
import org.sonar.server.metric.DefaultMetricFinder;
import org.sonar.server.notification.DefaultNotificationManager;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.notification.email.AlertsEmailTemplate;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.organization.BillingValidationsProxyImpl;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.ws.template.DefaultTemplatesResolverImpl;
import org.sonar.server.platform.DatabaseServerCompatibility;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.ServerFileSystemImpl;
import org.sonar.server.platform.ServerIdManager;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.platform.StartupMetadataProvider;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.platform.UrlSettings;
import org.sonar.server.platform.WebServerImpl;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;
import org.sonar.server.platform.monitoring.DbSection;
import org.sonar.server.platform.monitoring.OfficialDistribution;
import org.sonar.server.platform.monitoring.cluster.ProcessInfoProvider;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.privileged.PrivilegedPluginsBootstraper;
import org.sonar.server.plugins.privileged.PrivilegedPluginsStopper;
import org.sonar.server.property.InternalPropertiesImpl;
import org.sonar.server.qualitygate.QualityGateModule;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.CommonRuleDefinitionsImpl;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.DeprecatedRulesDefinitionLoader;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.EsSearchModule;
import org.sonar.server.setting.DatabaseSettingLoader;
import org.sonar.server.setting.DatabaseSettingsEnabler;
import org.sonar.server.setting.ThreadLocalSettings;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.DefaultUserFinder;
import org.sonar.server.user.DeprecatedUserFinder;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.util.OkHttpClientProvider;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.webhook.WebhookModule;
import org.sonarqube.ws.Rules;

import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;

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
    this.level1.startComponents();

    ComponentContainer level2 = this.level1.createChild();
    populateLevel2(level2);
    configureFromModules(level2);
    level2.startComponents();

    ComponentContainer level3 = level2.createChild();
    populateLevel3(level3);
    configureFromModules(level3);
    level3.startComponents();

    this.level4 = level3.createChild();
    populateLevel4(this.level4, props);

    configureFromModules(this.level4);
    ServerExtensionInstaller extensionInstaller = this.level4.getComponentByType(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(this.level4);
    this.level4.startComponents();

    startupTasks();

    return this;
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
  public ComputeEngineContainer stop() {
    if (level4 != null) {
      // try to graceful stop in-progress tasks
      CeProcessingScheduler ceProcessingScheduler = level4.getComponentByType(CeProcessingScheduler.class);
      ceProcessingScheduler.stopScheduling();
    }
    this.level1.stopComponents();
    return this;
  }

  @VisibleForTesting
  protected ComponentContainer getComponentContainer() {
    return level4;
  }

  private static void populateLevel1(ComponentContainer container, Props props, ComputeEngineStatus computeEngineStatus) {
    Version apiVersion = ApiVersion.load(System2.INSTANCE);
    container.add(
      props.rawProperties(),
      ThreadLocalSettings.class,
      new ConfigurationProvider(),
      new SonarQubeVersion(apiVersion),
      SonarRuntimeImpl.forSonarQube(ApiVersion.load(System2.INSTANCE), SonarQubeSide.COMPUTE_ENGINE),
      CeProcessLogging.class,
      UuidFactoryImpl.INSTANCE,
      NetworkUtilsImpl.INSTANCE,
      WebServerImpl.class,
      LogbackHelper.class,
      DefaultDatabase.class,
      DatabaseChecker.class,
      // must instantiate deprecated class in 5.2 and only this one (and not its replacement)
      // to avoid having two SqlSessionFactory instances
      org.sonar.core.persistence.MyBatis.class,
      PurgeProfiler.class,
      ServerFileSystemImpl.class,
      new TempFolderProvider(),
      System2.INSTANCE,

      // user session
      CeUserSession.class,

      // DB
      DaoModule.class,
      ReadOnlyPropertiesDao.class,
      DBSessionsImpl.class,
      DbClient.class,

      // Elasticsearch
      EsSearchModule.class,

      // rules/qprofiles
      RuleIndex.class,

      // issues
      IssueIndex.class,

      new OkHttpClientProvider(),
      computeEngineStatus);
    container.add(toArray(CorePropertyDefinitions.all()));
  }

  private static void populateLevel2(ComponentContainer container) {
    container.add(
      MigrationConfigurationModule.class,
      DatabaseVersion.class,
      DatabaseServerCompatibility.class,

      DatabaseSettingLoader.class,
      DatabaseSettingsEnabler.class,
      UrlSettings.class,

      // add ReadOnlyPropertiesDao at level2 again so that it shadows PropertiesDao
      ReadOnlyPropertiesDao.class,
      DefaultServerUpgradeStatus.class,

      // plugins
      PluginClassloaderFactory.class,
      CePluginJarExploder.class,
      PluginLoader.class,
      CePluginRepository.class,
      InstalledPluginReferentialFactory.class,
      ComputeEngineExtensionInstaller.class,

      // depends on plugins
      DefaultI18n.class, // used by RuleI18nManager
      RuleI18nManager.class, // used by DebtRulesXMLImporter
      Durations.class // used in Web Services and DebtCalculator
    );
  }

  private static void populateLevel3(ComponentContainer container) {
    container.add(
      new StartupMetadataProvider(),
      ServerIdManager.class,
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
      Periods.class,
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
      DeprecatedRulesDefinitionLoader.class,
      CommonRuleDefinitionsImpl.class,
      RuleDefinitionsLoader.class,
      RulesDefinitionXmlLoader.class,

      // languages
      Languages.class, // used by CommonRuleDefinitionsImpl

      // measure
      CoreCustomMetrics.class,
      DefaultMetricFinder.class,
      ProjectMeasuresIndex.class,

      // users
      DeprecatedUserFinder.class,
      DefaultUserFinder.class,
      UserIndexer.class,
      UserIndex.class,

      // permissions
      DefaultTemplatesResolverImpl.class,
      PermissionTemplateService.class,
      PermissionUpdater.class,
      UserPermissionChanger.class,
      GroupPermissionChanger.class,

      // components
      ComponentFinder.class, // used in ComponentService
      NewAlerts.class,
      NewAlerts.newMetadata(),
      ProjectMeasuresIndexer.class,
      ComponentIndexer.class,

      // views
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      IssueIndexer.class,
      IssueIteratorFactory.class,
      PermissionIndexer.class,
      IssueFieldsSetter.class, // used in Web Services and CE's DebtCalculator
      FunctionExecutor.class, // used by IssueWorkflow
      IssueWorkflow.class, // used in Web Services and CE's DebtCalculator
      NewIssuesEmailTemplate.class,
      MyNewIssuesEmailTemplate.class,
      IssueChangesEmailTemplate.class,
      ChangesOnMyIssueNotificationDispatcher.class,
      ChangesOnMyIssueNotificationDispatcher.newMetadata(),
      NewIssuesNotificationDispatcher.class,
      NewIssuesNotificationDispatcher.newMetadata(),
      MyNewIssuesNotificationDispatcher.class,
      MyNewIssuesNotificationDispatcher.newMetadata(),
      DoNotFixNotificationDispatcher.class,
      DoNotFixNotificationDispatcher.newMetadata(),
      NewIssuesNotificationFactory.class, // used by SendIssueNotificationsStep

      // technical debt
      DebtModelPluginRepository.class,
      DebtRulesXMLImporter.class,

      // Notifications
      AlertsEmailTemplate.class,
      EmailSettings.class,
      NotificationService.class,
      DefaultNotificationManager.class,
      EmailNotificationChannel.class,
      ReportAnalysisFailureNotificationModule.class,

      // Tests
      TestIndexer.class,

      // System
      ServerLogging.class,

      // privileged plugins
      PrivilegedPluginsBootstraper.class,
      PrivilegedPluginsStopper.class,

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

      QualityGateModule.class,

      // cleaning
      CeCleaningModule.class);

    if (props.valueAsBoolean(CLUSTER_ENABLED.getKey())) {
      container.add(
        // system health
        CeDistributedInformationImpl.class,

        // system info
        DbSection.class,
        ProcessInfoProvider.class);
    } else {
      container.add(StandaloneCeDistributedInformation.class);
    }
  }

  private static Object[] startupComponents() {
    return new Object[] {
      ServerLifecycleNotifier.class,
      PurgeCeActivities.class,
      CeQueueCleaner.class
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
