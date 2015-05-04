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
package org.sonar.server.platform;

import com.google.common.collect.Lists;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.platform.ComponentContainer;
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
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.computation.dbcleaner.DefaultPurgeTask;
import org.sonar.core.computation.dbcleaner.IndexPurgeListener;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.issue.IssueFilterSerializer;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.measure.db.MeasureFilterDao;
import org.sonar.core.metric.DefaultMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DefaultDatabase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.test.TestPlanPerspectiveLoader;
import org.sonar.core.test.TestablePerspectiveLoader;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.RubyQProfileActivityService;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityIndexDefinition;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.activity.ws.ActivitiesWebService;
import org.sonar.server.activity.ws.ActivityMapping;
import org.sonar.server.authentication.ws.AuthenticationWs;
import org.sonar.server.batch.BatchIndex;
import org.sonar.server.batch.BatchWs;
import org.sonar.server.batch.GlobalRepositoryAction;
import org.sonar.server.batch.IssuesAction;
import org.sonar.server.batch.ProjectRepositoryAction;
import org.sonar.server.batch.ProjectRepositoryLoader;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.DefaultComponentFinder;
import org.sonar.server.component.DefaultRubyComponentService;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.ComponentIndexDao;
import org.sonar.server.component.db.ComponentLinkDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.component.ws.ComponentAppAction;
import org.sonar.server.component.ws.ComponentsWs;
import org.sonar.server.component.ws.EventsWs;
import org.sonar.server.component.ws.ProjectsWs;
import org.sonar.server.component.ws.ResourcesWs;
import org.sonar.server.computation.ComputationThreadLauncher;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.ReportQueueCleaner;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.computation.ws.ComputationWebService;
import org.sonar.server.computation.ws.HistoryWsAction;
import org.sonar.server.computation.ws.IsQueueEmptyWebService;
import org.sonar.server.computation.ws.QueueWsAction;
import org.sonar.server.computation.ws.SubmitReportWsAction;
import org.sonar.server.config.ws.PropertiesWs;
import org.sonar.server.dashboard.db.DashboardDao;
import org.sonar.server.dashboard.db.WidgetDao;
import org.sonar.server.dashboard.db.WidgetPropertyDao;
import org.sonar.server.dashboard.ws.DashboardsShowAction;
import org.sonar.server.dashboard.ws.DashboardsWebService;
import org.sonar.server.db.DatabaseChecker;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.EmbeddedDatabaseFactory;
import org.sonar.server.db.migrations.DatabaseMigrator;
import org.sonar.server.db.migrations.MigrationSteps;
import org.sonar.server.db.migrations.PlatformDatabaseMigration;
import org.sonar.server.db.migrations.PlatformDatabaseMigrationExecutorServiceImpl;
import org.sonar.server.debt.DebtCharacteristicsXMLImporter;
import org.sonar.server.debt.DebtModelBackup;
import org.sonar.server.debt.DebtModelLookup;
import org.sonar.server.debt.DebtModelOperations;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.debt.DebtModelXMLExporter;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.design.FileDesignWidget;
import org.sonar.server.duplication.ws.DuplicationsJsonWriter;
import org.sonar.server.duplication.ws.DuplicationsParser;
import org.sonar.server.duplication.ws.DuplicationsWs;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.IndexCreator;
import org.sonar.server.es.IndexDefinitions;
import org.sonar.server.event.db.EventDao;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.AddTagsAction;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.CommentAction;
import org.sonar.server.issue.InternalRubyIssueService;
import org.sonar.server.issue.IssueBulkChangeService;
import org.sonar.server.issue.IssueChangelogFormatter;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueQueryService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.PlanAction;
import org.sonar.server.issue.RemoveTagsAction;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.SetSeverityAction;
import org.sonar.server.issue.TransitionAction;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.actionplan.ActionPlanWs;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.filter.IssueFilterWriter;
import org.sonar.server.issue.filter.IssueFilterWs;
import org.sonar.server.issue.filter.RegisterIssueFilters;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.notification.ChangesOnMyIssueNotificationDispatcher;
import org.sonar.server.issue.notification.DoNotFixNotificationDispatcher;
import org.sonar.server.issue.notification.IssueChangesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesNotificationDispatcher;
import org.sonar.server.issue.notification.NewIssuesEmailTemplate;
import org.sonar.server.issue.notification.NewIssuesNotificationDispatcher;
import org.sonar.server.issue.notification.NewIssuesNotificationFactory;
import org.sonar.server.issue.ws.ComponentTagsAction;
import org.sonar.server.issue.ws.IssueActionsWriter;
import org.sonar.server.issue.ws.IssueShowAction;
import org.sonar.server.issue.ws.IssuesWs;
import org.sonar.server.issue.ws.SetTagsAction;
import org.sonar.server.language.ws.LanguageWs;
import org.sonar.server.language.ws.ListAction;
import org.sonar.server.measure.MeasureFilterEngine;
import org.sonar.server.measure.MeasureFilterExecutor;
import org.sonar.server.measure.MeasureFilterFactory;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.measure.persistence.MetricDao;
import org.sonar.server.measure.ws.ManualMeasuresWs;
import org.sonar.server.measure.ws.MetricsWs;
import org.sonar.server.measure.ws.TimeMachineWs;
import org.sonar.server.notifications.NotificationCenter;
import org.sonar.server.notifications.NotificationService;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.InternalPermissionTemplateService;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.ws.PermissionsWs;
import org.sonar.server.platform.monitoring.DatabaseMonitor;
import org.sonar.server.platform.monitoring.EsMonitor;
import org.sonar.server.platform.monitoring.JvmPropertiesMonitor;
import org.sonar.server.platform.monitoring.PluginsMonitor;
import org.sonar.server.platform.monitoring.SonarQubeMonitor;
import org.sonar.server.platform.monitoring.SystemMonitor;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.ServerWs;
import org.sonar.server.platform.ws.SystemInfoWsAction;
import org.sonar.server.platform.ws.SystemRestartWsAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.platform.ws.UpgradesSystemWsAction;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.ServerPluginJarInstaller;
import org.sonar.server.plugins.ServerPluginJarsInstaller;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.plugins.ws.AvailablePluginsWsAction;
import org.sonar.server.plugins.ws.CancelAllPluginsWsAction;
import org.sonar.server.plugins.ws.InstallPluginsWsAction;
import org.sonar.server.plugins.ws.InstalledPluginsWsAction;
import org.sonar.server.plugins.ws.PendingPluginsWsAction;
import org.sonar.server.plugins.ws.PluginUpdateAggregator;
import org.sonar.server.plugins.ws.PluginWSCommons;
import org.sonar.server.plugins.ws.PluginsWs;
import org.sonar.server.plugins.ws.UninstallPluginsWsAction;
import org.sonar.server.plugins.ws.UpdatePluginsWsAction;
import org.sonar.server.plugins.ws.UpdatesPluginsWsAction;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.qualitygate.RegisterQualityGates;
import org.sonar.server.qualitygate.ws.QGatesAppAction;
import org.sonar.server.qualitygate.ws.QGatesCopyAction;
import org.sonar.server.qualitygate.ws.QGatesCreateAction;
import org.sonar.server.qualitygate.ws.QGatesCreateConditionAction;
import org.sonar.server.qualitygate.ws.QGatesDeleteConditionAction;
import org.sonar.server.qualitygate.ws.QGatesDeselectAction;
import org.sonar.server.qualitygate.ws.QGatesDestroyAction;
import org.sonar.server.qualitygate.ws.QGatesListAction;
import org.sonar.server.qualitygate.ws.QGatesRenameAction;
import org.sonar.server.qualitygate.ws.QGatesSearchAction;
import org.sonar.server.qualitygate.ws.QGatesSelectAction;
import org.sonar.server.qualitygate.ws.QGatesSetAsDefaultAction;
import org.sonar.server.qualitygate.ws.QGatesShowAction;
import org.sonar.server.qualitygate.ws.QGatesUnsetDefaultAction;
import org.sonar.server.qualitygate.ws.QGatesUpdateConditionAction;
import org.sonar.server.qualitygate.ws.QGatesWs;
import org.sonar.server.qualityprofile.BuiltInProfiles;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileProjectLookup;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.qualityprofile.QProfileReset;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.QProfiles;
import org.sonar.server.qualityprofile.RegisterQualityProfiles;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.qualityprofile.ws.BulkRuleActivationActions;
import org.sonar.server.qualityprofile.ws.ProfilesWs;
import org.sonar.server.qualityprofile.ws.ProjectAssociationActions;
import org.sonar.server.qualityprofile.ws.QProfileBackupAction;
import org.sonar.server.qualityprofile.ws.QProfileChangeParentAction;
import org.sonar.server.qualityprofile.ws.QProfileChangelogAction;
import org.sonar.server.qualityprofile.ws.QProfileCompareAction;
import org.sonar.server.qualityprofile.ws.QProfileCopyAction;
import org.sonar.server.qualityprofile.ws.QProfileCreateAction;
import org.sonar.server.qualityprofile.ws.QProfileDeleteAction;
import org.sonar.server.qualityprofile.ws.QProfileExportAction;
import org.sonar.server.qualityprofile.ws.QProfileExportersAction;
import org.sonar.server.qualityprofile.ws.QProfileImportersAction;
import org.sonar.server.qualityprofile.ws.QProfileInheritanceAction;
import org.sonar.server.qualityprofile.ws.QProfileProjectsAction;
import org.sonar.server.qualityprofile.ws.QProfileRenameAction;
import org.sonar.server.qualityprofile.ws.QProfileRestoreAction;
import org.sonar.server.qualityprofile.ws.QProfileRestoreBuiltInAction;
import org.sonar.server.qualityprofile.ws.QProfileSearchAction;
import org.sonar.server.qualityprofile.ws.QProfileSetDefaultAction;
import org.sonar.server.qualityprofile.ws.QProfilesWs;
import org.sonar.server.qualityprofile.ws.RuleActivationActions;
import org.sonar.server.ruby.PlatformRackBridge;
import org.sonar.server.ruby.PlatformRubyBridge;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.DeprecatedRulesDefinitionLoader;
import org.sonar.server.rule.RegisterRules;
import org.sonar.server.rule.RubyRuleService;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleDeleter;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.ws.ActiveRuleCompleter;
import org.sonar.server.rule.ws.AppAction;
import org.sonar.server.rule.ws.DeleteAction;
import org.sonar.server.rule.ws.RepositoriesAction;
import org.sonar.server.rule.ws.RuleMapping;
import org.sonar.server.rule.ws.RulesWebService;
import org.sonar.server.rule.ws.SearchAction;
import org.sonar.server.rule.ws.TagsAction;
import org.sonar.server.rule.ws.UpdateAction;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.IndexQueue;
import org.sonar.server.search.IndexSynchronizer;
import org.sonar.server.search.SearchClient;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.db.FileSourceDao;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.source.ws.HashAction;
import org.sonar.server.source.ws.IndexAction;
import org.sonar.server.source.ws.LinesAction;
import org.sonar.server.source.ws.RawAction;
import org.sonar.server.source.ws.ScmAction;
import org.sonar.server.source.ws.ShowAction;
import org.sonar.server.source.ws.SourcesWs;
import org.sonar.server.startup.CopyRequirementsFromCharacteristicsToRules;
import org.sonar.server.startup.GeneratePluginIndex;
import org.sonar.server.startup.JdbcDriverDeployer;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.startup.RegisterDashboards;
import org.sonar.server.startup.RegisterDebtModel;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.startup.RegisterNewMeasureFilters;
import org.sonar.server.startup.RegisterPermissionTemplates;
import org.sonar.server.startup.RegisterServletFilters;
import org.sonar.server.startup.RenameDeprecatedPropertyKeys;
import org.sonar.server.startup.RenameIssueWidgets;
import org.sonar.server.startup.ServerMetadataPersister;
import org.sonar.server.test.CoverageService;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.test.ws.TestsCoveredFilesAction;
import org.sonar.server.test.ws.TestsListAction;
import org.sonar.server.test.ws.TestsWs;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.text.RubyTextService;
import org.sonar.server.ui.JRubyI18n;
import org.sonar.server.ui.PageDecorations;
import org.sonar.server.ui.Views;
import org.sonar.server.ui.ws.ComponentNavigationAction;
import org.sonar.server.ui.ws.GlobalNavigationAction;
import org.sonar.server.ui.ws.NavigationWs;
import org.sonar.server.ui.ws.SettingsNavigationAction;
import org.sonar.server.updatecenter.ws.UpdateCenterWs;
import org.sonar.server.user.DefaultUserService;
import org.sonar.server.user.DoPrivileged;
import org.sonar.server.user.GroupMembershipFinder;
import org.sonar.server.user.GroupMembershipService;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserService;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.ws.FavoritesWs;
import org.sonar.server.user.ws.UserPropertiesWs;
import org.sonar.server.user.ws.UsersWs;
import org.sonar.server.util.BooleanTypeValidation;
import org.sonar.server.util.FloatTypeValidation;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringListTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TextTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.ListingWs;
import org.sonar.server.ws.WebServiceEngine;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

