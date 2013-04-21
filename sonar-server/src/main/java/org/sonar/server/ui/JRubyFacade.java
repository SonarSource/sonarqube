/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import com.google.common.collect.ListMultimap;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.License;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.NewUserHandler;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.MutableTestable;
import org.sonar.api.test.TestPlan;
import org.sonar.api.test.Testable;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.web.*;
import org.sonar.api.workflow.Review;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;
import org.sonar.api.workflow.screen.Screen;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.measure.MeasureFilterEngine;
import org.sonar.core.measure.MeasureFilterResult;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseMigrator;
import org.sonar.core.persistence.DryRunDatabaseFactory;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.core.source.HtmlSourceDecorator;
import org.sonar.core.timemachine.Periods;
import org.sonar.core.workflow.WorkflowEngine;
import org.sonar.markdown.Markdown;
import org.sonar.server.configuration.Backup;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.macro.MacroInterpreter;
import org.sonar.server.notifications.reviews.ReviewsNotificationManager;
import org.sonar.server.platform.*;
import org.sonar.server.plugins.*;
import org.sonar.server.rules.ProfilesConsole;
import org.sonar.server.rules.RulesConsole;
import org.sonar.updatecenter.common.PluginReferential;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import javax.annotation.Nullable;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public final class JRubyFacade {

  private static final JRubyFacade SINGLETON = new JRubyFacade();
  private JRubyI18n i18n;

  public static JRubyFacade getInstance() {
    return SINGLETON;
  }

  public static String markdownToHtml(String input) {
    return Markdown.convertToHtml(input);
  }

  private <T> T get(Class<T> componentType) {
    return getContainer().getComponentByType(componentType);
  }

  public MeasureFilterResult executeMeasureFilter(Map<String, Object> map, @Nullable Long userId) {
    return get(MeasureFilterEngine.class).execute(map, userId);
  }

  public Collection<ResourceType> getResourceTypesForFilter() {
    return get(ResourceTypes.class).getAll(ResourceTypes.AVAILABLE_FOR_FILTERS);
  }

  public Collection<ResourceType> getResourceTypes() {
    return get(ResourceTypes.class).getAll();
  }

  public Collection<ResourceType> getResourceRootTypes() {
    return get(ResourceTypes.class).getRoots();
  }

  public ResourceType getResourceType(String qualifier) {
    return get(ResourceTypes.class).get(qualifier);
  }

  public String getResourceTypeStringProperty(String resourceTypeQualifier, String resourceTypeProperty) {
    ResourceType resourceType = getResourceType(resourceTypeQualifier);
    if (resourceType != null) {
      return resourceType.getStringProperty(resourceTypeProperty);
    }
    return null;
  }

  public List<String> getQualifiersWithProperty(final String propertyKey) {
    List<String> qualifiers = newArrayList();
    for (ResourceType type : getResourceTypes()) {
      if (type.getBooleanProperty(propertyKey) == Boolean.TRUE) {
        qualifiers.add(type.getQualifier());
      }
    }
    return qualifiers;
  }

  public Boolean getResourceTypeBooleanProperty(String resourceTypeQualifier, String resourceTypeProperty) {
    ResourceType resourceType = getResourceType(resourceTypeQualifier);
    if (resourceType != null) {
      return resourceType.getBooleanProperty(resourceTypeProperty);
    }
    return null;
  }

  public Collection<String> getResourceLeavesQualifiers(String qualifier) {
    return get(ResourceTypes.class).getLeavesQualifiers(qualifier);
  }

  public Collection<String> getResourceChildrenQualifiers(String qualifier) {
    return get(ResourceTypes.class).getChildrenQualifiers(qualifier);
  }

  // UPDATE CENTER ------------------------------------------------------------
  public void downloadPlugin(String pluginKey, String pluginVersion) {
    get(PluginDownloader.class).download(pluginKey, Version.create(pluginVersion));
  }

  public void cancelPluginDownloads() {
    get(PluginDownloader.class).cancelDownloads();
  }

  public List<String> getPluginDownloads() {
    return get(PluginDownloader.class).getDownloads();
  }

  public void uninstallPlugin(String pluginKey) {
    get(PluginDeployer.class).uninstall(pluginKey);
  }

  public void cancelPluginUninstalls() {
    get(PluginDeployer.class).cancelUninstalls();
  }

  public List<String> getPluginUninstalls() {
    return get(PluginDeployer.class).getUninstalls();
  }

  public UpdateCenter getUpdatePluginCenter(boolean forceReload) {
    return get(UpdateCenterMatrixFactory.class).getUpdateCenter(forceReload);
  }

  public PluginReferential getInstalledPluginReferential() {
    return get(InstalledPluginReferentialFactory.class).getInstalledPluginReferential();
  }

  // PLUGINS ------------------------------------------------------------------
  public PropertyDefinitions getPropertyDefinitions() {
    return get(PropertyDefinitions.class);
  }

  public boolean hasPlugin(String key) {
    return get(PluginRepository.class).getPlugin(key) != null;
  }

  public Collection<PluginMetadata> getPluginsMetadata() {
    return get(PluginRepository.class).getMetadata();
  }

  // SYNTAX HIGHLIGHTING ------------------------------------------------------
  public String colorizeCode(String code, String language) {
    try {
      return get(CodeColorizers.class).toHtml(code, language);
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Can not highlight the code, language= " + language, e);
      return code;
    }
  }

  public List<ViewProxy<Widget>> getWidgets(String resourceScope, String resourceQualifier, String resourceLanguage, Object[] availableMeasures) {
    return get(Views.class).getWidgets(resourceScope, resourceQualifier, resourceLanguage, (String[]) availableMeasures);
  }

  public List<ViewProxy<Widget>> getWidgets() {
    return get(Views.class).getWidgets();
  }

  public ViewProxy<Widget> getWidget(String id) {
    return get(Views.class).getWidget(id);
  }

  public List<ViewProxy<Page>> getPages(String section, String resourceScope, String resourceQualifier, String resourceLanguage, Object[] availableMeasures) {
    return get(Views.class).getPages(section, resourceScope, resourceQualifier, resourceLanguage, (String[]) availableMeasures);
  }

  public List<ViewProxy<Page>> getResourceTabs() {
    return get(Views.class).getPages(NavigationSection.RESOURCE_TAB, null, null, null, null);
  }

  public List<ViewProxy<Page>> getResourceTabs(String scope, String qualifier, String language, Object[] availableMeasures) {
    return get(Views.class).getPages(NavigationSection.RESOURCE_TAB, scope, qualifier, language, (String[]) availableMeasures);
  }

  public List<ViewProxy<Page>> getResourceTabsForMetric(String scope, String qualifier, String language, Object[] availableMeasures, String metric) {
    return get(Views.class).getPagesForMetric(NavigationSection.RESOURCE_TAB, scope, qualifier, language, (String[]) availableMeasures, metric);
  }

  public ViewProxy<Page> getPage(String id) {
    return get(Views.class).getPage(id);
  }

  public Collection<RubyRailsWebservice> getRubyRailsWebservices() {
    return getContainer().getComponentsByType(RubyRailsWebservice.class);
  }

  public Collection<Language> getLanguages() {
    return getContainer().getComponentsByType(Language.class);
  }

  public Database getDatabase() {
    return get(Database.class);
  }

  public boolean createDatabase() {
    return get(DatabaseMigrator.class).createDatabase();
  }

  /* PROFILES CONSOLE : RULES AND METRIC THRESHOLDS */
  public List<RuleRepository> getRuleRepositories() {
    return get(RulesConsole.class).getRepositories();
  }

  public RuleRepository getRuleRepository(String repositoryKey) {
    return get(RulesConsole.class).getRepository(repositoryKey);
  }

  public Set<RuleRepository> getRuleRepositoriesByLanguage(String languageKey) {
    return get(RulesConsole.class).getRepositoriesByLanguage(languageKey);
  }

  public String backupProfile(int profileId) {
    return get(ProfilesConsole.class).backupProfile(profileId);
  }

  public ValidationMessages restoreProfile(String xmlBackup, boolean deleteExisting) {
    return get(ProfilesConsole.class).restoreProfile(xmlBackup, deleteExisting);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    return get(ProfilesConsole.class).getProfileExportersForLanguage(language);
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    return get(ProfilesConsole.class).getProfileImportersForLanguage(language);
  }

  /**
   * @throws IllegalArgumentException if no such exporter
   */
  public String exportProfile(int profileId, String exporterKey) {
    return get(ProfilesConsole.class).exportProfile(profileId, exporterKey);
  }

  public ValidationMessages importProfile(String profileName, String language, String importerKey, String fileContent) {
    return get(ProfilesConsole.class).importProfile(profileName, language, importerKey, fileContent);
  }

  public String getProfileExporterMimeType(String exporterKey) {
    return get(ProfilesConsole.class).getProfileExporter(exporterKey).getMimeType();
  }

  public void copyProfile(long profileId, String newProfileName) {
    getProfilesManager().copyProfile((int) profileId, newProfileName);
  }

  public ValidationMessages changeParentProfile(int profileId, String parentName, String userName) {
    return getProfilesManager().changeParentProfile(profileId, parentName, userName);
  }

  public void ruleActivated(int parentProfileId, int activeRuleId, String userName) {
    getProfilesManager().activated(parentProfileId, activeRuleId, userName);
  }

  public void ruleParamChanged(int parentProfileId, int activeRuleId, String paramKey, String oldValue, String newValue, String userName) {
    getProfilesManager().ruleParamChanged(parentProfileId, activeRuleId, paramKey, oldValue, newValue, userName);
  }

  public void ruleSeverityChanged(int parentProfileId, int activeRuleId, int oldSeverityId, int newSeverityId, String userName) {
    getProfilesManager().ruleSeverityChanged(parentProfileId, activeRuleId, RulePriority.values()[oldSeverityId],
        RulePriority.values()[newSeverityId], userName);
  }

  public void ruleDeactivated(int parentProfileId, int deactivatedRuleId, String userName) {
    getProfilesManager().deactivated(parentProfileId, deactivatedRuleId, userName);
  }

  public void revertRule(int profileId, int activeRuleId, String userName) {
    getProfilesManager().revert(profileId, activeRuleId, userName);
  }

  public List<Footer> getWebFooters() {
    return getContainer().getComponentsByType(Footer.class);
  }

  public Backup getBackup() {
    return get(Backup.class);
  }

  private ProfilesManager getProfilesManager() {
    return get(ProfilesManager.class);
  }

  public void setGlobalProperty(String key, @Nullable String value) {
    get(ServerSettings.class).setProperty(key, value);
    get(SettingsChangeNotifier.class).onGlobalPropertyChange(key, value);
  }

  public Settings getSettings() {
    return get(Settings.class);
  }

  public String getConfigurationValue(String key) {
    return get(Settings.class).getString(key);
  }

  public List<InetAddress> getValidInetAddressesForServerId() {
    return get(ServerIdGenerator.class).getAvailableAddresses();
  }

  public String generateServerId(String organisation, String ipAddress) {
    return get(ServerIdGenerator.class).generate(organisation, ipAddress);
  }

  public Connection getConnection() {
    try {
      return get(Database.class).getDataSource().getConnection();
    } catch (Exception e) {
      /* activerecord does not correctly manage exceptions when connection can not be opened. */
      return null;
    }
  }

  public Object getCoreComponentByClassname(String className) {
    if (className == null) {
      return null;
    }

    try {
      return get(Class.forName(className));
    } catch (ClassNotFoundException e) {
      LoggerFactory.getLogger(getClass()).error("Component not found: " + className, e);
      return null;
    }
  }

  public Object getComponentByClassname(String pluginKey, String className) {
    Object component = null;
    Class<?> componentClass = get(DefaultServerPluginRepository.class).getClass(pluginKey, className);
    if (componentClass != null) {
      component = get(componentClass);
    }
    return component;
  }

  private JRubyI18n getJRubyI18n() {
    if (i18n == null) {
      i18n = get(JRubyI18n.class);
    }
    return i18n;
  }

  public String getMessage(String rubyLocale, String key, String defaultValue, Object... parameters) {
    return getJRubyI18n().message(rubyLocale, key, defaultValue, parameters);
  }

  public String getRuleName(String rubyLocale, String repositoryKey, String key) {
    return getJRubyI18n().getRuleName(rubyLocale, repositoryKey, key);
  }

  public String getRuleDescription(String rubyLocale, String repositoryKey, String key) {
    return getJRubyI18n().getRuleDescription(rubyLocale, repositoryKey, key);
  }

  public String getRuleParamDescription(String rubyLocale, String repositoryKey, String key, String paramKey) {
    return getJRubyI18n().getRuleParamDescription(rubyLocale, repositoryKey, key, paramKey);
  }

  public List<RuleI18nManager.RuleKey> searchRuleName(String rubyLocale, String searchText) {
    return getJRubyI18n().searchRuleName(rubyLocale, searchText);
  }

  public String getJsL10nDictionnary(String rubyLocale) {
    return getJRubyI18n().getJsDictionnary(rubyLocale);
  }

  public void indexProjects() {
    get(ResourceIndexerDao.class).indexProjects();
  }

  public void indexResource(long resourceId) {
    get(ResourceIndexerDao.class).indexResource(resourceId);
  }

  public void deleteResourceTree(long rootProjectId) {
    try {
      get(PurgeDao.class).deleteResourceTree(rootProjectId);
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(JRubyFacade.class).error("Fail to delete resource with ID: " + rootProjectId, e);
      throw e;
    }
  }

  public void logError(String message) {
    LoggerFactory.getLogger(getClass()).error(message);
  }

  public boolean hasSecretKey() {
    return get(Settings.class).getEncryption().hasSecretKey();
  }

  public String encrypt(String clearText) {
    return get(Settings.class).getEncryption().encrypt(clearText);
  }

  public String generateRandomSecretKey() {
    return get(Settings.class).getEncryption().generateRandomSecretKey();
  }

  public License parseLicense(String base64) {
    return License.readBase64(base64);
  }

  public String getServerHome() {
    return get(Settings.class).getString(CoreProperties.SONAR_HOME);
  }

  public ReviewsNotificationManager getReviewsNotificationManager() {
    return get(ReviewsNotificationManager.class);
  }

  public ComponentContainer getContainer() {
    return Platform.getInstance().getContainer();
  }

  // REVIEWS ------------------------------------------------------------------
  public List<Screen> listAvailableReviewScreens(Review review, DefaultWorkflowContext context) {
    return get(WorkflowEngine.class).listAvailableScreens(review, context, true);
  }

  public ListMultimap<Long, Screen> listAvailableReviewsScreens(DefaultReview[] reviews, DefaultWorkflowContext context) {
    return get(WorkflowEngine.class).listAvailableScreens(reviews, context, true);
  }

  public Screen getReviewScreen(String commandKey) {
    return get(WorkflowEngine.class).getScreen(commandKey);
  }

  public void executeReviewCommand(String commandKey, DefaultReview review, DefaultWorkflowContext context, Map<String, String> parameters) {
    try {
      get(WorkflowEngine.class).execute(commandKey, review, context, parameters);
    } catch (RuntimeException e) {
      LoggerFactory.getLogger(JRubyFacade.class).error("Fail to execute command: " + commandKey + " on review " + review.getReviewId(), e);
      throw e;
    }
  }

  // UPDATE PROJECT KEY ------------------------------------------------------------------
  public void updateResourceKey(long projectId, String newKey) {
    get(ResourceKeyUpdaterDao.class).updateKey(projectId, newKey);
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(long projectId, String stringToReplace, String replacementString) {
    return get(ResourceKeyUpdaterDao.class).checkModuleKeysBeforeRenaming(projectId, stringToReplace, replacementString);
  }

  public void bulkUpdateKey(long projectId, String stringToReplace, String replacementString) {
    get(ResourceKeyUpdaterDao.class).bulkUpdateKey(projectId, stringToReplace, replacementString);
  }

  // USERS
  public void onNewUser(Map<String, String> fields) {
    NewUserNotifier notifier = get(NewUserNotifier.class);
    // notifier is null when creating the administrator in the migration script 011.
    if (notifier != null) {
      notifier.onNewUser(NewUserHandler.Context.builder()
          .setLogin(fields.get("login"))
          .setName(fields.get("name"))
          .setEmail(fields.get("email"))
          .build());
    }
  }

  public byte[] createDatabaseForDryRun(@Nullable Long projectId) {
    return get(DryRunDatabaseFactory.class).createDatabaseForDryRun(projectId);
  }

  public String getPeriodLabel(int periodIndex) {
    return get(Periods.class).label(periodIndex);
  }

  public String getPeriodLabel(String mode, String param, Date date) {
    return get(Periods.class).label(mode, param, date);
  }

  public String getPeriodLabel(String mode, String param, String date) {
    return get(Periods.class).label(mode, param, date);
  }

  public String getPeriodAbbreviation(int periodIndex) {
    return get(Periods.class).abbreviation(periodIndex);
  }

  public TestPlan testPlan(long snapshotId) {
    return get(SnapshotPerspectives.class).as(MutableTestPlan.class, snapshotId);
  }

  public TestPlan testPlan(String componentKey) {
    return get(SnapshotPerspectives.class).as(MutableTestPlan.class, componentKey);
  }

  public Testable testable(long snapshotId) {
    return get(SnapshotPerspectives.class).as(MutableTestable.class, snapshotId);
  }

  public Testable testable(String componentKey) {
    return get(SnapshotPerspectives.class).as(MutableTestable.class, componentKey);
  }

  public MacroInterpreter getMacroInterpreter(){
    return get(MacroInterpreter.class);
  }

  public Collection<String> getHighlightedSourceLines(long snapshotId) {
    return get(HtmlSourceDecorator.class).getDecoratedSourceAsHtml(snapshotId);
  }
}
