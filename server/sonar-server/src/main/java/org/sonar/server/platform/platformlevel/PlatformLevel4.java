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
package org.sonar.server.platform.platformlevel;

import java.util.List;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.ce.CeModule;
import org.sonar.ce.notification.ReportAnalysisFailureNotificationModule;
import org.sonar.ce.settings.ProjectConfigurationFactory;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.authentication.AuthenticationModule;
import org.sonar.server.authentication.LogOAuthWarning;
import org.sonar.server.badge.ws.ProjectBadgesWsModule;
import org.sonar.server.batch.BatchWsModule;
import org.sonar.server.branch.BranchFeatureProxyImpl;
import org.sonar.server.branch.ws.BranchWsModule;
import org.sonar.server.ce.ws.CeWsModule;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.component.ws.ComponentsWsModule;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtModelXMLExporter;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.duplication.ws.DuplicationsParser;
import org.sonar.server.duplication.ws.DuplicationsWs;
import org.sonar.server.duplication.ws.ShowResponseBuilder;
import org.sonar.server.edition.EditionsWsModule;
import org.sonar.server.edition.FinalizeEditionChange;
import org.sonar.server.email.ws.EmailsWsModule;
import org.sonar.server.es.IndexCreator;
import org.sonar.server.es.IndexDefinitions;
import org.sonar.server.es.ProjectIndexersImpl;
import org.sonar.server.es.RecoveryIndexer;
import org.sonar.server.es.metadata.EsDbCompatibilityImpl;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;
import org.sonar.server.event.NewAlerts;
import org.sonar.server.favorite.FavoriteModule;
import org.sonar.server.health.NodeHealthModule;
import org.sonar.server.issue.AddTagsAction;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.CommentAction;
import org.sonar.server.issue.IssueChangePostProcessorImpl;
import org.sonar.server.issue.RemoveTagsAction;
import org.sonar.server.issue.SetSeverityAction;
import org.sonar.server.issue.SetTypeAction;
import org.sonar.server.issue.TransitionAction;
import org.sonar.server.issue.index.IssueIndexDefinition;
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
import org.sonar.server.issue.ws.IssueWsModule;
import org.sonar.server.language.ws.LanguageWs;
import org.sonar.server.measure.custom.ws.CustomMeasuresWsModule;
import org.sonar.server.measure.index.ProjectsEsModule;
import org.sonar.server.measure.live.LiveMeasureModule;
import org.sonar.server.measure.ws.MeasuresWsModule;
import org.sonar.server.measure.ws.TimeMachineWs;
import org.sonar.server.metric.CoreCustomMetrics;
import org.sonar.server.metric.DefaultMetricFinder;
import org.sonar.server.metric.ws.MetricsWsModule;
import org.sonar.server.notification.NotificationModule;
import org.sonar.server.notification.ws.NotificationWsModule;
import org.sonar.server.organization.BillingValidationsProxyImpl;
import org.sonar.server.organization.OrganizationCreationImpl;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.organization.ws.OrganizationsWsModule;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.ws.PermissionsWsModule;
import org.sonar.server.permission.ws.template.DefaultTemplatesResolverImpl;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.platform.ClusterVerification;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.platform.SettingsChangeNotifier;
import org.sonar.server.platform.monitoring.WebSystemInfoModule;
import org.sonar.server.platform.web.WebPagesFilter;
import org.sonar.server.platform.web.requestid.HttpRequestIdModule;
import org.sonar.server.platform.ws.ChangeLogLevelAction;
import org.sonar.server.platform.ws.ChangeLogLevelClusterService;
import org.sonar.server.platform.ws.ChangeLogLevelStandaloneService;
import org.sonar.server.platform.ws.DbMigrationStatusAction;
import org.sonar.server.platform.ws.HealthActionModule;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.LogsAction;
import org.sonar.server.platform.ws.MigrateDbAction;
import org.sonar.server.platform.ws.PingAction;
import org.sonar.server.platform.ws.RestartAction;
import org.sonar.server.platform.ws.ServerWs;
import org.sonar.server.platform.ws.StatusAction;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.platform.ws.UpgradesAction;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.edition.EditionInstaller;
import org.sonar.server.plugins.edition.EditionInstallerExecutor;
import org.sonar.server.plugins.edition.EditionPluginDownloader;
import org.sonar.server.plugins.edition.EditionPluginUninstaller;
import org.sonar.server.plugins.privileged.PrivilegedPluginsBootstraper;
import org.sonar.server.plugins.privileged.PrivilegedPluginsStopper;
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
import org.sonar.server.projectanalysis.ProjectAnalysisModule;
import org.sonar.server.projectlink.ws.ProjectLinksModule;
import org.sonar.server.projecttag.ws.ProjectTagsWsModule;
import org.sonar.server.property.InternalPropertiesImpl;
import org.sonar.server.property.ws.PropertiesWs;
import org.sonar.server.qualitygate.QualityGateModule;
import org.sonar.server.qualityprofile.BuiltInQProfileDefinitionsBridge;
import org.sonar.server.qualityprofile.BuiltInQProfileRepositoryImpl;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotificationDispatcher;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotificationTemplate;
import org.sonar.server.qualityprofile.QProfileBackuperImpl;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.QProfileResetImpl;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.qualityprofile.ws.ProfilesWs;
import org.sonar.server.qualityprofile.ws.QProfilesWsModule;
import org.sonar.server.root.ws.RootWsModule;
import org.sonar.server.rule.CommonRuleDefinitionsImpl;
import org.sonar.server.rule.DeprecatedRulesDefinitionLoader;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.rule.WebServerRuleFinderImpl;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.ws.ActiveRuleCompleter;
import org.sonar.server.rule.ws.RepositoriesAction;
import org.sonar.server.rule.ws.RuleMapper;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.rule.ws.RuleWsSupport;
import org.sonar.server.rule.ws.RulesWs;
import org.sonar.server.rule.ws.TagsAction;
import org.sonar.server.setting.ws.SettingsWsModule;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.ws.HashAction;
import org.sonar.server.source.ws.IndexAction;
import org.sonar.server.source.ws.LinesAction;
import org.sonar.server.source.ws.RawAction;
import org.sonar.server.source.ws.ScmAction;
import org.sonar.server.source.ws.SourcesWs;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.telemetry.TelemetryClient;
import org.sonar.server.telemetry.TelemetryDaemon;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.test.index.TestIndexDefinition;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.test.ws.CoveredFilesAction;
import org.sonar.server.test.ws.TestsWs;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ui.DeprecatedViews;
import org.sonar.server.ui.PageDecorations;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ui.ws.NavigationWsModule;
import org.sonar.server.updatecenter.UpdateCenterModule;
import org.sonar.server.user.DefaultUserFinder;
import org.sonar.server.user.DeprecatedUserFinder;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserSessionFactoryImpl;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.user.ws.UsersWsModule;
import org.sonar.server.usergroups.DefaultGroupCreatorImpl;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.UserGroupsModule;
import org.sonar.server.usertoken.UserTokenModule;
import org.sonar.server.util.TypeValidationModule;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.webhook.WebhookModule;
import org.sonar.server.webhook.ws.WebhooksWsModule;
import org.sonar.server.ws.DeprecatedPropertiesWsFilter;
import org.sonar.server.ws.WebServiceEngine;
import org.sonar.server.ws.WebServiceFilter;
import org.sonar.server.ws.WebServiceReroutingFilter;
import org.sonar.server.ws.ws.WebServicesWsModule;

