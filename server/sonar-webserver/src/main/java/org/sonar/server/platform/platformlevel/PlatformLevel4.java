/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.alm.client.TimeoutConfigurationImpl;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClientConfiguration;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubApplicationHttpClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.GithubPaginatedHttpClientImpl;
import org.sonar.alm.client.github.RatioBasedRateLimitChecker;
import org.sonar.alm.client.github.config.GithubProvisioningConfigValidator;
import org.sonar.alm.client.github.security.GithubAppSecurityImpl;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.auth.bitbucket.BitbucketModule;
import org.sonar.auth.github.GitHubModule;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.auth.gitlab.GitLabModule;
import org.sonar.auth.ldap.LdapModule;
import org.sonar.auth.saml.SamlModule;
import org.sonar.ce.task.projectanalysis.notification.ReportAnalysisFailureNotificationModule;
import org.sonar.ce.task.projectanalysis.taskprocessor.AuditPurgeTaskProcessor;
import org.sonar.ce.task.projectanalysis.taskprocessor.IssueSyncTaskProcessor;
import org.sonar.ce.task.projectanalysis.taskprocessor.ReportTaskProcessor;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectExportTaskProcessor;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.documentation.DefaultDocumentationLinkGenerator;
import org.sonar.core.extension.CoreExtensionsInstaller;
import org.sonar.core.language.LanguagesProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.almintegration.ws.AlmIntegrationsWSModule;
import org.sonar.server.almintegration.ws.CredentialsEncoderHelper;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.almintegration.ws.github.GithubProvisioningWs;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.almsettings.ws.AlmSettingsWsModule;
import org.sonar.server.almsettings.ws.DelegatingDevOpsProjectCreatorFactory;
import org.sonar.server.almsettings.ws.GithubProjectCreatorFactory;
import org.sonar.server.authentication.AuthenticationModule;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifierImpl;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifierNotificationHandler;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifierNotificationTemplate;
import org.sonar.server.authentication.LogOAuthWarning;
import org.sonar.server.authentication.ws.AuthenticationWsModule;
import org.sonar.server.badge.ws.ProjectBadgesWsModule;
import org.sonar.server.batch.BatchWsModule;
import org.sonar.server.branch.BranchFeatureProxyImpl;
import org.sonar.server.branch.ws.BranchWsModule;
import org.sonar.server.ce.CeModule;
import org.sonar.server.ce.projectdump.ProjectExportWsModule;
import org.sonar.server.ce.ws.CeWsModule;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.component.index.EntityDefinitionIndexer;
import org.sonar.server.component.ws.ComponentViewerJsonWriter;
import org.sonar.server.component.ws.ComponentsWsModule;
import org.sonar.server.developers.ws.DevelopersWsModule;
import org.sonar.server.dismissmessage.ws.DismissMessageWsModule;
import org.sonar.server.duplication.ws.DuplicationsParser;
import org.sonar.server.duplication.ws.DuplicationsWs;
import org.sonar.server.duplication.ws.ShowResponseBuilder;
import org.sonar.server.email.ws.EmailsWsModule;
import org.sonar.server.es.IndexCreator;
import org.sonar.server.es.IndexDefinitions;
import org.sonar.server.es.IndexersImpl;
import org.sonar.server.es.RecoveryIndexer;
import org.sonar.server.es.metadata.EsDbCompatibilityImpl;
import org.sonar.server.es.metadata.MetadataIndexDefinition;
import org.sonar.server.es.metadata.MetadataIndexImpl;
import org.sonar.server.extension.CoreExtensionBootstraper;
import org.sonar.server.extension.CoreExtensionStopper;
import org.sonar.server.favorite.FavoriteModule;
import org.sonar.server.favorite.ws.FavoriteWsModule;
import org.sonar.server.feature.ws.FeatureWsModule;
import org.sonar.server.health.NodeHealthModule;
import org.sonar.server.hotspot.ws.HotspotsWsModule;
import org.sonar.server.issue.AddTagsAction;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.CommentAction;
import org.sonar.server.issue.IssueChangePostProcessorImpl;
import org.sonar.server.issue.RemoveTagsAction;
import org.sonar.server.issue.SetSeverityAction;
import org.sonar.server.issue.SetTypeAction;
import org.sonar.server.issue.TransitionAction;
import org.sonar.server.issue.index.AsyncIssueIndexingImpl;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.notification.IssuesChangesNotificationModule;
import org.sonar.server.issue.notification.MyNewIssuesEmailTemplate;
import org.sonar.server.issue.notification.MyNewIssuesNotificationHandler;
import org.sonar.server.issue.notification.NewIssuesEmailTemplate;
import org.sonar.server.issue.notification.NewIssuesNotificationHandler;
import org.sonar.server.issue.ws.IssueWsModule;
import org.sonar.server.language.LanguageValidation;
import org.sonar.server.language.ws.LanguageWs;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.loginmessage.LoginMessageFeature;
import org.sonar.server.management.DelegatingManagedServices;
import org.sonar.server.measure.index.ProjectsEsModule;
import org.sonar.server.measure.live.LiveMeasureModule;
import org.sonar.server.measure.ws.MeasuresWsModule;
import org.sonar.server.metric.UnanalyzedLanguageMetrics;
import org.sonar.server.metric.ws.MetricsWsModule;
import org.sonar.server.monitoring.ComputeEngineMetricStatusTask;
import org.sonar.server.monitoring.ElasticSearchMetricTask;
import org.sonar.server.monitoring.MainCollector;
import org.sonar.server.monitoring.MonitoringWsModule;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.sonar.server.monitoring.SonarLintConnectedClientsTask;
import org.sonar.server.monitoring.WebUptimeTask;
import org.sonar.server.monitoring.ce.NumberOfTasksInQueueTask;
import org.sonar.server.monitoring.ce.RecentTasksDurationTask;
import org.sonar.server.monitoring.devops.AzureMetricsTask;
import org.sonar.server.monitoring.devops.BitbucketMetricsTask;
import org.sonar.server.monitoring.devops.GithubMetricsTask;
import org.sonar.server.monitoring.devops.GitlabMetricsTask;
import org.sonar.server.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.newcodeperiod.ws.NewCodePeriodsWsModule;
import org.sonar.server.notification.NotificationModule;
import org.sonar.server.notification.ws.NotificationWsModule;
import org.sonar.server.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.permission.GroupPermissionChanger;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChanger;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.ws.PermissionsWsModule;
import org.sonar.server.platform.ClusterVerification;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.SystemInfoWriterModule;
import org.sonar.server.platform.WebCoreExtensionsInstaller;
import org.sonar.server.platform.db.CheckAnyonePermissionsAtStartup;
import org.sonar.server.platform.db.CheckLanguageSpecificParamsAtStartup;
import org.sonar.server.platform.web.SonarLintConnectionFilter;
import org.sonar.server.platform.web.WebServiceFilter;
import org.sonar.server.platform.web.WebServiceReroutingFilter;
import org.sonar.server.platform.web.requestid.HttpRequestIdModule;
import org.sonar.server.platform.ws.ChangeLogLevelServiceModule;
import org.sonar.server.platform.ws.HealthCheckerModule;
import org.sonar.server.platform.ws.L10nWs;
import org.sonar.server.platform.ws.ServerWs;
import org.sonar.server.platform.ws.SystemWsModule;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.PluginUninstaller;
import org.sonar.server.plugins.PluginsRiskConsentFilter;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.ws.AvailableAction;
import org.sonar.server.plugins.ws.CancelAllAction;
import org.sonar.server.plugins.ws.DownloadAction;
import org.sonar.server.plugins.ws.InstallAction;
import org.sonar.server.plugins.ws.InstalledAction;
import org.sonar.server.plugins.ws.PendingAction;
import org.sonar.server.plugins.ws.PluginUpdateAggregator;
import org.sonar.server.plugins.ws.PluginsWs;
import org.sonar.server.plugins.ws.UninstallAction;
import org.sonar.server.plugins.ws.UpdatesAction;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectQGChangeEventListener;
import org.sonar.server.project.VisibilityService;
import org.sonar.server.project.ws.ProjectsWsModule;
import org.sonar.server.projectanalysis.ws.ProjectAnalysisWsModule;
import org.sonar.server.projectlink.ws.ProjectLinksModule;
import org.sonar.server.projecttag.ws.ProjectTagsWsModule;
import org.sonar.server.property.InternalPropertiesImpl;
import org.sonar.server.pushapi.ServerPushModule;
import org.sonar.server.pushapi.hotspots.HotspotChangeEventServiceImpl;
import org.sonar.server.pushapi.issues.IssueChangeEventServiceImpl;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventServiceImpl;
import org.sonar.server.qualitygate.QualityGateModule;
import org.sonar.server.qualitygate.notification.QGChangeNotificationHandler;
import org.sonar.server.qualitygate.ws.QualityGateWsModule;
import org.sonar.server.qualityprofile.QProfileBackuperImpl;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.QProfileParser;
import org.sonar.server.qualityprofile.QProfileResetImpl;
import org.sonar.server.qualityprofile.QProfileRulesImpl;
import org.sonar.server.qualityprofile.QProfileTreeImpl;
import org.sonar.server.qualityprofile.builtin.BuiltInQPChangeNotificationHandler;
import org.sonar.server.qualityprofile.builtin.BuiltInQPChangeNotificationTemplate;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileRepositoryImpl;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.qualityprofile.ws.QProfilesWsModule;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleDescriptionFormatter;
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
import org.sonar.server.saml.ws.SamlValidationModule;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.scannercache.ws.AnalysisCacheWsModule;
import org.sonar.server.setting.ProjectConfigurationLoaderImpl;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.setting.ws.SettingsWsModule;
import org.sonar.server.source.ws.SourceWsModule;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.telemetry.CloudUsageDataProvider;
import org.sonar.server.telemetry.QualityProfileDataProvider;
import org.sonar.server.telemetry.TelemetryClient;
import org.sonar.server.telemetry.TelemetryDaemon;
import org.sonar.server.telemetry.TelemetryDataJsonWriter;
import org.sonar.server.telemetry.TelemetryDataLoaderImpl;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ui.WebAnalyticsLoaderImpl;
import org.sonar.server.ui.ws.NavigationWsModule;
import org.sonar.server.updatecenter.UpdateCenterModule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserSessionFactoryImpl;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.ws.UsersWsModule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.UserGroupsModule;
import org.sonar.server.usertoken.UserTokenModule;
import org.sonar.server.usertoken.ws.UserTokenWsModule;
import org.sonar.server.util.TypeValidationModule;
import org.sonar.server.view.index.ViewIndex;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.webhook.WebhookModule;
import org.sonar.server.webhook.WebhookQGChangeEventListener;
import org.sonar.server.webhook.ws.WebhooksWsModule;
import org.sonar.server.ws.WebServiceEngine;
import org.sonar.server.ws.ws.WebServicesWsModule;

