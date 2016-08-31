/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.Version;
import org.sonar.ce.CeConfigurationModule;
import org.sonar.ce.CeHttpModule;
import org.sonar.ce.CeQueueModule;
import org.sonar.ce.CeTaskCommonsModule;
import org.sonar.ce.db.ReadOnlyPropertiesDao;
import org.sonar.ce.es.EsIndexerEnabler;
import org.sonar.ce.platform.ComputeEngineExtensionInstaller;
import org.sonar.ce.settings.ComputeEngineSettings;
import org.sonar.ce.user.CeUserSession;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DatabaseChecker;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.version.DatabaseVersion;
import org.sonar.process.Props;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.computation.queue.PurgeCeActivities;
import org.sonar.server.computation.task.projectanalysis.ProjectAnalysisTaskModule;
import org.sonar.server.computation.taskprocessor.CeTaskProcessorModule;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.event.NewAlerts;
import org.sonar.server.issue.IssueUpdater;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
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
import org.sonar.server.metric.CoreCustomMetrics;
import org.sonar.server.metric.DefaultMetricFinder;
import org.sonar.server.notification.DefaultNotificationManager;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.notification.email.AlertsEmailTemplate;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.platform.DatabaseServerCompatibility;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.ServerFileSystemImpl;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.platform.StartupMetadataProvider;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.platform.UrlSettings;
import org.sonar.server.platform.cluster.ClusterImpl;
import org.sonar.server.platform.cluster.ClusterProperties;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.privileged.PrivilegedPluginsBootstraper;
import org.sonar.server.plugins.privileged.PrivilegedPluginsStopper;
import org.sonar.server.qualityprofile.BuiltInProfiles;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileProjectLookup;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.CommonRuleDefinitionsImpl;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.DeprecatedRulesDefinitionLoader;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.search.EsSearchModule;
import org.sonar.server.setting.ProjectSettingsFactory;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.DefaultUserFinder;
import org.sonar.server.user.DeprecatedUserFinder;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexer;
import org.sonarqube.ws.Rules;

public class ComputeEngineContainerImpl implements ComputeEngineContainer {

  @CheckForNull
  private ComponentContainer level1;
  @CheckForNull
  private ComponentContainer level4;

  @Override
  public ComputeEngineContainer start(Props props) {
    this.level1 = new ComponentContainer();
    this.level1
      .add(props.rawProperties())
      .add(level1Components())
      .add(toArray(CorePropertyDefinitions.all()))
      .add(toArray(ClusterProperties.definitions()));
    configureFromModules(this.level1);
    this.level1.startComponents();

    ComponentContainer level2 = this.level1.createChild();
    level2.add(level2Components());
    configureFromModules(level2);
    level2.startComponents();

    ComponentContainer level3 = level2.createChild();
    level3.add(level3Components());
    configureFromModules(level3);
    level3.startComponents();

    this.level4 = level3.createChild();
    this.level4.add(level4Components());
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
    this.level1.stopComponents();
    return this;
  }

  @VisibleForTesting
  protected ComponentContainer getComponentContainer() {
    return level4;
  }

  private static Object[] level1Components() {
    Version apiVersion = ApiVersion.load(System2.INSTANCE);
    return new Object[] {
      ComputeEngineSettings.class,
      new SonarQubeVersion(apiVersion),
      SonarRuntimeImpl.forSonarQube(ApiVersion.load(System2.INSTANCE), SonarQubeSide.COMPUTE_ENGINE),
      UuidFactoryImpl.INSTANCE,
      UrlSettings.class,
      ClusterImpl.class,
      DefaultDatabase.class,
      DatabaseChecker.class,
      // must instantiate deprecated class in 5.2 and only this one (and not its replacement)
      // to avoid having two SqlSessionFactory instances
      org.sonar.core.persistence.MyBatis.class,
      DatabaseServerCompatibility.class,
      DatabaseVersion.class,
      PurgeProfiler.class,
      ServerFileSystemImpl.class,
      // no TempFolderCleaner.class, responsibility of Web Server
      new TempFolderProvider(),
      System2.INSTANCE,

      // user session
      CeUserSession.class,

      // DB
      DaoModule.class,
      // DbClient.class, replaced by CeDbClient to use ReadOnlyPropertiesDao instead of PropertiesDao
      ReadOnlyPropertiesDao.class,
      DbClient.class,
      // MigrationStepModule.class, DB maintenance, responsibility of Web Server

      // Elasticsearch
      EsSearchModule.class,

      // rules/qprofiles
      RuleIndex.class,
      ActiveRuleIndex.class,

      // issues
      IssueIndex.class,

      // Classes kept for backward compatibility of plugins/libs (like sonar-license) that are directly calling classes from the core
      // org.sonar.core.properties.PropertiesDao.class, replaced by ReadOnlyPropertiesDao (declared above) which is a ReadOnly
      // implementation
    };
  }