class ServerComponents {

  private final Platform platform;
  private final Properties properties;
  @Nullable
  private final Object[] extraRootComponents;
  private final List<Object> level4AddedComponents = Lists.newArrayList();

  ServerComponents(Platform platform, Properties properties, Object... extraRootComponents) {
    this.platform = platform;
    this.properties = properties;
    this.extraRootComponents = extraRootComponents;
  }

  /**
   * All the stuff required to connect to database
   */
  Collection<Object> level1Components() {
    List<Object> components = Lists.newArrayList(platform, properties);
    addExtraRootComponents(components);
    components.addAll(Arrays.asList(
      ServerSettings.class,
      ServerImpl.class,
      Logback.class,
      EmbeddedDatabaseFactory.class,
      DefaultDatabase.class,
      DatabaseChecker.class,
      MyBatis.class,
      IndexQueue.class,
      DatabaseServerCompatibility.class,
      DatabaseVersion.class,
      PurgeProfiler.class,
      DefaultServerFileSystem.class,
      SemaphoreUpdater.class,
      SemaphoresImpl.class,
      TempFolderCleaner.class,
      new TempFolderProvider(),
      System2.INSTANCE,

      // rack bridges
      PlatformRackBridge.class,

      // DB
      DbClient.class,

      // Elasticsearch
      SearchClient.class,
      IndexClient.class,
      EsClient.class,

      // users
      GroupDao.class,
      UserDao.class,
      UserGroupDao.class,

      // dashboards
      DashboardDao.class,
      DashboardsWebService.class,
      DashboardsShowAction.class,
      WidgetDao.class,
      WidgetPropertyDao.class,

      // rules/qprofiles
      RuleNormalizer.class,
      ActiveRuleNormalizer.class,
      RuleIndex.class,
      ActiveRuleIndex.class,
      RuleDao.class,
      ActiveRuleDao.class,

      // issues
      IssueIndex.class,
      IssueDao.class,

      // measures
      MeasureDao.class,
      MetricDao.class,
      MeasureFilterDao.class,

      // components
      ComponentDao.class,
      ComponentIndexDao.class,
      ComponentLinkDao.class,
      SnapshotDao.class,

      EventDao.class,
      ActivityDao.class,
      AnalysisReportDao.class,
      FileSourceDao.class
      ));
    components.addAll(CorePropertyDefinitions.all());
    components.addAll(MigrationSteps.CLASSES);
    components.addAll(DaoUtils.getDaoClasses());
    return components;
  }