import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel4OrNone;

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
      MetadataIndexImpl.class,
      EsDbCompatibilityImpl.class);

    addIfCluster(new NodeHealthModule());

    add(
      RuleDescriptionFormatter.class,
      ClusterVerification.class,
      LogServerId.class,
      LogOAuthWarning.class,
      PluginUninstaller.class,
      PluginDownloader.class,
      PageRepository.class,
      ResourceTypes.class,
      DefaultResourceTypes.get(),
      SettingsChangeNotifier.class,
      ServerWs.class,
      IndexDefinitions.class,
      WebAnalyticsLoaderImpl.class,
      new MonitoringWsModule(),
      DefaultBranchNameResolver.class,
      DefaultDocumentationLinkGenerator.class,
      DelegatingManagedServices.class,
      DelegatingDevOpsProjectCreatorFactory.class,

      // batch
      new BatchWsModule(),

      // update center
      new UpdateCenterModule(),

      // quality profile
      BuiltInQProfileRepositoryImpl.class,
      ActiveRuleIndexer.class,
      QProfileComparison.class,
      QProfileTreeImpl.class,
      QProfileRulesImpl.class,
      RuleActivator.class,
      QualityProfileChangeEventServiceImpl.class,
      QProfileExporters.class,
      QProfileFactoryImpl.class,
      QProfileCopier.class,
      QProfileBackuperImpl.class,
      QProfileParser.class,
      QProfileResetImpl.class,
      new QProfilesWsModule(),

      // rule
      RuleIndexDefinition.class,
      RuleIndexer.class,
      WebServerRuleFinderImpl.class,
      RuleDefinitionsLoader.class,
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
      LanguagesProvider.class,
      LanguageWs.class,
      LanguageValidation.class,
      org.sonar.server.language.ws.ListAction.class,

      // measure
      new MetricsWsModule(),
      new MeasuresWsModule(),
      UnanalyzedLanguageMetrics.class,

      new QualityGateModule(),
      new QualityGateWsModule(),

      // web services
      WebServiceEngine.class,
      new WebServicesWsModule(),
      SonarLintConnectionFilter.class,
      WebServiceFilter.class,
      WebServiceReroutingFilter.class,

      // localization
      L10nWs.class,
      org.sonar.server.platform.ws.IndexAction.class,

      // authentication
      new AuthenticationModule(),
      new AuthenticationWsModule(),
      new BitbucketModule(),
      GitHubSettings.class,
      new GitHubModule(),
      new GitLabModule(),
      new LdapModule(),
      new SamlModule(),
      new SamlValidationModule(),
      DefaultAdminCredentialsVerifierImpl.class,
      DefaultAdminCredentialsVerifierNotificationTemplate.class,
      DefaultAdminCredentialsVerifierNotificationHandler.class,

      // users
      UserSessionFactoryImpl.class,
      SecurityRealmFactory.class,
      NewUserNotifier.class,
      UserUpdater.class,
      new UsersWsModule(),
      new UserTokenModule(),
      new UserTokenWsModule(),

      // groups
      new UserGroupsModule(),
      DefaultGroupFinder.class,

      // permissions
      DefaultTemplatesResolverImpl.class,
      new PermissionsWsModule(),
      PermissionTemplateService.class,
      PermissionUpdater.class,
      UserPermissionChanger.class,
      GroupPermissionChanger.class,
      CheckAnyonePermissionsAtStartup.class,
      CheckLanguageSpecificParamsAtStartup.class,
      VisibilityService.class,

      // components
      new BranchWsModule(),
      new ProjectsWsModule(),
      new ProjectsEsModule(),
      new ProjectTagsWsModule(),
      new ComponentsWsModule(),
      ComponentService.class,
      ComponentUpdater.class,
      ComponentFinder.class,
      QGChangeNotificationHandler.class,
      QGChangeNotificationHandler.newMetadata(),
      ComponentCleanerService.class,
      ComponentIndexDefinition.class,
      ComponentIndex.class,
      EntityDefinitionIndexer.class,
      new LiveMeasureModule(),
      ComponentViewerJsonWriter.class,

      new DevelopersWsModule(),

      new FavoriteModule(),
      new FavoriteWsModule(),

      // views
      ViewIndexDefinition.class,
      ViewIndexer.class,
      ViewIndex.class,

      // issues
      IssueIndexDefinition.class,
      AsyncIssueIndexingImpl.class,
      IssueIndexer.class,
      IssueIteratorFactory.class,
      PermissionIndexer.class,
      new IssueWsModule(),
      NewIssuesEmailTemplate.class,
      MyNewIssuesEmailTemplate.class,
      new IssuesChangesNotificationModule(),
      NewIssuesNotificationHandler.class,
      NewIssuesNotificationHandler.newMetadata(),
      MyNewIssuesNotificationHandler.class,
      MyNewIssuesNotificationHandler.newMetadata(),
      IssueChangeEventServiceImpl.class,
      HotspotChangeEventServiceImpl.class,

      // issues actions
      AssignAction.class,
      SetTypeAction.class,
      SetSeverityAction.class,
      CommentAction.class,
      TransitionAction.class,
      AddTagsAction.class,
      RemoveTagsAction.class,
      IssueChangePostProcessorImpl.class,

      // hotspots
      new HotspotsWsModule(),

      // source
      new SourceWsModule(),

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
      BuiltInQPChangeNotificationTemplate.class,
      BuiltInQPChangeNotificationHandler.class,

      new NotificationModule(),
      new NotificationWsModule(),
      new EmailsWsModule(),

      // Settings
      ProjectConfigurationLoaderImpl.class,
      PersistentSettings.class,
      new SettingsWsModule(),

      new TypeValidationModule(),

      // New Code Periods
      new NewCodePeriodsWsModule(),
      NewCodeDefinitionResolver.class,

      // Project Links
      new ProjectLinksModule(),

      // Project Analyses
      new ProjectAnalysisWsModule(),

      // System
      ServerLogging.class,
      new ChangeLogLevelServiceModule(getWebServer()),
      new HealthCheckerModule(getWebServer()),
      new SystemWsModule(),

      // Plugins WS
      PluginUpdateAggregator.class,
      InstalledAction.class,
      AvailableAction.class,
      DownloadAction.class,
      UpdatesAction.class,
      PendingAction.class,
      InstallAction.class,
      org.sonar.server.plugins.ws.UpdateAction.class,
      UninstallAction.class,
      CancelAllAction.class,
      PluginsWs.class,

      // Scanner Cache
      ScannerCache.class,
      new AnalysisCacheWsModule(),

      // ALM integrations
      TimeoutConfigurationImpl.class,
      CredentialsEncoderHelper.class,
      ImportHelper.class,
      ProjectKeyGenerator.class,
      RatioBasedRateLimitChecker.class,
      GithubAppSecurityImpl.class,
      GithubApplicationClientImpl.class,
      GithubPaginatedHttpClientImpl.class,
      GithubApplicationHttpClientImpl.class,
      GithubProvisioningConfigValidator.class,
      GithubProvisioningWs.class,
      GithubProjectCreatorFactory.class,
      GithubPermissionConverter.class,
      BitbucketCloudRestClientConfiguration.class,
      BitbucketServerRestClient.class,
      GitlabHttpClient.class,
      AzureDevOpsHttpClient.class,
      new AlmIntegrationsWSModule(),
      BitbucketCloudValidator.class,
      BitbucketServerSettingsValidator.class,
      GithubGlobalSettingsValidator.class,
      GitlabGlobalSettingsValidator.class,
      AzureDevOpsValidator.class,

      // ALM settings
      new AlmSettingsWsModule(),

      // Project export
      new ProjectExportWsModule(),

      // Branch
      BranchFeatureProxyImpl.class,

      // Project badges
      new ProjectBadgesWsModule(),

      // Core Extensions
      CoreExtensionBootstraper.class,
      CoreExtensionStopper.class,

      MultipleAlmFeature.class,

      LoginMessageFeature.class,

      // ServerPush endpoints
      new ServerPushModule(),

      // Compute engine (must be after Views and Developer Cockpit)
      new ReportAnalysisFailureNotificationModule(),
      new CeModule(),
      new CeWsModule(),
      ReportTaskProcessor.class,
      IssueSyncTaskProcessor.class,
      AuditPurgeTaskProcessor.class,
      ProjectExportTaskProcessor.class,

      // SonarSource editions
      PlatformEditionProvider.class,

      InternalPropertiesImpl.class,

      // UI
      new NavigationWsModule(),

      // SonarQube features
      new FeatureWsModule(),

      // webhooks
      WebhookQGChangeEventListener.class,
      new WebhookModule(),
      new WebhooksWsModule(),

      ProjectQGChangeEventListener.class,

      // Http Request ID
      new HttpRequestIdModule(),

      RecoveryIndexer.class,
      IndexersImpl.class,

      // telemetry
      TelemetryDataLoaderImpl.class,
      TelemetryDataJsonWriter.class,
      TelemetryDaemon.class,
      TelemetryClient.class,
      CloudUsageDataProvider.class,
      QualityProfileDataProvider.class,

      // monitoring
      ServerMonitoringMetrics.class,

      // dismiss message
      new DismissMessageWsModule(),

      AzureMetricsTask.class,
      BitbucketMetricsTask.class,
      GithubMetricsTask.class,
      GitlabMetricsTask.class,

      NumberOfTasksInQueueTask.class,
      RecentTasksDurationTask.class,

      ComputeEngineMetricStatusTask.class,
      ElasticSearchMetricTask.class,
      WebUptimeTask.class,
      SonarLintConnectedClientsTask.class,

      MainCollector.class,

      PluginsRiskConsentFilter.class);

    // system info
    add(new SystemInfoWriterModule(getWebServer()));

    addAll(level4AddedComponents);
  }

  @Override
  public PlatformLevel start() {
    SpringComponentContainer container = getContainer();
    CoreExtensionsInstaller coreExtensionsInstaller = parent.get(WebCoreExtensionsInstaller.class);
    coreExtensionsInstaller.install(container, hasPlatformLevel4OrNone(), noAdditionalSideFilter());

    ServerExtensionInstaller extensionInstaller = parent.get(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(container);

    super.start();

    return this;
  }

}
