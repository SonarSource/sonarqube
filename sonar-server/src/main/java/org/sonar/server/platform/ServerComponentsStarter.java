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

import org.sonar.api.config.EmailSettings;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.profiles.AnnotationProfileParser;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.component.db.ComponentDao;
import org.sonar.core.issue.IssueFilterSerializer;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.FunctionExecutor;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.measure.MeasureFilterEngine;
import org.sonar.core.measure.MeasureFilterExecutor;
import org.sonar.core.measure.MeasureFilterFactory;
import org.sonar.core.metric.DefaultMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtManager;
import org.sonar.core.technicaldebt.TechnicalDebtModelRepository;
import org.sonar.core.technicaldebt.TechnicalDebtModelSynchronizer;
import org.sonar.core.technicaldebt.TechnicalDebtXMLImporter;
import org.sonar.core.test.TestPlanPerspectiveLoader;
import org.sonar.core.test.TestablePerspectiveLoader;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.component.DefaultComponentFinder;
import org.sonar.server.component.DefaultRubyComponentService;
import org.sonar.server.debt.DebtCharacteristicsXMLImporter;
import org.sonar.server.debt.DebtModelSynchronizer;
import org.sonar.server.debt.DebtRulesXMLImporter;
import org.sonar.server.debt.DebtService;
import org.sonar.server.es.ESIndex;
import org.sonar.server.issue.ActionPlanService;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.AssignAction;
import org.sonar.server.issue.CommentAction;
import org.sonar.server.issue.DefaultIssueFinder;
import org.sonar.server.issue.InternalRubyIssueService;
import org.sonar.server.issue.IssueBulkChangeService;
import org.sonar.server.issue.IssueChangelogFormatter;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.IssueStatsFinder;
import org.sonar.server.issue.PlanAction;
import org.sonar.server.issue.PublicRubyIssueService;
import org.sonar.server.issue.ServerIssueStorage;
import org.sonar.server.issue.SetSeverityAction;
import org.sonar.server.issue.TransitionAction;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.filter.IssueFilterWs;
import org.sonar.server.issue.ws.IssueShowWsHandler;
import org.sonar.server.issue.ws.IssuesWs;
import org.sonar.server.notifications.NotificationCenter;
import org.sonar.server.notifications.NotificationService;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.InternalPermissionTemplateService;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.qualitygate.ws.QgateAppHandler;
import org.sonar.server.qualitygate.ws.QualityGatesWs;
import org.sonar.server.qualityprofile.ESActiveRule;
import org.sonar.server.qualityprofile.ProfilesManager;
import org.sonar.server.qualityprofile.QProfileActiveRuleOperations;
import org.sonar.server.qualityprofile.QProfileBackup;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileOperations;
import org.sonar.server.qualityprofile.QProfileProjectLookup;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.qualityprofile.QProfileRepositoryExporter;
import org.sonar.server.qualityprofile.QProfileRuleLookup;
import org.sonar.server.qualityprofile.QProfiles;
import org.sonar.server.rule.DeprecatedRulesDefinition;
import org.sonar.server.rule.ESRuleTags;
import org.sonar.server.rule.RubyRuleService;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleOperations;
import org.sonar.server.rule.RuleRegistration;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.rule.RuleRepositories;
import org.sonar.server.rule.RuleTagLookup;
import org.sonar.server.rule.RuleTagOperations;
import org.sonar.server.rule.RuleTags;
import org.sonar.server.rule.Rules;
import org.sonar.server.rule.ws.AddTagsWsHandler;
import org.sonar.server.rule.ws.RemoveTagsWsHandler;
import org.sonar.server.rule.ws.RuleSearchWsHandler;
import org.sonar.server.rule.ws.RuleShowWsHandler;
import org.sonar.server.rule.ws.RuleTagsWs;
import org.sonar.server.rule.ws.RulesWs;
import org.sonar.server.source.CodeColorizers;
import org.sonar.server.source.DeprecatedSourceDecorator;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.ws.SourcesShowWsHandler;
import org.sonar.server.source.ws.SourcesWs;
import org.sonar.server.startup.CleanPreviewAnalysisCache;
import org.sonar.server.startup.CopyRequirementsFromCharacteristicsToRules;
import org.sonar.server.startup.GenerateBootstrapIndex;
import org.sonar.server.startup.GeneratePluginIndex;
import org.sonar.server.startup.GwtPublisher;
import org.sonar.server.startup.JdbcDriverDeployer;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.startup.RegisterDebtModel;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.startup.RegisterNewDashboards;
import org.sonar.server.startup.RegisterNewMeasureFilters;
import org.sonar.server.startup.RegisterNewProfiles;
import org.sonar.server.startup.RegisterPermissionTemplates;
import org.sonar.server.startup.RegisterServletFilters;
import org.sonar.server.startup.RenameDeprecatedPropertyKeys;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.text.RubyTextService;
import org.sonar.server.ui.PageDecorations;
import org.sonar.server.ui.Views;
import org.sonar.server.user.DefaultUserService;
import org.sonar.server.user.GroupMembershipFinder;
import org.sonar.server.user.GroupMembershipService;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.util.BooleanTypeValidation;
import org.sonar.server.util.FloatTypeValidation;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.StringListTypeValidation;
import org.sonar.server.util.StringTypeValidation;
import org.sonar.server.util.TextTypeValidation;
import org.sonar.server.util.TypeValidations;
import org.sonar.server.ws.ListingWs;
import org.sonar.server.ws.WebServiceEngine;