public class PlatformLevel4 extends PlatformLevel {

  private final List<Object> level4AddedComponents;

  public PlatformLevel4(PlatformLevel parent, List<Object> level4AddedComponents) {
    super("level4", parent);
    this.level4AddedComponents = level4AddedComponents;
  }

  @Override
  protected void configureLevel() {
    addIfStartupLeader(
      IndexCreator.class,
      MetadataIndexDefinition.class,
      MetadataIndex.class,
      EsDbCompatibilityImpl.class);

    addIfCluster(
      NodeHealthModule.class,
      ChangeLogLevelClusterService.class);
    addIfStandalone(
      ChangeLogLevelStandaloneService.class);

    add(
      ClusterVerification.class,
      LogServerId.class,
      LogOAuthWarning.class,
      PluginDownloader.class,
      PluginUninstaller.class,
      DeprecatedViews.class,
      PageRepository.class,
      ResourceTypes.class,
      DefaultResourceTypes.get(),
      SettingsChangeNotifier.class,
      PageDecorations.class,
      Periods.class,
      ServerWs.class,
      BackendCleanup.class,
      IndexDefinitions.class,
      WebPagesFilter.class,

      // edition
      EditionInstaller.class,
      EditionPluginDownloader.class,
      EditionInstallerExecutor.class,
      EditionPluginUninstaller.class,

      // batch
      BatchWsModule.class,

      // update center
      UpdateCenterModule.class,

      // organizations
      OrganizationValidationImpl.class,
      OrganizationCreationImpl.class,
      OrganizationsWsModule.class,
      BillingValidationsProxyImpl.class,

      // quality profile
      BuiltInQProfileDefinitionsBridge.class,
      BuiltInQProfileRepositoryImpl.class,
      ActiveRuleIndexer.class,
      XMLProfileParser.class,
      XMLProfileSerializer.class,
      AnnotationProfileParser.class,
      QProfileComparison.class,
      ProfilesWs.class,
      QProfileTreeImpl.class,
      QProfileRulesImpl.class,
      RuleActivator.class,
      QProfileExporters.class,
      QProfileFactoryImpl.class,
      QProfileCopier.class,
      QProfileBackuperImpl.class,
      QProfileResetImpl.class,
      QProfilesWsModule.class,

      // rule
      RuleIndexDefinition.class,
      RuleIndexer.class,
      AnnotationRuleParser.class,
      XMLRuleParser.class,
      WebServerRuleFinderImpl.class,
      DeprecatedRulesDefinitionLoader.class,
      RuleDefinitionsLoader.class,
      CommonRuleDefinitionsImpl.class,
      RulesDefinitionXmlLoader.class,
      RuleUpdater.class,
      RuleCreator.class,
      org.sonar.server.rule.ws.UpdateAction.class,
      RulesWs.class,
      RuleWsSupport.class,
      org.sonar.server.rule.ws.SearchAction.class,
      org.sonar.server.rule.ws.ShowAction.class,
      org.sonar.server.rule.ws.CreateAction.class,
      org.sonar.server.rule.ws.DeleteAction.class,
      org.sonar.server.rule.ws.ListAction.class,
      TagsAction.class,
      RuleMapper.class,
      ActiveRuleCompleter.class,
      RepositoriesAction.class,
      RuleQueryFactory.class,
      org.sonar.server.rule.ws.AppAction.class,

      // languages
      Languages.class,
      LanguageWs.class,
      org.sonar.server.language.ws.ListAction.class,

      // measure
      MetricsWsModule.class,
      MeasuresWsModule.class,
      CustomMeasuresWsModule.class,
      CoreCustomMetrics.class,
      DefaultMetricFinder.class,
      TimeMachineWs.class,

      QualityGateModule.class,

      // web services
      WebServiceEngine.class,
      WebServicesWsModule.class,
      WebServiceFilter.class,
      DeprecatedPropertiesWsFilter.class,
      WebServiceReroutingFilter.class,

      // localization
      L10nWs.class,
      org.sonar.server.platform.ws.IndexAction.class,

      // authentication
      AuthenticationModule.class,

      // users
      UserSessionFactoryImpl.class,
      SecurityRealmFactory.class,
      DeprecatedUserFinder.class,
      NewUserNotifier.class,
      DefaultUserFinder.class,
      UserIndexDefinition.class,
      UserIndexer.class,
      UserIndex.class,
      UserUpdater.class,
      UsersWsModule.class,
      UserTokenModule.class,

      // groups
      UserGroupsModule.class,
      DefaultGroupCreatorImpl.class,
      DefaultGroupFinder.class,

      // permissions
      DefaultTemplatesResolverImpl.class,
      PermissionsWsModule.class,
      PermissionTemplateService.class,
      PermissionUpdater.class,
      UserPermissionChanger.class,
      GroupPermissionChanger.class,

      // components
      BranchWsModule.class,
      ProjectsWsModule.class,
      ProjectsEsModule.class,
      ProjectTagsWsModule.class,
      ComponentsWsModule.class,
      ComponentService.class,
      ComponentUpdater.class,
      ComponentFinder.class,
      NewAlerts.class,
      NewAlerts.newMetadata(),
      ComponentCleanerService.class,
      ComponentIndexDefinition.class,
      ComponentIndex.class,
      ComponentIndexer.class,
      LiveMeasureModule.class,

      FavoriteModule.class,

      // views
      ViewIndexDefinition.class,
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      IssueIndexDefinition.class,
      IssueIndexer.class,
      IssueIteratorFactory.class,
      PermissionIndexer.class,
      IssueWsModule.class,
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

      // issues actions
      AssignAction.class,
      SetTypeAction.class,
      SetSeverityAction.class,
      CommentAction.class,
      TransitionAction.class,
      AddTagsAction.class,
      RemoveTagsAction.class,
      IssueChangePostProcessorImpl.class,

      // technical debt
      DebtModelPluginRepository.class,
      DebtModelXMLExporter.class,
      DebtRulesXMLImporter.class,

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

      // Duplications
      DuplicationsParser.class,
      DuplicationsWs.class,
      ShowResponseBuilder.class,
      org.sonar.server.duplication.ws.ShowAction.class,

      // text
      MacroInterpreter.class,

      // Notifications
      // Those class are required in order to be able to send emails during startup
      // Without having two NotificationModule (one in StartupLevel and one in Level4)
      BuiltInQualityProfilesNotificationTemplate.class,
      BuiltInQualityProfilesNotificationDispatcher.class,

      NotificationModule.class,
      NotificationWsModule.class,
      EmailsWsModule.class,

      // Tests
      TestsWs.class,
      CoveredFilesAction.class,
      org.sonar.server.test.ws.ListAction.class,
      TestIndexDefinition.class,
      TestIndex.class,
      TestIndexer.class,

      // Settings
      PersistentSettings.class,
      PropertiesWs.class,
      org.sonar.server.property.ws.IndexAction.class,
      SettingsWsModule.class,

      TypeValidationModule.class,

      // Project Links
      ProjectLinksModule.class,

      // Project Analyses
      ProjectAnalysisModule.class,

      // System
      ServerLogging.class,
      RestartAction.class,
      PingAction.class,
      UpgradesAction.class,
      StatusAction.class,
      MigrateDbAction.class,
      LogsAction.class,
      ChangeLogLevelAction.class,
      DbMigrationStatusAction.class,
      HealthActionModule.class,
      SystemWs.class,

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

      // Branch
      BranchFeatureProxyImpl.class,

      // Project badges
      ProjectBadgesWsModule.class,

      // privileged plugins
      PrivilegedPluginsBootstraper.class,
      PrivilegedPluginsStopper.class,

      // Compute engine (must be after Views and Developer Cockpit)
      ReportAnalysisFailureNotificationModule.class,
      CeModule.class,
      CeWsModule.class,

      // SonarSource editions
      EditionsWsModule.class,

      InternalPropertiesImpl.class,
      ProjectConfigurationFactory.class,

      // UI
      NavigationWsModule.class,

      // root
      RootWsModule.class,

      // webhooks
      WebhookModule.class,
      WebhooksWsModule.class,

      // Http Request ID
      HttpRequestIdModule.class,

      RecoveryIndexer.class,
      ProjectIndexersImpl.class);

    // telemetry
    add(TelemetryDataLoader.class);
    addIfStartupLeader(TelemetryDaemon.class, TelemetryClient.class);

    // edition
    addIfStartupLeader(FinalizeEditionChange.class);
    
    // system info
    addIfCluster(WebSystemInfoModule.forClusterMode()).otherwiseAdd(WebSystemInfoModule.forStandaloneMode());

    addAll(level4AddedComponents);
  }

  @Override
  public PlatformLevel start() {
    ServerExtensionInstaller extensionInstaller = get(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(getContainer());

    super.start();

    return this;
  }
}