  private void addExtraRootComponents(List<Object> components) {
    if (this.extraRootComponents != null) {
      for (Object extraRootComponent : this.extraRootComponents) {
        if (extraRootComponent != null) {
          components.add(extraRootComponent);
        }
      }
    }
  }

  /**
   * The stuff required to display the db upgrade form in webapp.
   * Needs to be connected to db.
   */
  Collection<Object> level2Components() {
    return Lists.<Object>newArrayList(
      DefaultServerUpgradeStatus.class,
      DatabaseMigrator.class,

      // depends on Ruby
      PlatformRubyBridge.class,

      // plugins
      ServerPluginJarsInstaller.class,
      ServerPluginJarInstaller.class,
      InstalledPluginReferentialFactory.class,
      ServerPluginRepository.class,
      ServerExtensionInstaller.class,

      // depends on plugins
      RailsAppsDeployer.class,
      JRubyI18n.class,
      DefaultI18n.class,
      RuleI18nManager.class,
      Durations.class
      );
  }

  /**
   * The core components that complete the initialization of database
   * when its schema is up-to-date.
   */
  Collection<Object> level3Components() {
    return Lists.newArrayList(
      PersistentSettings.class,
      DefaultDatabaseConnector.class,
      ThreadLocalDatabaseSessionFactory.class,
      new DatabaseSessionProvider(),
      ServerMetadataPersister.class,
      DefaultHttpDownloader.class,
      UriReader.class,
      ServerIdGenerator.class,

      PlatformDatabaseMigrationExecutorServiceImpl.class,
      PlatformDatabaseMigration.class
      );
  }