public class ServerComponentsStarter {

  public void start(ComponentContainer pico) {
    registerComponents(pico);
    pico.startComponents();
    executeStartupTaks(pico);
  }

  private void registerComponents(ComponentContainer pico) {
    pico.addSingleton(ESIndex.class);
    pico.addSingleton(UpdateCenterClient.class);
    pico.addSingleton(UpdateCenterMatrixFactory.class);
    pico.addSingleton(PluginDownloader.class);
    pico.addSingleton(ChartFactory.class);
    pico.addSingleton(Languages.class);
    pico.addSingleton(Views.class);
    pico.addSingleton(CodeColorizers.class);
    pico.addComponent(ProfilesManager.class, false);
    pico.addSingleton(SecurityRealmFactory.class);
    pico.addSingleton(ServerLifecycleNotifier.class);
    pico.addSingleton(AnnotationProfileParser.class);
    pico.addSingleton(DefaultRuleFinder.class);
    pico.addSingleton(DefaultMetricFinder.class);
    pico.addSingleton(ResourceTypes.class);
    pico.addSingleton(SettingsChangeNotifier.class);
    pico.addSingleton(PageDecorations.class);
    pico.addSingleton(MeasureFilterFactory.class);
    pico.addSingleton(MeasureFilterExecutor.class);
    pico.addSingleton(MeasureFilterEngine.class);
    pico.addSingleton(PreviewCache.class);
    pico.addSingleton(DefaultResourcePermissions.class);
    pico.addSingleton(Periods.class);

    // web services
    pico.addSingleton(WebServiceEngine.class);
    pico.addSingleton(ListingWs.class);

    // quality profiles
    pico.addSingleton(QProfileRuleLookup.class);
    pico.addSingleton(QProfiles.class);
    pico.addSingleton(QProfileLookup.class);
    pico.addSingleton(QProfileOperations.class);
    pico.addSingleton(QProfileActiveRuleOperations.class);
    pico.addSingleton(QProfileProjectOperations.class);
    pico.addSingleton(QProfileProjectLookup.class);
    pico.addSingleton(QProfileBackup.class);
    pico.addSingleton(QProfileRepositoryExporter.class);
    pico.addSingleton(ESActiveRule.class);

    // quality gates
    pico.addSingleton(QualityGateDao.class);
    pico.addSingleton(QualityGateConditionDao.class);
    pico.addSingleton(QualityGates.class);
    pico.addSingleton(ProjectQgateAssociationDao.class);
    pico.addSingleton(QgateProjectFinder.class);
    pico.addSingleton(QgateAppHandler.class);
    pico.addSingleton(QualityGatesWs.class);

    // users
    pico.addSingleton(HibernateUserFinder.class);
    pico.addSingleton(NewUserNotifier.class);
    pico.addSingleton(DefaultUserFinder.class);
    pico.addSingleton(DefaultUserService.class);

    // groups
    pico.addSingleton(GroupMembershipService.class);
    pico.addSingleton(GroupMembershipFinder.class);

    // permissions
    pico.addSingleton(PermissionFacade.class);
    pico.addSingleton(InternalPermissionService.class);
    pico.addSingleton(InternalPermissionTemplateService.class);
    pico.addSingleton(PermissionFinder.class);

    // components
    pico.addSingleton(DefaultComponentFinder.class);
    pico.addSingleton(DefaultRubyComponentService.class);
    pico.addSingleton(ComponentDao.class);

    // issues
    pico.addSingleton(ServerIssueStorage.class);
    pico.addSingleton(IssueUpdater.class);
    pico.addSingleton(FunctionExecutor.class);
    pico.addSingleton(IssueWorkflow.class);
    pico.addSingleton(IssueService.class);
    pico.addSingleton(IssueCommentService.class);
    pico.addSingleton(DefaultIssueFinder.class);
    pico.addSingleton(IssueStatsFinder.class);
    pico.addSingleton(PublicRubyIssueService.class);
    pico.addSingleton(InternalRubyIssueService.class);
    pico.addSingleton(ActionPlanService.class);
    pico.addSingleton(IssueChangelogService.class);
    pico.addSingleton(IssueNotifications.class);
    pico.addSingleton(ActionService.class);
    pico.addSingleton(Actions.class);
    pico.addSingleton(IssueFilterSerializer.class);
    pico.addSingleton(IssueFilterService.class);
    pico.addSingleton(IssueBulkChangeService.class);
    pico.addSingleton(IssueChangelogFormatter.class);
    pico.addSingleton(IssueFilterWs.class);
    pico.addSingleton(IssueShowWsHandler.class);
    pico.addSingleton(IssuesWs.class);

    // issues actions
    pico.addSingleton(AssignAction.class);
    pico.addSingleton(PlanAction.class);
    pico.addSingleton(SetSeverityAction.class);
    pico.addSingleton(CommentAction.class);
    pico.addSingleton(TransitionAction.class);

    // rules
    pico.addSingleton(Rules.class);
    pico.addSingleton(RuleOperations.class);
    pico.addSingleton(RuleRegistry.class);
    pico.addSingleton(RubyRuleService.class);
    pico.addSingleton(RuleRepositories.class);
    pico.addSingleton(RulesWs.class);
    pico.addSingleton(RuleShowWsHandler.class);
    pico.addSingleton(RuleSearchWsHandler.class);
    pico.addSingleton(AddTagsWsHandler.class);
    pico.addSingleton(RemoveTagsWsHandler.class);

    // rule tags
    pico.addSingleton(ESRuleTags.class);
    pico.addSingleton(RuleTagLookup.class);
    pico.addSingleton(RuleTagOperations.class);
    pico.addSingleton(RuleTags.class);
    pico.addSingleton(RuleTagsWs.class);

    // technical debt
    pico.addSingleton(DebtService.class);
    pico.addSingleton(TechnicalDebtModelSynchronizer.class);
    pico.addSingleton(DebtModelSynchronizer.class);
    pico.addSingleton(TechnicalDebtModelRepository.class);
    pico.addSingleton(TechnicalDebtXMLImporter.class);
    pico.addSingleton(DebtRulesXMLImporter.class);
    pico.addSingleton(DebtCharacteristicsXMLImporter.class);
    pico.addSingleton(DefaultTechnicalDebtManager.class);

    // source
    pico.addSingleton(HtmlSourceDecorator.class);
    pico.addSingleton(DeprecatedSourceDecorator.class);
    pico.addSingleton(SourceService.class);
    pico.addSingleton(SourcesWs.class);
    pico.addSingleton(SourcesShowWsHandler.class);

    // text
    pico.addSingleton(MacroInterpreter.class);
    pico.addSingleton(RubyTextService.class);

    // Notifications
    pico.addSingleton(EmailSettings.class);
    pico.addSingleton(NotificationService.class);
    pico.addSingleton(NotificationCenter.class);
    pico.addSingleton(DefaultNotificationManager.class);

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

    ServerExtensionInstaller extensionRegistrar = pico.getComponentByType(ServerExtensionInstaller.class);
    extensionRegistrar.registerExtensions(pico);
  }

