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
import org.apache.commons.configuration.BaseConfiguration;
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
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.component.db.ComponentDao;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.GwtI18n;
import org.sonar.core.i18n.RuleI18nManager;
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
import org.sonar.core.persistence.*;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.core.test.TestPlanPerspectiveLoader;
import org.sonar.core.test.TestablePerspectiveLoader;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.core.user.HibernateUserFinder;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;
import org.sonar.server.authentication.ws.AuthenticationWs;
import org.sonar.server.charts.ChartFactory;
import org.sonar.server.cluster.LocalNonBlockingWorkQueue;
import org.sonar.server.component.DefaultComponentFinder;
import org.sonar.server.component.DefaultRubyComponentService;
import org.sonar.server.component.ws.ComponentsWs;
import org.sonar.server.component.ws.ProjectsWs;
import org.sonar.server.component.ws.ResourcesWs;
import org.sonar.server.db.EmbeddedDatabaseFactory;
import org.sonar.server.db.migrations.DatabaseMigrations;
import org.sonar.server.db.migrations.DatabaseMigrator;
import org.sonar.server.debt.*;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;
import org.sonar.server.issue.*;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.issue.actionplan.ActionPlanWs;
import org.sonar.server.issue.filter.IssueFilterService;
import org.sonar.server.issue.filter.IssueFilterWriter;
import org.sonar.server.issue.filter.IssueFilterWs;
import org.sonar.server.issue.ws.IssueShowAction;
import org.sonar.server.issue.ws.IssuesWs;
import org.sonar.server.measure.ws.TimeMachineWs;
import org.sonar.server.notifications.NotificationCenter;
import org.sonar.server.notifications.NotificationService;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.InternalPermissionTemplateService;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.platform.ws.RestartHandler;
import org.sonar.server.platform.ws.ServerWs;
import org.sonar.server.platform.ws.SystemWs;
import org.sonar.server.plugins.*;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.qualitygate.RegisterQualityGates;
import org.sonar.server.qualitygate.ws.*;
import org.sonar.server.qualityprofile.*;
import org.sonar.server.qualityprofile.ws.ProfilesWs;
import org.sonar.server.qualityprofile.ws.QProfileRestoreDefaultAction;
import org.sonar.server.rule.*;
import org.sonar.server.rule.ws.*;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.rule2.ws.RulesWebService;
import org.sonar.server.rule2.ws.SearchAction;
import org.sonar.server.rule2.ws.TagsAction;
import org.sonar.server.search.IndexUtils;
import org.sonar.server.source.CodeColorizers;
import org.sonar.server.source.DeprecatedSourceDecorator;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.source.ws.ScmAction;
import org.sonar.server.source.ws.ScmWriter;
import org.sonar.server.source.ws.ShowAction;
import org.sonar.server.source.ws.SourcesWs;
import org.sonar.server.startup.*;
import org.sonar.server.test.ws.TestsWs;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.text.RubyTextService;
import org.sonar.server.ui.JRubyI18n;
import org.sonar.server.ui.JRubyProfiling;
import org.sonar.server.ui.PageDecorations;
import org.sonar.server.ui.Views;
import org.sonar.server.updatecenter.ws.UpdateCenterWs;
import org.sonar.server.user.*;
import org.sonar.server.user.ws.UsersWs;
import org.sonar.server.util.*;
import org.sonar.server.ws.ListingWs;
import org.sonar.server.ws.WebServiceEngine;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class ServerComponents {

  private final Object[] rootComponents;
  private List level4AddedComponents = Lists.newArrayList();

  ServerComponents(Object... rootComponents) {
    this.rootComponents = rootComponents;
  }

  /**
   * All the stuff required to connect to database
   */
  Collection level1Components() {
    List components = Lists.newArrayList(rootComponents);
    components.addAll(Arrays.asList(
      new BaseConfiguration(),
      ServerSettings.class,
      ServerImpl.class,
      Logback.class,
      Profiling.class,
      JRubyProfiling.class,
      EmbeddedDatabaseFactory.class,
      DefaultDatabase.class,
      MyBatis.class,
      LocalNonBlockingWorkQueue.class,
      DatabaseServerCompatibility.class,
      DatabaseVersion.class,
      PurgeProfiler.class,
      DefaultServerFileSystem.class,
      PreviewDatabaseFactory.class,
      SemaphoreUpdater.class,
      SemaphoresImpl.class,
      TempFolderCleaner.class,
      new TempFolderProvider(),
      System2.INSTANCE,
      org.sonar.server.rule2.RuleDao.class,
      org.sonar.server.rule2.ActiveRuleDao.class
    ));
    components.addAll(CorePropertyDefinitions.all());
    components.addAll(DatabaseMigrations.CLASSES);
    components.addAll(DaoUtils.getDaoClasses());
    return components;
  }

  /**
   * The stuff required to display the db upgrade form in webapp.
   * Needs to be connected to db.
   */
  Collection level2Components() {
    return Lists.newArrayList(
      DefaultServerUpgradeStatus.class,
      DatabaseMigrator.class,

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
      GwtI18n.class,
      Durations.class,

      // ws
      RestartHandler.class,
      SystemWs.class,
      BatchWs.class
    );
  }

  /**
   * The core components that complete the initialization of database
   * when its schema is up-to-date.
   */
  Collection level3Components() {
    List components = Lists.newArrayList(
      PersistentSettings.class,
      DefaultDatabaseConnector.class,
      ThreadLocalDatabaseSessionFactory.class,
      new DatabaseSessionProvider(),
      ServerMetadataPersister.class,
      ESNode.class,
      HttpDownloader.class,
      UriReader.class,
      ServerIdGenerator.class
    );
    components.addAll(IndexUtils.getIndexClasses());
    return components;
  }


  void startLevel4Components(ComponentContainer pico) {
    pico.addSingleton(ESIndex.class);
    pico.addSingleton(PluginDownloader.class);
    pico.addSingleton(ChartFactory.class);
    pico.addSingleton(Languages.class);
    pico.addSingleton(Views.class);
    pico.addSingleton(CodeColorizers.class);
    pico.addSingleton(ResourceTypes.class);
    pico.addSingleton(SettingsChangeNotifier.class);
    pico.addSingleton(PageDecorations.class);
    pico.addSingleton(PreviewCache.class);
    pico.addSingleton(DefaultResourcePermissions.class);
    pico.addSingleton(Periods.class);
    pico.addSingleton(ServerWs.class);

    // update center
    pico.addSingleton(UpdateCenterClient.class);
    pico.addSingleton(UpdateCenterMatrixFactory.class);
    pico.addSingleton(UpdateCenterWs.class);

    // quality profile
    pico.addSingleton(XMLProfileParser.class);
    pico.addSingleton(XMLProfileSerializer.class);
    pico.addComponent(ProfilesDao.class, false);
    pico.addComponent(ProfilesManager.class, false);
    pico.addSingleton(AnnotationProfileParser.class);
    pico.addSingleton(QProfileRuleLookup.class);
    pico.addSingleton(QProfiles.class);
    pico.addSingleton(QProfileLookup.class);
    pico.addSingleton(QProfileOperations.class);
    pico.addSingleton(QProfileActiveRuleOperations.class);
    pico.addSingleton(QProfileProjectOperations.class);
    pico.addSingleton(QProfileProjectLookup.class);
    pico.addSingleton(QProfileBackup.class);
    pico.addSingleton(QProfileRepositoryExporter.class);
    pico.addSingleton(DefaultProfilesCache.class);
    pico.addSingleton(ESActiveRule.class);
    pico.addSingleton(QProfileRestoreDefaultAction.class);
    pico.addSingleton(ProfilesWs.class);

    // rule
    pico.addSingleton(AnnotationRuleParser.class);
    pico.addSingleton(XMLRuleParser.class);
    pico.addComponent(RulesDao.class, false);
    pico.addSingleton(DefaultRuleFinder.class);
    pico.addSingleton(Rules.class);
    pico.addSingleton(RuleOperations.class);
    pico.addSingleton(RuleRegistry.class);
    pico.addSingleton(RubyRuleService.class);
    pico.addSingleton(RuleRepositories.class);
    pico.addSingleton(DeprecatedRulesDefinition.class);
    pico.addSingleton(RuleDefinitionsLoader.class);
    pico.addSingleton(RulesWs.class);
    pico.addSingleton(RuleShowWsHandler.class);
    pico.addSingleton(RuleSearchWsHandler.class);
    pico.addSingleton(AddTagsWsHandler.class);
    pico.addSingleton(RemoveTagsWsHandler.class);
    pico.addSingleton(RulesDefinitionXmlLoader.class);

    // experimental rules
    pico.addSingleton(RuleService.class);
    pico.addSingleton(RulesWebService.class);
    pico.addSingleton(SearchAction.class);
    pico.addSingleton(org.sonar.server.rule2.ws.ShowAction.class);
    pico.addSingleton(TagsAction.class);

    // rule tags
    pico.addSingleton(ESRuleTags.class);
    pico.addSingleton(RuleTagLookup.class);
    pico.addSingleton(RuleTagOperations.class);
    pico.addSingleton(RuleTags.class);
    pico.addSingleton(RuleTagsWs.class);

    // measure
    pico.addComponent(MeasuresDao.class, false);
    pico.addSingleton(MeasureFilterFactory.class);
    pico.addSingleton(MeasureFilterExecutor.class);
    pico.addSingleton(MeasureFilterEngine.class);
    pico.addSingleton(DefaultMetricFinder.class);
    pico.addSingleton(ServerLifecycleNotifier.class);
    pico.addSingleton(TimeMachineWs.class);

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

    // authentication
    pico.addSingleton(AuthenticationWs.class);

    // users
    pico.addSingleton(SecurityRealmFactory.class);
    pico.addSingleton(HibernateUserFinder.class);
    pico.addSingleton(NewUserNotifier.class);
    pico.addSingleton(DefaultUserFinder.class);
    pico.addSingleton(DefaultUserService.class);
    pico.addSingleton(UsersWs.class);

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
    pico.addSingleton(ResourcesWs.class);
    pico.addSingleton(ComponentsWs.class);
    pico.addSingleton(ProjectsWs.class);

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
    pico.addSingleton(IssueChangelogService.class);
    pico.addSingleton(IssueNotifications.class);
    pico.addSingleton(ActionService.class);
    pico.addSingleton(Actions.class);
    pico.addSingleton(IssueBulkChangeService.class);
    pico.addSingleton(IssueChangelogFormatter.class);
    pico.addSingleton(IssueShowAction.class);
    pico.addSingleton(IssuesWs.class);

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
    pico.addSingleton(DeprecatedSourceDecorator.class);
    pico.addSingleton(SourceService.class);
    pico.addSingleton(SourcesWs.class);
    pico.addSingleton(ShowAction.class);
    pico.addSingleton(ScmWriter.class);
    pico.addSingleton(ScmAction.class);

    // text
    pico.addSingleton(MacroInterpreter.class);
    pico.addSingleton(RubyTextService.class);

    // Notifications
    pico.addSingleton(EmailSettings.class);
    pico.addSingleton(NotificationService.class);
    pico.addSingleton(NotificationCenter.class);
    pico.addSingleton(DefaultNotificationManager.class);

    // Tests
    pico.addSingleton(TestsWs.class);

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

    for (Object components : level4AddedComponents) {
      pico.addSingleton(components);
    }


    ServerExtensionInstaller extensionInstaller = pico.getComponentByType(ServerExtensionInstaller.class);
    extensionInstaller.installExtensions(pico);

    pico.startComponents();
    executeStartupTaks(pico);
  }

  void addComponents(Collection components) {
    this.level4AddedComponents.addAll(components);
  }

  private void executeStartupTaks(ComponentContainer pico) {
    final ComponentContainer startupContainer = pico.createChild();
    startupContainer.addSingleton(GwtPublisher.class);
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
    startupContainer.addSingleton(CleanPreviewAnalysisCache.class);
    startupContainer.addSingleton(CopyRequirementsFromCharacteristicsToRules.class);

    DoPrivileged.execute(new DoPrivileged.Task() {
      @Override
      protected void doPrivileged() {
        startupContainer.startComponents();
        startupContainer.getComponentByType(ServerLifecycleNotifier.class).notifyStart();
      }
    });

    // Do not put the following statements in a finally block.
    // It would hide the possible exception raised during startup
    // See SONAR-3107
    startupContainer.stopComponents();

    pico.getComponentByType(DatabaseSessionFactory.class).clear();
  }
}
