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
package org.sonar.server.platform.platformlevel;

import java.util.List;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.core.computation.dbcleaner.IndexPurgeListener;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.issue.IssueFilterSerializer;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.metric.DefaultMetricFinder;
import org.sonar.server.notification.DefaultNotificationManager;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.resource.DefaultResourceTypes;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.RubyQProfileActivityService;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityIndexDefinition;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.activity.ws.ActivitiesWs;
import org.sonar.server.activity.ws.ActivityMapping;
import org.sonar.server.authentication.ws.AuthenticationWs;
import org.sonar.server.batch.BatchWsModule;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.charts.DistributionAreaChart;
import org.sonar.server.charts.DistributionBarChart;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.DefaultComponentFinder;
import org.sonar.server.component.DefaultRubyComponentService;
import org.sonar.server.component.ws.ComponentsWs;
import org.sonar.server.component.ws.EventsWs;
import org.sonar.server.component.ws.ResourcesWs;
import org.sonar.server.computation.ComputationThreadLauncher;
import org.sonar.server.computation.ReportQueue;
import org.sonar.server.computation.ws.ComputationWs;
import org.sonar.server.computation.ws.HistoryAction;
import org.sonar.server.computation.ws.IsQueueEmptyWs;
import org.sonar.server.computation.ws.QueueAction;
import org.sonar.server.config.ws.PropertiesWs;
import org.sonar.server.custommeasure.ws.CustomMeasuresWsModule;
import org.sonar.server.dashboard.template.GlobalDefaultDashboard;
import org.sonar.server.dashboard.template.ProjectDefaultDashboard;
import org.sonar.server.dashboard.template.ProjectIssuesDashboard;
import org.sonar.server.dashboard.template.ProjectTimeMachineDashboard;
import org.sonar.server.dashboard.widget.ActionPlansWidget;
import org.sonar.server.dashboard.widget.AlertsWidget;
import org.sonar.server.dashboard.widget.BubbleChartWidget;
import org.sonar.server.dashboard.widget.ComplexityWidget;
import org.sonar.server.dashboard.widget.CoverageWidget;
import org.sonar.server.dashboard.widget.CustomMeasuresWidget;
import org.sonar.server.dashboard.widget.DebtOverviewWidget;
import org.sonar.server.dashboard.widget.DescriptionWidget;
import org.sonar.server.dashboard.widget.DocumentationCommentsWidget;
import org.sonar.server.dashboard.widget.DuplicationsWidget;
import org.sonar.server.dashboard.widget.EventsWidget;
import org.sonar.server.dashboard.widget.HotspotMetricWidget;
import org.sonar.server.dashboard.widget.IssueFilterWidget;
import org.sonar.server.dashboard.widget.IssueTagCloudWidget;
import org.sonar.server.dashboard.widget.IssuesWidget;
import org.sonar.server.dashboard.widget.ItCoverageWidget;
import org.sonar.server.dashboard.widget.MeasureFilterAsBubbleChartWidget;
import org.sonar.server.dashboard.widget.MeasureFilterAsCloudWidget;
import org.sonar.server.dashboard.widget.MeasureFilterAsHistogramWidget;
import org.sonar.server.dashboard.widget.MeasureFilterAsPieChartWidget;
import org.sonar.server.dashboard.widget.MeasureFilterAsTreemapWidget;
import org.sonar.server.dashboard.widget.MeasureFilterListWidget;
import org.sonar.server.dashboard.widget.ProjectFileCloudWidget;
import org.sonar.server.dashboard.widget.ProjectIssueFilterWidget;
import org.sonar.server.dashboard.widget.SizeWidget;
import org.sonar.server.dashboard.widget.TechnicalDebtPyramidWidget;
import org.sonar.server.dashboard.widget.TimeMachineWidget;
import org.sonar.server.dashboard.widget.TimelineWidget;
import org.sonar.server.dashboard.widget.TreemapWidget;
import org.sonar.server.dashboard.widget.WelcomeWidget;
import org.sonar.server.dashboard.ws.DashboardsWs;
import org.sonar.server.debt.DebtCharacteristicsXMLImporter;
import org.sonar.server.debt.DebtModelBackup;
import org.sonar.server.debt.DebtModelLookup;
import org.sonar.server.debt.DebtModelOperations;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.debt.DebtModelXMLExporter;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.duplication.ws.DuplicationsJsonWriter;
import org.sonar.server.duplication.ws.DuplicationsParser;
import org.sonar.server.duplication.ws.DuplicationsWs;
import org.sonar.server.es.IndexCreator;
import org.sonar.server.es.IndexDefinitions;
import org.sonar.server.event.NewAlerts;
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
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.filter.IssueFilterWriter;
import org.sonar.server.issue.filter.IssueFilterWs;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
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
import org.sonar.server.issue.ws.IssuesWs;
import org.sonar.server.issue.ws.SetTagsAction;
import org.sonar.server.language.ws.LanguageWs;
import org.sonar.server.measure.MeasureFilterEngine;
import org.sonar.server.measure.MeasureFilterExecutor;
import org.sonar.server.measure.MeasureFilterFactory;
import org.sonar.server.measure.template.MyFavouritesFilter;
import org.sonar.server.measure.template.ProjectFilter;
import org.sonar.server.measure.ws.ManualMeasuresWs;
import org.sonar.server.measure.ws.TimeMachineWs;
import org.sonar.server.metric.CoreCustomMetrics;
import org.sonar.server.metric.ws.MetricsWsModule;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.notification.email.AlertsEmailTemplate;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.InternalPermissionTemplateService;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.ws.PermissionsWs;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.platform.SettingsChangeNotifier;
import org.sonar.server.platform.monitoring.DatabaseMonitor;
import org.sonar.server.platform.monitoring.EsMonitor;
import org.sonar.server.platform.monitoring.JvmPropertiesMonitor;
import org.sonar.server.platform.monitoring.PluginsMonitor;
import org.sonar.server.platform.monitoring.SonarQubeMonitor;
import org.sonar.server.platform.monitoring.SystemMonitor;
import org.sonar.server.platform.ws.InfoAction;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.MigrateDbSystemAction;
import org.sonar.server.platform.ws.RestartAction;
import org.sonar.server.platform.ws.ServerWs;
import org.sonar.server.platform.ws.StatusAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.platform.ws.UpgradesAction;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.plugins.ws.AvailableAction;
import org.sonar.server.plugins.ws.CancelAllAction;
import org.sonar.server.plugins.ws.InstallAction;
import org.sonar.server.plugins.ws.InstalledAction;
import org.sonar.server.plugins.ws.PendingAction;
import org.sonar.server.plugins.ws.PluginUpdateAggregator;
import org.sonar.server.plugins.ws.PluginWSCommons;
import org.sonar.server.plugins.ws.PluginsWs;
import org.sonar.server.plugins.ws.UninstallAction;
import org.sonar.server.plugins.ws.UpdatesAction;
import org.sonar.server.project.ws.ProjectsWsModule;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.qualitygate.ws.CreateConditionAction;
import org.sonar.server.qualitygate.ws.DeleteConditionAction;
import org.sonar.server.qualitygate.ws.DeselectAction;
import org.sonar.server.qualitygate.ws.DestroyAction;
import org.sonar.server.qualitygate.ws.QGatesWs;
import org.sonar.server.qualitygate.ws.SelectAction;
import org.sonar.server.qualitygate.ws.SetAsDefaultAction;
import org.sonar.server.qualitygate.ws.UnsetDefaultAction;
import org.sonar.server.qualitygate.ws.UpdateConditionAction;
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
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.RuleActivatorContextFactory;
import org.sonar.server.qualityprofile.ws.BackupAction;
import org.sonar.server.qualityprofile.ws.BulkRuleActivationActions;
import org.sonar.server.qualityprofile.ws.ChangeParentAction;
import org.sonar.server.qualityprofile.ws.ChangelogAction;
import org.sonar.server.qualityprofile.ws.CompareAction;
import org.sonar.server.qualityprofile.ws.CopyAction;
import org.sonar.server.qualityprofile.ws.CreateAction;
import org.sonar.server.qualityprofile.ws.ExportAction;
import org.sonar.server.qualityprofile.ws.ExportersAction;
import org.sonar.server.qualityprofile.ws.ImportersAction;
import org.sonar.server.qualityprofile.ws.InheritanceAction;
import org.sonar.server.qualityprofile.ws.ProfilesWs;
import org.sonar.server.qualityprofile.ws.ProjectAssociationActions;
import org.sonar.server.qualityprofile.ws.ProjectsAction;
import org.sonar.server.qualityprofile.ws.QProfilesWs;
import org.sonar.server.qualityprofile.ws.RenameAction;
import org.sonar.server.qualityprofile.ws.RestoreAction;
import org.sonar.server.qualityprofile.ws.RestoreBuiltInAction;
import org.sonar.server.qualityprofile.ws.RuleActivationActions;
import org.sonar.server.qualityprofile.ws.SetDefaultAction;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.DeprecatedRulesDefinitionLoader;
import org.sonar.server.rule.RubyRuleService;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleDeleter;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.ws.ActiveRuleCompleter;
import org.sonar.server.rule.ws.RepositoriesAction;
import org.sonar.server.rule.ws.RuleMapping;
import org.sonar.server.rule.ws.RulesWs;
import org.sonar.server.rule.ws.TagsAction;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;
import org.sonar.server.source.index.SourceLineIndexer;
import org.sonar.server.source.ws.HashAction;
import org.sonar.server.source.ws.IndexAction;
import org.sonar.server.source.ws.LinesAction;
import org.sonar.server.source.ws.RawAction;
import org.sonar.server.source.ws.ScmAction;
import org.sonar.server.source.ws.SourcesWs;
import org.sonar.server.test.CoverageService;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.test.ws.CoveredFilesAction;
import org.sonar.server.test.ws.TestsWs;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.text.RubyTextService;
import org.sonar.server.ui.PageDecorations;
import org.sonar.server.ui.Views;
import org.sonar.server.ui.ws.ComponentNavigationAction;
import org.sonar.server.ui.ws.GlobalNavigationAction;
import org.sonar.server.ui.ws.NavigationWs;
import org.sonar.server.ui.ws.SettingsNavigationAction;
import org.sonar.server.updatecenter.ws.UpdateCenterWs;
import org.sonar.server.user.DefaultUserService;
import org.sonar.server.user.GroupMembershipFinder;
import org.sonar.server.user.GroupMembershipService;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.ws.CurrentAction;
import org.sonar.server.user.ws.FavoritesWs;
import org.sonar.server.user.ws.UserPropertiesWs;
import org.sonar.server.user.ws.UsersWs;
import org.sonar.server.usergroups.ws.UserGroupsModule;
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