  private void executeStartupTaks(ComponentContainer pico) {
    ComponentContainer startupContainer = pico.createChild();
    startupContainer.addSingleton(GwtPublisher.class);
    startupContainer.addSingleton(RegisterMetrics.class);
    startupContainer.addSingleton(DeprecatedRulesDefinition.class);
    startupContainer.addSingleton(RuleDefinitionsLoader.class);
    startupContainer.addSingleton(RuleRegistration.class);
    startupContainer.addSingleton(RegisterNewProfiles.class);
    startupContainer.addSingleton(JdbcDriverDeployer.class);
    startupContainer.addSingleton(RegisterDebtModel.class);
    startupContainer.addSingleton(GeneratePluginIndex.class);
    startupContainer.addSingleton(GenerateBootstrapIndex.class);
    startupContainer.addSingleton(RegisterNewMeasureFilters.class);
    startupContainer.addSingleton(RegisterNewDashboards.class);
    startupContainer.addSingleton(RegisterPermissionTemplates.class);
    startupContainer.addSingleton(RenameDeprecatedPropertyKeys.class);
    startupContainer.addSingleton(LogServerId.class);
    startupContainer.addSingleton(RegisterServletFilters.class);
    startupContainer.addSingleton(CleanPreviewAnalysisCache.class);
    startupContainer.addSingleton(CopyRequirementsFromCharacteristicsToRules.class);
    startupContainer.startComponents();

    startupContainer.getComponentByType(ServerLifecycleNotifier.class).notifyStart();

    // Do not put the following statements in a finally block.
    // It would hide the possible exception raised during startup
    // See SONAR-3107
    startupContainer.stopComponents();
    pico.removeChild();
    pico.getComponentByType(DatabaseSessionFactory.class).clear();
  }
}