  private static Object[] level2Components() {
    return new Object[] {
      // add ReadOnlyPropertiesDao at level2 again so that it shadows PropertiesDao
      ReadOnlyPropertiesDao.class,
      DefaultServerUpgradeStatus.class,
      // no DatabaseMigrator.class, responsibility of Web Server

      // plugins
      PluginClassloaderFactory.class,
      CePluginJarExploder.class,
      PluginLoader.class,
      CePluginRepository.class,
      InstalledPluginReferentialFactory.class,
      ComputeEngineExtensionInstaller.class,

      // depends on plugins
      // RailsAppsDeployer.class,
      // JRubyI18n.class,
      DefaultI18n.class, // used by RuleI18nManager
      RuleI18nManager.class, // used by DebtRulesXMLImporter
      Durations.class, // used in Web Services and DebtCalculator
    };
  }

  private static Object[] level3Components() {
    return new Object[] {
      new StartupMetadataProvider(),
      PersistentSettings.class,
      UriReader.class,
      ServerImpl.class
    };
  }

  private static Object[] level4Components() {
    return new Object[] {
      // PluginDownloader.class, no use in CE
      // Views.class, UI
      ResourceTypes.class,
      DefaultResourceTypes.get(),
      // SettingsChangeNotifier.class, used only by JRuby
      // PageDecorations.class, used only by JRuby
      Periods.class, // used by JRuby and EvaluationResultTextConverterImpl
      // ServerWs.class, no Web Service in CE
      // BackendCleanup.class, DB maintenance, responsibility of Web Server
      // IndexDefinitions.class, ES maintenance, responsibility of Web Server
      // IndexCreator.class, ES maintenance, responsibility of Web Server

      // Activity
      ActivityIndexer.class,
      ActivityIndex.class,
      ActivityService.class,
      // ActivityIndexDefinition.class, ES maintenance, responsibility of Web Server

      // batch
      // BatchWsModule.class, no Web Service in CE

      // Dashboard, UI
      // [...]

      // update center, no Update Center in CE

      // quality profile
      ActiveRuleIndexer.class,
      XMLProfileParser.class,
      XMLProfileSerializer.class,
      AnnotationProfileParser.class,
      Rules.QProfiles.class,
      QProfileLookup.class,
      QProfileProjectOperations.class,
      QProfileProjectLookup.class,
      QProfileComparison.class,
      BuiltInProfiles.class,
      // RestoreBuiltInAction.class, no Web Service in CE
      // org.sonar.server.qualityprofile.ws.SearchAction.class, no Web Service in CE
      // SearchDataLoader.class, no Web Service in CE
      // SetDefaultAction.class, no Web Service in CE
      // ProjectsAction.class, no Web Service in CE
      // org.sonar.server.qualityprofile.ws.DeleteAction.class, no Web Service in CE
      // RenameAction.class, no Web Service in CE
      // CopyAction.class, no Web Service in CE
      // BackupAction.class, no Web Service in CE
      // RestoreAction.class, no Web Service in CE
      // CreateAction.class, no Web Service in CE
      // ImportersAction.class, no Web Service in
      // InheritanceAction.class, no Web Service in CE
      // ChangeParentAction.class, no Web Service in CE
      // ChangelogAction.class, no Web Service in CE
      // CompareAction.class, no Web Service in CE
      // ExportAction.class, no Web Service in CE
      // ExportersAction.class, no Web Service in CE
      // QProfilesWs.class, no Web Service in CE
      // ProfilesWs.class, no Web Service in CE
      // OldRestoreAction.class, no Web Service in CE
      // RuleActivationActions.class, no Web Service in CE
      // BulkRuleActivationActions.class, no Web Service in CE
      // ProjectAssociationActions.class, no Web Service in CE
      // RuleActivator.class, indirectly only used in Web Services
      // QProfileLoader.class, only used in QProfileService
      // QProfileExporters.class, only used in Web Service and QProfileService
      // QProfileService.class, depends on UserSession
      // RuleActivatorContextFactory.class, indirectly only used in Web Services
      // QProfileFactory.class, indirectly only used in Web Services
      // QProfileCopier.class, indirectly only used in Web Services
      // QProfileBackuper.class, indirectly only used in Web Services
      // QProfileReset.class, indirectly only used in Web Services

      // rule
      // RuleIndexDefinition.class, ES maintenance, responsibility of Web Server
      RuleIndexer.class,
      AnnotationRuleParser.class,
      XMLRuleParser.class,
      DefaultRuleFinder.class,
      // RuleOperations.class, supposed to be dropped in 4.4
      // RubyRuleService.class, used by JRuby
      RuleRepositories.class,
      DeprecatedRulesDefinitionLoader.class,
      CommonRuleDefinitionsImpl.class,
      RuleDefinitionsLoader.class,
      RulesDefinitionXmlLoader.class,
      // RuleUpdater.class, only used in Web Services
      // RuleCreator.class, only used from Ruby or Web Service
      // RuleDeleter.class, only used from Ruby or Web Service
      // RuleService.class, only used from Ruby or Web Service
      // org.sonar.server.rule.ws.UpdateAction.class, no Web Service in CE
      // RulesWs.class, no Web Service in CE
      // org.sonar.server.rule.ws.SearchAction.class, no Web Service in CE
      // org.sonar.server.rule.ws.ShowAction.class, no Web Service in CE
      // org.sonar.server.rule.ws.CreateAction.class, no Web Service in CE
      // org.sonar.server.rule.ws.DeleteAction.class, no Web Service in CE
      // org.sonar.server.rule.ws.ListAction.class, no Web Service in CE
      // TagsAction.class, no Web Service in CE
      // RuleMapper.class, only used in Web Services
      // ActiveRuleCompleter.class, only used in Web Services
      // RepositoriesAction.class, no Web Service in CE
      // org.sonar.server.rule.ws.AppAction.class, no Web Service in CE

      // languages
      Languages.class, // used by CommonRuleDefinitionsImpl
      // org.sonar.server.language.ws.ListAction.class, no Web Service in CE
      // LanguageWs.class, no Web Service in CE

      // measure
      // MeasureFilterFactory.class, used only in MeasureFilterEngine
      // MeasureFilterExecutor.class, used only in MeasureFilterEngine
      // MeasureFilterEngine.class, used only in JRubyFacade
      // MetricsWsModule.class, no Web Service in CE
      // MeasuresWsModule.class, no Web Service in CE
      // CustomMeasuresWsModule.class, no Web Service in CE
      // ProjectFilter.class, used only in GlobalDefaultDashboard
      // MyFavouritesFilter.class, used only in GlobalDefaultDashboard
      CoreCustomMetrics.class,
      DefaultMetricFinder.class,
      // TimeMachineWs.class, no Web Service in CE

      // quality gates
      // QualityGates.class, used only in Web Service and RegisterQualityGates
      // QgateProjectFinder.class, used only in Web Service
      // org.sonar.server.qualitygate.ws.ListAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.SearchAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.ShowAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.CreateAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.RenameAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.CopyAction.class, no Web Service in CE
      // DestroyAction.class, no Web Service in CE
      // SetAsDefaultAction.class, no Web Service in CE
      // UnsetDefaultAction.class, no Web Service in CE
      // SelectAction.class, no Web Service in CE
      // DeselectAction.class, no Web Service in CE
      // CreateConditionAction.class, no Web Service in CE
      // DeleteConditionAction.class, no Web Service in CE
      // UpdateConditionAction.class, no Web Service in CE
      // org.sonar.server.qualitygate.ws.AppAction.class, no Web Service in CE
      // ProjectStatusAction.class, no Web Service in CE
      // QGatesWs.class, no Web Service in CE

      // web services
      // WebServiceEngine.class, no Web Service in CE
      // WebServicesWs.class, no Web Service in CE

      // localization
      // L10nWs.class, no Web Service in CE

      // authentication
      // AuthenticationModule.class, only used for Web Server security

      // users
      // SecurityRealmFactory.class, only used for Web Server security
      DeprecatedUserFinder.class,
      // NewUserNotifier.class, only used in UI or UserUpdater
      DefaultUserFinder.class,
      // DefaultUserService.class, used only by Ruby
      // UserJsonWriter.class, used only in Web Service
      // UsersWs.class, no Web Service in CE
      // org.sonar.server.user.ws.CreateAction.class, no Web Service in CE
      // org.sonar.server.user.ws.UpdateAction.class, no Web Service in CE
      // org.sonar.server.user.ws.DeactivateAction.class, no Web Service in CE
      // org.sonar.server.user.ws.ChangePasswordAction.class, no Web Service in CE
      // CurrentAction.class, no Web Service in CE
      // org.sonar.server.user.ws.SearchAction.class, no Web Service in CE
      // org.sonar.server.user.ws.GroupsAction.class, no Web Service in CE
      // FavoritesWs.class, no Web Service in CE
      // UserPropertiesWs.class, no Web Service in CE
      // UserIndexDefinition.class, ES maintenance, responsibility of Web Server
      UserIndexer.class,
      UserIndex.class,
      // UserUpdater.class,
      // UserTokenModule.class,

      // groups
      // GroupMembershipFinder.class, // only used byGroupMembershipService
      // GroupMembershipService.class, // only used by Ruby
      // UserGroupsModule.class, no Web Service in CE

      // permissions
      PermissionRepository.class,
      // PermissionService.class, // depends on UserSession
      // PermissionUpdater.class, // depends on UserSession
      // PermissionFinder.class, used only in Web Service
      // PermissionsWsModule.class, no Web Service in CE

      // components
      // ProjectsWsModule.class, no Web Service in CE
      // ComponentsWsModule.class, no Web Service in CE
      // DefaultComponentFinder.class, only used in DefaultRubyComponentService
      // DefaultRubyComponentService.class, only used by Ruby
      ComponentFinder.class, // used in ComponentService
      ComponentService.class, // used in ReportSubmitter
      NewAlerts.class,
      NewAlerts.newMetadata(),
      ComponentCleanerService.class,

      // views
      // ViewIndexDefinition.class, ES maintenance, responsibility of Web Server
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      // IssueIndexDefinition.class,
      IssueIndexer.class,
      IssueAuthorizationIndexer.class,
      // ServerIssueStorage.class, indirectly used only in Web Services
      IssueUpdater.class, // used in Web Services and CE's DebtCalculator
      FunctionExecutor.class, // used by IssueWorkflow
      IssueWorkflow.class, // used in Web Services and CE's DebtCalculator
      // IssueCommentService.class, indirectly used only in Web Services
      // InternalRubyIssueService.class, indirectly used only in Web Services
      // IssueChangelogService.class, indirectly used only in Web Services
      // ActionService.class, indirectly used only in Web Services
      // IssueBulkChangeService.class, indirectly used only in Web Services
      // WsResponseCommonFormat.class, indirectly used only in Web Services
      // IssueWsModule.class, no Web Service in CE
      // IssueService.class, indirectly used only in Web Services
      // IssueQueryService.class, used only in Web Services and Ruby
      NewIssuesEmailTemplate.class,
      MyNewIssuesEmailTemplate.class,
      IssueChangesEmailTemplate.class,
      AlertsEmailTemplate.class,
      ChangesOnMyIssueNotificationDispatcher.class,
      ChangesOnMyIssueNotificationDispatcher.newMetadata(),
      NewIssuesNotificationDispatcher.class,
      NewIssuesNotificationDispatcher.newMetadata(),
      MyNewIssuesNotificationDispatcher.class,
      MyNewIssuesNotificationDispatcher.newMetadata(),
      DoNotFixNotificationDispatcher.class,
      DoNotFixNotificationDispatcher.newMetadata(),
      NewIssuesNotificationFactory.class, // used by SendIssueNotificationsStep
      EmailNotificationChannel.class,

      // IssueFilterWsModule.class, no Web Service in CE

      // action plan
      // ActionPlanWs.class, no Web Service in CE
      // ActionPlanService.class, no Web Service in CE

      // issues actions
      // AssignAction.class, no Web Service in CE
      // SetTypeAction.class, no Web Service in CE
      // PlanAction.class, no Web Service in CE
      // SetSeverityAction.class, no Web Service in CE
      // CommentAction.class, no Web Service in CE
      // TransitionAction.class, no Web Service in CE
      // AddTagsAction.class, no Web Service in CE
      // RemoveTagsAction.class, no Web Service in CE

      // technical debt
      // DebtModelService.class,
      // DebtModelBackup.class,
      DebtModelPluginRepository.class,
      // DebtModelXMLExporter.class,
      DebtRulesXMLImporter.class,

      // source
      // HtmlSourceDecorator.class, indirectly used only in Web Service
      // SourceService.class, indirectly used only in Web Service
      // SourcesWs.class, no Web Service in CE
      // org.sonar.server.source.ws.ShowAction.class, no Web Service in CE
      // LinesAction.class, no Web Service in CE
      // HashAction.class, no Web Service in CE
      // RawAction.class, no Web Service in CE
      // IndexAction.class, no Web Service in CE
      // ScmAction.class, no Web Service in CE

      // // Duplications
      // DuplicationsParser.class,
      // DuplicationsWs.class, no Web Service in CE
      // DuplicationsJsonWriter.class,
      // org.sonar.server.duplication.ws.ShowAction.class, no Web Service in CE

      // text
      // MacroInterpreter.class, only used in Web Services and Ruby
      // RubyTextService.class,

      // Notifications
      EmailSettings.class,
      NotificationService.class,
      NotificationCenter.class,
      DefaultNotificationManager.class,

      // Tests
      // CoverageService.class,
      // TestsWs.class,
      // CoveredFilesAction.class,
      // org.sonar.server.test.ws.ListAction.class,
      // TestIndexDefinition.class,
      // TestIndex.class,
      TestIndexer.class,

      // Properties
      // PropertiesWs.class, no Web Service in CE

      // TypeValidationModule.class, indirectly used only in Web Service

      // System
      ServerLogging.class,
      // RestartAction.class, no Web Service in CE
      // InfoAction.class, no Web Service in CE
      // UpgradesAction.class, no Web Service in CE
      // StatusAction.class, no Web Service in CE
      // SystemWs.class, no Web Service in CE
      // SystemMonitor.class, no Monitor in CE, responsibility of Web Server
      // SonarQubeMonitor.class, no Monitor in CE, responsibility of Web Server
      // EsMonitor.class, no Monitor in CE, responsibility of Web Server
      // PluginsMonitor.class, no Monitor in CE, responsibility of Web Server
      // JvmPropsMonitor.class, no Monitor in CE, responsibility of Web Server
      // DatabaseMonitor.class, no Monitor in CE, responsibility of Web Server
      // MigrateDbAction.class, no Web Service in CE
      // ChangeLogLevelAction.class, no Web Service in CE
      // DbMigrationStatusAction.class, no Web Service in CE

      // Plugins WS
      // PluginWSCommons.class, no Web Service in CE
      // PluginUpdateAggregator.class, no Web Service in CE
      // InstalledAction.class, no Web Service in CE
      // AvailableAction.class, no Web Service in CE
      // UpdatesAction.class, no Web Service in CE
      // PendingAction.class, no Web Service in CE
      // InstallAction.class, no Web Service in CE
      // org.sonar.server.plugins.ws.UpdateAction.class, no Web Service in CE
      // UninstallAction.class, no Web Service in CE
      // CancelAllAction.class, no Web Service in CE
      // PluginsWs.class, no Web Service in CE

      // Views plugin
      // ViewsBootstrap.class, Views not supported in 5.5
      // ViewsStopper.class, Views not supported in 5.5

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
      // CeWsModule.class, no Web Service in CE

      ProjectSettingsFactory.class,

      // UI
      // GlobalNavigationAction.class, no Web Service in CE
      // SettingsNavigationAction.class, no Web Service in CE
      // ComponentNavigationAction.class, no Web Service in CE
      // NavigationWs.class, no Web Service in CE
    };
  }