  void startLevel4Components(ComponentContainer pico) {
    pico.addSingleton(PluginDownloader.class);
    pico.addSingleton(ChartFactory.class);
    pico.addSingleton(Views.class);
    pico.addSingleton(ResourceTypes.class);
    pico.addSingleton(SettingsChangeNotifier.class);
    pico.addSingleton(PageDecorations.class);
    pico.addSingleton(DefaultResourcePermissions.class);
    pico.addSingleton(Periods.class);
    pico.addSingleton(ServerWs.class);
    pico.addSingleton(BackendCleanup.class);
    pico.addSingleton(IndexDefinitions.class);
    pico.addSingleton(IndexCreator.class);

    // Activity
    pico.addSingleton(ActivityService.class);
    pico.addSingleton(ActivityIndexDefinition.class);
    pico.addSingleton(ActivityIndexer.class);
    pico.addSingleton(ActivityIndex.class);

    // batch
    pico.addSingleton(BatchIndex.class);
    pico.addSingleton(GlobalRepositoryAction.class);
    pico.addSingleton(ProjectRepositoryAction.class);
    pico.addSingleton(ProjectRepositoryLoader.class);
    pico.addSingleton(SubmitReportWsAction.class);
    pico.addSingleton(IssuesAction.class);
    pico.addSingleton(BatchWs.class);

    // update center
    pico.addSingleton(UpdateCenterClient.class);
    pico.addSingleton(UpdateCenterMatrixFactory.class);
    pico.addSingleton(UpdateCenterWs.class);

    // quality profile
    pico.addSingleton(XMLProfileParser.class);
    pico.addSingleton(XMLProfileSerializer.class);
    pico.addSingleton(AnnotationProfileParser.class);
    pico.addSingleton(QProfiles.class);
    pico.addSingleton(QProfileLookup.class);
    pico.addSingleton(QProfileProjectOperations.class);
    pico.addSingleton(QProfileProjectLookup.class);
    pico.addSingleton(QProfileComparison.class);
    pico.addSingleton(BuiltInProfiles.class);
    pico.addSingleton(QProfileRestoreBuiltInAction.class);
    pico.addSingleton(QProfileSearchAction.class);
    pico.addSingleton(QProfileSetDefaultAction.class);
    pico.addSingleton(QProfileProjectsAction.class);
    pico.addSingleton(QProfileDeleteAction.class);
    pico.addSingleton(QProfileRenameAction.class);
    pico.addSingleton(QProfileCopyAction.class);
    pico.addSingleton(QProfileBackupAction.class);
    pico.addSingleton(QProfileRestoreAction.class);
    pico.addSingleton(QProfileCreateAction.class);
    pico.addSingleton(QProfileImportersAction.class);
    pico.addSingleton(QProfileInheritanceAction.class);
    pico.addSingleton(QProfileChangeParentAction.class);
    pico.addSingleton(QProfileChangelogAction.class);
    pico.addSingleton(QProfileCompareAction.class);
    pico.addSingleton(QProfileExportAction.class);
    pico.addSingleton(QProfileExportersAction.class);
    pico.addSingleton(QProfilesWs.class);
    pico.addSingleton(ProfilesWs.class);
    pico.addSingleton(RuleActivationActions.class);
    pico.addSingleton(BulkRuleActivationActions.class);
    pico.addSingleton(ProjectAssociationActions.class);
    pico.addSingleton(RuleActivator.class);
    pico.addSingleton(QProfileLoader.class);
    pico.addSingleton(QProfileExporters.class);
    pico.addSingleton(QProfileService.class);
    pico.addSingleton(RuleActivatorContextFactory.class);
    pico.addSingleton(QProfileFactory.class);
    pico.addSingleton(QProfileCopier.class);
    pico.addSingleton(QProfileBackuper.class);
    pico.addSingleton(QProfileReset.class);
    pico.addSingleton(RubyQProfileActivityService.class);

    // rule
    pico.addSingleton(AnnotationRuleParser.class);
    pico.addSingleton(XMLRuleParser.class);
    pico.addSingleton(DefaultRuleFinder.class);
    pico.addSingleton(RuleOperations.class);
    pico.addSingleton(RubyRuleService.class);
    pico.addSingleton(RuleRepositories.class);
    pico.addSingleton(DeprecatedRulesDefinitionLoader.class);
    pico.addSingleton(RuleDefinitionsLoader.class);
    pico.addSingleton(RulesDefinitionXmlLoader.class);
    pico.addSingleton(RuleService.class);
    pico.addSingleton(RuleUpdater.class);
    pico.addSingleton(RuleCreator.class);
    pico.addSingleton(RuleDeleter.class);
    pico.addSingleton(UpdateAction.class);
    pico.addSingleton(RulesWebService.class);
    pico.addSingleton(SearchAction.class);
    pico.addSingleton(org.sonar.server.rule.ws.ShowAction.class);
    pico.addSingleton(org.sonar.server.rule.ws.CreateAction.class);
    pico.addSingleton(DeleteAction.class);
    pico.addSingleton(TagsAction.class);
    pico.addSingleton(RuleMapping.class);
    pico.addSingleton(ActiveRuleCompleter.class);
    pico.addSingleton(RepositoriesAction.class);
    pico.addSingleton(AppAction.class);

    // languages
    pico.addSingleton(Languages.class);
    pico.addSingleton(LanguageWs.class);
    pico.addSingleton(ListAction.class);

    // activity
    pico.addSingleton(ActivitiesWebService.class);
    pico.addSingleton(org.sonar.server.activity.ws.SearchAction.class);
    pico.addSingleton(ActivityMapping.class);

    // measure
    pico.addComponent(MeasuresDao.class, false);
    pico.addSingleton(MeasureFilterFactory.class);
    pico.addSingleton(MeasureFilterExecutor.class);
    pico.addSingleton(MeasureFilterEngine.class);
    pico.addSingleton(DefaultMetricFinder.class);
    pico.addSingleton(ServerLifecycleNotifier.class);
    pico.addSingleton(TimeMachineWs.class);
    pico.addSingleton(ManualMeasuresWs.class);
    pico.addSingleton(MetricsWs.class);

    // quality gates
    pico.addSingleton(QualityGateDao.class);
    pico.addSingleton(QualityGateConditionDao.class);
    pico.addSingleton(QualityGates.class);
    pico.addSingleton(ProjectQgateAssociationDao.class);
    pico.addSingleton(QgateProjectFinder.class);

    pico.addSingleton(QGatesListAction.class);
    pico.addSingleton(QGatesSearchAction.class);
    pico.addSingleton(QGatesShowAction.class);
    pico.addSingleton(QGatesCreateAction.class);
    pico.addSingleton(QGatesRenameAction.class);
    pico.addSingleton(QGatesCopyAction.class);
    pico.addSingleton(QGatesDestroyAction.class);
    pico.addSingleton(QGatesSetAsDefaultAction.class);
    pico.addSingleton(QGatesUnsetDefaultAction.class);
    pico.addSingleton(QGatesSelectAction.class);
    pico.addSingleton(QGatesDeselectAction.class);
    pico.addSingleton(QGatesCreateConditionAction.class);
    pico.addSingleton(QGatesDeleteConditionAction.class);
    pico.addSingleton(QGatesUpdateConditionAction.class);
    pico.addSingleton(QGatesAppAction.class);
    pico.addSingleton(QGatesWs.class);

    // web services
    pico.addSingleton(WebServiceEngine.class);
    pico.addSingleton(ListingWs.class);

    // localization
    pico.addSingleton(L10nWs.class);

    // authentication
    pico.addSingleton(AuthenticationWs.class);

    // users
    pico.addSingleton(SecurityRealmFactory.class);
    pico.addSingleton(HibernateUserFinder.class);
    pico.addSingleton(NewUserNotifier.class);
    pico.addSingleton(DefaultUserFinder.class);
    pico.addSingleton(DefaultUserService.class);
    pico.addSingleton(UsersWs.class);
    pico.addSingleton(org.sonar.server.user.ws.CreateAction.class);
    pico.addSingleton(org.sonar.server.user.ws.UpdateAction.class);
    pico.addSingleton(org.sonar.server.user.ws.CurrentUserAction.class);
    pico.addSingleton(org.sonar.server.issue.ws.AuthorsAction.class);
    pico.addSingleton(FavoritesWs.class);
    pico.addSingleton(UserPropertiesWs.class);
    pico.addSingleton(UserIndexDefinition.class);
    pico.addSingleton(UserIndexer.class);
    pico.addSingleton(UserIndex.class);
    pico.addSingleton(UserService.class);
    pico.addSingleton(UserUpdater.class);

    // groups
    pico.addSingleton(GroupMembershipService.class);
    pico.addSingleton(GroupMembershipFinder.class);

    // permissions
    pico.addSingleton(PermissionFacade.class);
    pico.addSingleton(InternalPermissionService.class);
    pico.addSingleton(InternalPermissionTemplateService.class);
    pico.addSingleton(PermissionFinder.class);
    pico.addSingleton(PermissionsWs.class);

    // components
    pico.addSingleton(DefaultComponentFinder.class);
    pico.addSingleton(DefaultRubyComponentService.class);
    pico.addSingleton(ComponentService.class);
    pico.addSingleton(ResourcesWs.class);
    pico.addSingleton(ComponentsWs.class);
    pico.addSingleton(ProjectsWs.class);
    pico.addSingleton(ComponentAppAction.class);
    pico.addSingleton(org.sonar.server.component.ws.SearchAction.class);
    pico.addSingleton(EventsWs.class);
    pico.addSingleton(ComponentCleanerService.class);

    // views
    pico.addSingleton(ViewIndexDefinition.class);
    pico.addSingleton(ViewIndexer.class);
    pico.addSingleton(ViewIndex.class);

    // issues
    pico.addSingleton(IssueIndexDefinition.class);
    pico.addSingleton(IssueIndexer.class);
    pico.addSingleton(IssueAuthorizationIndexer.class);
    pico.addSingleton(ServerIssueStorage.class);
    pico.addSingleton(IssueUpdater.class);
    pico.addSingleton(FunctionExecutor.class);
    pico.addSingleton(IssueWorkflow.class);
    pico.addSingleton(IssueCommentService.class);
    pico.addSingleton(InternalRubyIssueService.class);
    pico.addSingleton(IssueChangelogService.class);
    pico.addSingleton(ActionService.class);
    pico.addSingleton(Actions.class);
    pico.addSingleton(IssueBulkChangeService.class);
    pico.addSingleton(IssueChangelogFormatter.class);
    pico.addSingleton(IssuesWs.class);
    pico.addSingleton(IssueShowAction.class);
    pico.addSingleton(org.sonar.server.issue.ws.SearchAction.class);
    pico.addSingleton(org.sonar.server.issue.ws.TagsAction.class);
    pico.addSingleton(SetTagsAction.class);
    pico.addSingleton(ComponentTagsAction.class);
    pico.addSingleton(IssueService.class);
    pico.addSingleton(IssueActionsWriter.class);
    pico.addSingleton(IssueQueryService.class);
    pico.addSingleton(NewIssuesEmailTemplate.class);
    pico.addSingleton(MyNewIssuesEmailTemplate.class);
    pico.addSingleton(IssueChangesEmailTemplate.class);
    pico.addSingleton(ChangesOnMyIssueNotificationDispatcher.class);
    pico.addSingleton(ChangesOnMyIssueNotificationDispatcher.newMetadata());
    pico.addSingleton(NewIssuesNotificationDispatcher.class);
    pico.addSingleton(NewIssuesNotificationDispatcher.newMetadata());
    pico.addSingleton(MyNewIssuesNotificationDispatcher.class);
    pico.addSingleton(MyNewIssuesNotificationDispatcher.newMetadata());
    pico.addSingleton(DoNotFixNotificationDispatcher.class);
    pico.addSingleton(DoNotFixNotificationDispatcher.newMetadata());
    pico.addSingleton(NewIssuesNotificationFactory.class);

    // issue filters
    pico.addSingleton(IssueFilterService.class);
    pico.addSingleton(IssueFilterSerializer.class);
    pico.addSingleton(IssueFilterWs.class);
    pico.addSingleton(IssueFilterWriter.class);
    pico.addSingleton(org.sonar.server.issue.filter.AppAction.class);
    pico.addSingleton(org.sonar.server.issue.filter.ShowAction.class);
    pico.addSingleton(org.sonar.server.issue.filter.FavoritesAction.class);

    // action plan
    pico.addSingleton(ActionPlanWs.class);
    pico.addSingleton(ActionPlanService.class);

    // issues actions
    pico.addSingleton(AssignAction.class);
    pico.addSingleton(PlanAction.class);
    pico.addSingleton(SetSeverityAction.class);
    pico.addSingleton(CommentAction.class);
    pico.addSingleton(TransitionAction.class);
    pico.addSingleton(AddTagsAction.class);
    pico.addSingleton(RemoveTagsAction.class);

    // technical debt
    pico.addSingleton(DebtModelService.class);
    pico.addSingleton(DebtModelOperations.class);
    pico.addSingleton(DebtModelLookup.class);
    pico.addSingleton(DebtModelBackup.class);
    pico.addSingleton(DebtModelPluginRepository.class);
    pico.addSingleton(DebtModelXMLExporter.class);
    pico.addSingleton(DebtRulesXMLImporter.class);
    pico.addSingleton(DebtCharacteristicsXMLImporter.class);

    // source
    pico.addSingleton(HtmlSourceDecorator.class);
    pico.addSingleton(SourceService.class);
    pico.addSingleton(SourcesWs.class);
    pico.addSingleton(ShowAction.class);
    pico.addSingleton(LinesAction.class);
    pico.addSingleton(HashAction.class);
    pico.addSingleton(RawAction.class);
    pico.addSingleton(IndexAction.class);
    pico.addSingleton(ScmAction.class);
    pico.addSingleton(SourceLineIndexDefinition.class);
    pico.addSingleton(SourceLineIndex.class);
    pico.addSingleton(SourceLineIndexer.class);

    // Duplications
    pico.addSingleton(DuplicationsParser.class);
    pico.addSingleton(DuplicationsWs.class);
    pico.addSingleton(DuplicationsJsonWriter.class);
    pico.addSingleton(org.sonar.server.duplication.ws.ShowAction.class);

    // text
    pico.addSingleton(MacroInterpreter.class);
    pico.addSingleton(RubyTextService.class);

    // Notifications
    pico.addSingleton(EmailSettings.class);
    pico.addSingleton(NotificationService.class);
    pico.addSingleton(NotificationCenter.class);
    pico.addSingleton(DefaultNotificationManager.class);

    // Tests
    pico.addSingleton(CoverageService.class);
    pico.addSingleton(TestsWs.class);
    pico.addSingleton(TestsCoveredFilesAction.class);
    pico.addSingleton(TestsListAction.class);
    pico.addSingleton(TestIndexDefinition.class);
    pico.addSingleton(TestIndex.class);
    pico.addSingleton(TestIndexer.class);

    // Properties
    pico.addSingleton(PropertiesWs.class);

    // graphs and perspective related classes
    pico.addSingleton(TestablePerspectiveLoader.class);
    pico.addSingleton(TestPlanPerspectiveLoader.class);
    pico.addSingleton(SnapshotPerspectives.class);

    // Type validation
    pico.addSingleton(TypeValidations.class);
    pico.addSingleton(IntegerTypeValidation.class);
    pico.addSingleton(FloatTypeValidation.class);
    pico.addSingleton(BooleanTypeValidation.class);
    pico.addSingleton(TextTypeValidation.class);
    pico.addSingleton(StringTypeValidation.class);
    pico.addSingleton(StringListTypeValidation.class);

    // Design
    pico.addSingleton(FileDesignWidget.class);

    // System
    pico.addSingletons(Arrays.asList(
      SystemRestartWsAction.class,
      SystemInfoWsAction.class,
      UpgradesSystemWsAction.class,
      SystemWs.class,
      SystemMonitor.class,
      SonarQubeMonitor.class,
      EsMonitor.class,
      PluginsMonitor.class,
      JvmPropertiesMonitor.class,
      DatabaseMonitor.class
      ));

    // Plugins WS
    pico.addSingleton(PluginWSCommons.class);
    pico.addSingleton(PluginUpdateAggregator.class);
    pico.addSingleton(InstalledPluginsWsAction.class);
    pico.addSingleton(AvailablePluginsWsAction.class);
    pico.addSingleton(UpdatesPluginsWsAction.class);
    pico.addSingleton(PendingPluginsWsAction.class);
    pico.addSingleton(InstallPluginsWsAction.class);
    pico.addSingleton(UpdatePluginsWsAction.class);
    pico.addSingleton(UninstallPluginsWsAction.class);
    pico.addSingleton(CancelAllPluginsWsAction.class);
    pico.addSingleton(PluginsWs.class);

    // Compute engine
    pico.addSingleton(ReportQueue.class);
    pico.addSingleton(ComputationThreadLauncher.class);
    pico.addSingleton(ComputationWebService.class);
    pico.addSingleton(IsQueueEmptyWebService.class);
    pico.addSingleton(QueueWsAction.class);
    pico.addSingleton(HistoryWsAction.class);
    pico.addSingleton(DefaultPeriodCleaner.class);
    pico.addSingleton(DefaultPurgeTask.class);
    pico.addSingleton(ProjectCleaner.class);
    pico.addSingleton(ProjectSettingsFactory.class);
    pico.addSingleton(IndexPurgeListener.class);

    // UI
    pico.addSingleton(GlobalNavigationAction.class);
    pico.addSingleton(SettingsNavigationAction.class);
    pico.addSingleton(ComponentNavigationAction.class);
    pico.addSingleton(NavigationWs.class);

    for (Object components : level4AddedComponents) {
      pico.addSingleton(components);
    }

    ServerExtensionInstaller extensionInstaller = pico.getComponentByType(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(pico);

    pico.startComponents();
  }

  void addComponents(Collection components) {
    this.level4AddedComponents.addAll(components);
  }

  public void executeStartupTasks(ComponentContainer pico) {
    final ComponentContainer startupContainer = pico.createChild();
    startupContainer.addSingleton(IndexSynchronizer.class);
    startupContainer.addSingleton(RegisterMetrics.class);
    startupContainer.addSingleton(RegisterQualityGates.class);
    startupContainer.addSingleton(RegisterRules.class);
    startupContainer.addSingleton(RegisterQualityProfiles.class);
    startupContainer.addSingleton(JdbcDriverDeployer.class);
    startupContainer.addSingleton(RegisterDebtModel.class);
    startupContainer.addSingleton(GeneratePluginIndex.class);
    startupContainer.addSingleton(RegisterNewMeasureFilters.class);
    startupContainer.addSingleton(RegisterDashboards.class);
    startupContainer.addSingleton(RegisterPermissionTemplates.class);
    startupContainer.addSingleton(RenameDeprecatedPropertyKeys.class);
    startupContainer.addSingleton(LogServerId.class);
    startupContainer.addSingleton(RegisterServletFilters.class);
    startupContainer.addSingleton(CopyRequirementsFromCharacteristicsToRules.class);
    startupContainer.addSingleton(ReportQueueCleaner.class);
    startupContainer.addSingleton(RegisterIssueFilters.class);
    startupContainer.addSingleton(RenameIssueWidgets.class);

    DoPrivileged.execute(new DoPrivileged.Task() {
      @Override
      protected void doPrivileged() {
        startupContainer.getComponentByType(IndexSynchronizer.class).executeDeprecated();
        startupContainer.startComponents();
        startupContainer.getComponentByType(IndexSynchronizer.class).execute();
        startupContainer.getComponentByType(ServerLifecycleNotifier.class).notifyStart();
      }
    });

    // Do not put the following statements in a finally block.
    // It would hide the possible exception raised during startup
    // See SONAR-3107
    startupContainer.stopComponents();

    pico.getComponentByType(DatabaseSessionFactory.class).clear();
    pico.removeChild();
  }
}