public class PlatformLevel4 extends PlatformLevel {

  private final List<Object> level4AddedComponents;

  public PlatformLevel4(PlatformLevel parent, List<Object> level4AddedComponents) {
    super("level4", parent);
    this.level4AddedComponents = level4AddedComponents;
  }

  @Override
  protected void configureLevel() {
    add(
      PluginDownloader.class,
      ChartFactory.class,
      DistributionBarChart.class,
      DistributionAreaChart.class,
      Views.class,
      ResourceTypes.class,
      DefaultResourceTypes.get(),
      SettingsChangeNotifier.class,
      PageDecorations.class,
      DefaultResourcePermissions.class,
      Periods.class,
      ServerWs.class,
      BackendCleanup.class,
      IndexDefinitions.class,
      IndexCreator.class,

      // Activity
      ActivityService.class,
      ActivityIndexDefinition.class,
      ActivityIndexer.class,
      ActivityIndex.class,

      // batch
      BatchWsModule.class,

      // Dashboard
      DashboardsWs.class,
      org.sonar.server.dashboard.ws.ShowAction.class,
      ProjectDefaultDashboard.class,
      ProjectIssuesDashboard.class,
      ProjectTimeMachineDashboard.class,
      GlobalDefaultDashboard.class,
      AlertsWidget.class,
      CoverageWidget.class,
      ItCoverageWidget.class,
      DescriptionWidget.class,
      ComplexityWidget.class,
      IssuesWidget.class,
      SizeWidget.class,
      EventsWidget.class,
      CustomMeasuresWidget.class,
      TimelineWidget.class,
      BubbleChartWidget.class,
      TimeMachineWidget.class,
      HotspotMetricWidget.class,
      TreemapWidget.class,
      MeasureFilterListWidget.class,
      MeasureFilterAsTreemapWidget.class,
      WelcomeWidget.class,
      DocumentationCommentsWidget.class,
      DuplicationsWidget.class,
      TechnicalDebtPyramidWidget.class,
      MeasureFilterAsPieChartWidget.class,
      MeasureFilterAsCloudWidget.class,
      MeasureFilterAsHistogramWidget.class,
      MeasureFilterAsBubbleChartWidget.class,
      ProjectFileCloudWidget.class,
      DebtOverviewWidget.class,
      ActionPlansWidget.class,
      IssueFilterWidget.class,
      ProjectIssueFilterWidget.class,
      IssueTagCloudWidget.class,

      // update center
      UpdateCenterClient.class,
      UpdateCenterMatrixFactory.class,
      UpdateCenterWs.class,

      // quality profile
      XMLProfileParser.class,
      XMLProfileSerializer.class,
      AnnotationProfileParser.class,
      QProfiles.class,
      QProfileLookup.class,
      QProfileProjectOperations.class,
      QProfileProjectLookup.class,
      QProfileComparison.class,
      BuiltInProfiles.class,
      RestoreBuiltInAction.class,
      org.sonar.server.qualityprofile.ws.SearchAction.class,
      SetDefaultAction.class,
      ProjectsAction.class,
      org.sonar.server.qualityprofile.ws.DeleteAction.class,
      RenameAction.class,
      CopyAction.class,
      BackupAction.class,
      RestoreAction.class,
      CreateAction.class,
      ImportersAction.class,
      InheritanceAction.class,
      ChangeParentAction.class,
      ChangelogAction.class,
      CompareAction.class,
      ExportAction.class,
      ExportersAction.class,
      QProfilesWs.class,
      ProfilesWs.class,
      RuleActivationActions.class,
      BulkRuleActivationActions.class,
      ProjectAssociationActions.class,
      RuleActivator.class,
      QProfileLoader.class,
      QProfileExporters.class,
      QProfileService.class,
      RuleActivatorContextFactory.class,
      QProfileFactory.class,
      QProfileCopier.class,
      QProfileBackuper.class,
      QProfileReset.class,
      RubyQProfileActivityService.class,

      // rule
      AnnotationRuleParser.class,
      XMLRuleParser.class,
      DefaultRuleFinder.class,
      RuleOperations.class,
      RubyRuleService.class,
      RuleRepositories.class,
      DeprecatedRulesDefinitionLoader.class,
      RuleDefinitionsLoader.class,
      RulesDefinitionXmlLoader.class,
      RuleService.class,
      RuleUpdater.class,
      RuleCreator.class,
      RuleDeleter.class,
      org.sonar.server.rule.ws.UpdateAction.class,
      RulesWs.class,
      org.sonar.server.rule.ws.SearchAction.class,
      org.sonar.server.rule.ws.ShowAction.class,
      org.sonar.server.rule.ws.CreateAction.class,
      org.sonar.server.rule.ws.DeleteAction.class,
      TagsAction.class,
      RuleMapping.class,
      ActiveRuleCompleter.class,
      RepositoriesAction.class,
      org.sonar.server.rule.ws.AppAction.class,

      // languages
      Languages.class,
      LanguageWs.class,
      org.sonar.server.language.ws.ListAction.class,

      // activity
      ActivitiesWs.class,
      org.sonar.server.activity.ws.SearchAction.class,
      ActivityMapping.class,

      // measure
      MeasuresDao.class,

      MeasureFilterFactory.class,
      MeasureFilterExecutor.class,
      MeasureFilterEngine.class,
      ManualMeasuresWs.class,
      MetricsWsModule.class,
      CustomMeasuresWsModule.class,
      ProjectFilter.class,
      MyFavouritesFilter.class,
      CoreCustomMetrics.class,
      DefaultMetricFinder.class,
      TimeMachineWs.class,

      // quality gates
      QualityGateDao.class,
      QualityGateConditionDao.class,
      QualityGates.class,
      ProjectQgateAssociationDao.class,
      QgateProjectFinder.class,

      org.sonar.server.qualitygate.ws.ListAction.class,
      org.sonar.server.qualitygate.ws.SearchAction.class,
      org.sonar.server.qualitygate.ws.ShowAction.class,
      org.sonar.server.qualitygate.ws.CreateAction.class,
      org.sonar.server.qualitygate.ws.RenameAction.class,
      org.sonar.server.qualitygate.ws.CopyAction.class,
      DestroyAction.class,
      SetAsDefaultAction.class,
      UnsetDefaultAction.class,
      SelectAction.class,
      DeselectAction.class,
      CreateConditionAction.class,
      DeleteConditionAction.class,
      UpdateConditionAction.class,
      org.sonar.server.qualitygate.ws.AppAction.class,
      QGatesWs.class,

      // web services
      WebServiceEngine.class,
      ListingWs.class,

      // localization
      L10nWs.class,

      // authentication
      AuthenticationWs.class,

      // users
      SecurityRealmFactory.class,
      HibernateUserFinder.class,
      NewUserNotifier.class,
      DefaultUserFinder.class,
      DefaultUserService.class,
      UsersWs.class,
      org.sonar.server.user.ws.CreateAction.class,
      org.sonar.server.user.ws.UpdateAction.class,
      org.sonar.server.user.ws.DeactivateAction.class,
      org.sonar.server.user.ws.ChangePasswordAction.class,
      CurrentAction.class,
      org.sonar.server.user.ws.SearchAction.class,
      org.sonar.server.user.ws.GroupsAction.class,
      org.sonar.server.issue.ws.AuthorsAction.class,
      FavoritesWs.class,
      UserPropertiesWs.class,
      UserIndexDefinition.class,
      UserIndexer.class,
      UserIndex.class,
      UserUpdater.class,

      // groups
      GroupMembershipService.class,
      GroupMembershipFinder.class,
      UserGroupsModule.class,

      // permissions
      PermissionFacade.class,
      InternalPermissionService.class,
      InternalPermissionTemplateService.class,
      PermissionFinder.class,
      PermissionsWs.class,

      // components
      ProjectsWsModule.class,
      DefaultComponentFinder.class,
      DefaultRubyComponentService.class,
      ComponentService.class,
      ResourcesWs.class,
      ComponentsWs.class,
      org.sonar.server.component.ws.AppAction.class,
      org.sonar.server.component.ws.SearchAction.class,
      EventsWs.class,
      NewAlerts.class,
      NewAlerts.newMetadata(),
      ComponentCleanerService.class,

      // views
      ViewIndexDefinition.class,
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      IssueIndexDefinition.class,
      IssueIndexer.class,
      IssueAuthorizationIndexer.class,
      ServerIssueStorage.class,
      IssueUpdater.class,
      FunctionExecutor.class,
      IssueWorkflow.class,
      IssueCommentService.class,
      InternalRubyIssueService.class,
      IssueChangelogService.class,
      ActionService.class,
      Actions.class,
      IssueBulkChangeService.class,
      IssueChangelogFormatter.class,
      IssuesWs.class,
      org.sonar.server.issue.ws.ShowAction.class,
      org.sonar.server.issue.ws.SearchAction.class,
      org.sonar.server.issue.ws.TagsAction.class,
      SetTagsAction.class,
      ComponentTagsAction.class,
      IssueService.class,
      IssueActionsWriter.class,
      IssueQueryService.class,
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
      NewIssuesNotificationFactory.class,
      EmailNotificationChannel.class,
      AlertsEmailTemplate.class,

      // issue filters
      IssueFilterService.class,
      IssueFilterSerializer.class,
      IssueFilterWs.class,
      IssueFilterWriter.class,
      org.sonar.server.issue.filter.AppAction.class,
      org.sonar.server.issue.filter.ShowAction.class,
      org.sonar.server.issue.filter.FavoritesAction.class,

      // action plan
      ActionPlanWs.class,
      ActionPlanService.class,

      // issues actions
      AssignAction.class,
      PlanAction.class,
      SetSeverityAction.class,
      CommentAction.class,
      TransitionAction.class,
      AddTagsAction.class,
      RemoveTagsAction.class,

      // technical debt
      DebtModelService.class,
      DebtModelOperations.class,
      DebtModelLookup.class,
      DebtModelBackup.class,
      DebtModelPluginRepository.class,
      DebtModelXMLExporter.class,
      DebtRulesXMLImporter.class,
      DebtCharacteristicsXMLImporter.class,

      // source
      HtmlSourceDecorator.class,
      SourceService.class,
      SourcesWs.class,
      org.sonar.server.source.ws.ShowAction.class,
      LinesAction.class,
      HashAction.class,
      RawAction.class,
      IndexAction.class,
      ScmAction.class,
      SourceLineIndexDefinition.class,
      SourceLineIndex.class,
      SourceLineIndexer.class,

      // Duplications
      DuplicationsParser.class,
      DuplicationsWs.class,
      DuplicationsJsonWriter.class,
      org.sonar.server.duplication.ws.ShowAction.class,

      // text
      MacroInterpreter.class,
      RubyTextService.class,

      // Notifications
      EmailSettings.class,
      NotificationService.class,
      NotificationCenter.class,
      DefaultNotificationManager.class,

      // Tests
      CoverageService.class,
      TestsWs.class,
      CoveredFilesAction.class,
      org.sonar.server.test.ws.ListAction.class,
      TestIndexDefinition.class,
      TestIndex.class,
      TestIndexer.class,

      // Properties
      PropertiesWs.class,

      // Type validation
      TypeValidations.class,
      IntegerTypeValidation.class,
      FloatTypeValidation.class,
      BooleanTypeValidation.class,
      TextTypeValidation.class,
      StringTypeValidation.class,
      StringListTypeValidation.class,

      // System
      RestartAction.class,
      InfoAction.class,
      UpgradesAction.class,
      MigrateDbSystemAction.class,
      StatusAction.class,
      SystemWs.class,
      SystemMonitor.class,
      SonarQubeMonitor.class,
      EsMonitor.class,
      PluginsMonitor.class,
      JvmPropertiesMonitor.class,
      DatabaseMonitor.class,

      // Plugins WS
      PluginWSCommons.class,
      PluginUpdateAggregator.class,
      InstalledAction.class,
      AvailableAction.class,
      UpdatesAction.class,
      PendingAction.class,
      InstallAction.class,
      org.sonar.server.plugins.ws.UpdateAction.class,
      UninstallAction.class,
      CancelAllAction.class,
      PluginsWs.class,

      // Compute engine
      ReportQueue.class,
      ComputationThreadLauncher.class,
      ComputationWs.class,
      IsQueueEmptyWs.class,
      QueueAction.class,
      HistoryAction.class,
      DefaultPeriodCleaner.class,
      ProjectCleaner.class,
      ProjectSettingsFactory.class,
      IndexPurgeListener.class,

      // UI
      GlobalNavigationAction.class,
      SettingsNavigationAction.class,
      ComponentNavigationAction.class,
      NavigationWs.class);

    addAll(level4AddedComponents);
  }

  @Override
  public PlatformLevel start() {
    ServerExtensionInstaller extensionInstaller = getComponentByType(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(getContainer());

    super.start();

    return this;
  }
}