  private static Object[] startupComponents() {
    return new Object[] {
      // IndexerStartupTask.class, ES maintenance, responsibility of Web Server
      EsIndexerEnabler.class,
      // RegisterMetrics.class, DB maintenance, responsibility of Web Server
      // RegisterQualityGates.class, DB maintenance, responsibility of Web Server
      // RegisterRules.class, DB maintenance, responsibility of Web Server
      // RegisterQualityProfiles.class, DB maintenance, responsibility of Web Server
      // GeneratePluginIndex.class, ES maintenance, responsibility of Web Server
      // RegisterNewMeasureFilters.class, DB maintenance, responsibility of Web Server
      // RegisterDashboards.class, UI related, anyway, DB maintenance, responsibility of Web Server
      // RegisterPermissionTemplates.class, DB maintenance, responsibility of Web Server
      // RenameDeprecatedPropertyKeys.class, DB maintenance, responsibility of Web Server
      LogServerId.class,
      // RegisterServletFilters.class, Web Server only
      // RegisterIssueFilters.class, DB maintenance, responsibility of Web Server
      // RenameIssueWidgets.class, UI related, anyway, DB maintenance, responsibility of Web Server
      ServerLifecycleNotifier.class,
      PurgeCeActivities.class,
      // DisplayLogOnDeprecatedProjects.class, responsibility of Web Server
      // ClearRulesOverloadedDebt.class, DB maintenance, responsibility of Web Server
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
