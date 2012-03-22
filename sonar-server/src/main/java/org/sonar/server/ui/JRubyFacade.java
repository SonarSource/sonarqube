/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.ui;

import org.apache.commons.configuration.Configuration;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.License;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.web.*;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseMigrator;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.markdown.Markdown;
import org.sonar.server.configuration.Backup;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.filters.Filter;
import org.sonar.server.filters.FilterExecutor;
import org.sonar.server.filters.FilterResult;
import org.sonar.server.notifications.reviews.ReviewsNotificationManager;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.platform.ServerSettings;
import org.sonar.server.plugins.*;
import org.sonar.server.rules.ProfilesConsole;
import org.sonar.server.rules.RulesConsole;
import org.sonar.updatecenter.common.Version;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JRubyFacade {

  private static final JRubyFacade SINGLETON = new JRubyFacade();
  private JRubyI18n i18n;

  public static JRubyFacade getInstance() {
    return SINGLETON;
  }

  public FilterResult executeFilter(Filter filter) {
    return getContainer().getComponentByType(FilterExecutor.class).execute(filter);
  }

  public Collection<ResourceType> getResourceTypesForFilter() {
    return getContainer().getComponentByType(ResourceTypes.class).getAll(ResourceTypes.AVAILABLE_FOR_FILTERS);
  }

  public ResourceType getResourceType(String qualifier) {
    return getContainer().getComponentByType(ResourceTypes.class).get(qualifier);
  }

  public Collection<String> getResourceLeavesQualifiers(String qualifier) {
    return getContainer().getComponentByType(ResourceTypes.class).getLeavesQualifiers(qualifier);
  }

  public Collection<String> getResourceChildrenQualifiers(String qualifier) {
    return getContainer().getComponentByType(ResourceTypes.class).getChildrenQualifiers(qualifier);
  }

  // UPDATE CENTER ------------------------------------------------------------

  public void downloadPlugin(String pluginKey, String pluginVersion) {
    getContainer().getComponentByType(PluginDownloader.class).download(pluginKey, Version.create(pluginVersion));
  }

  public void cancelPluginDownloads() {
    getContainer().getComponentByType(PluginDownloader.class).cancelDownloads();
  }

  public List<String> getPluginDownloads() {
    return getContainer().getComponentByType(PluginDownloader.class).getDownloads();
  }

  public void uninstallPlugin(String pluginKey) {
    getContainer().getComponentByType(PluginDeployer.class).uninstall(pluginKey);
  }

  public void cancelPluginUninstalls() {
    getContainer().getComponentByType(PluginDeployer.class).cancelUninstalls();
  }

  public List<String> getPluginUninstalls() {
    return getContainer().getComponentByType(PluginDeployer.class).getUninstalls();
  }

  public UpdateCenterMatrix getUpdateCenterMatrix(boolean forceReload) {
    return getContainer().getComponentByType(UpdateCenterMatrixFactory.class).getMatrix(forceReload);
  }

  // PLUGINS ------------------------------------------------------------------

  public PropertyDefinitions getPropertyDefinitions() {
    return getContainer().getComponentByType(PropertyDefinitions.class);
  }

  public boolean hasPlugin(String key) {
    return getContainer().getComponentByType(PluginRepository.class).getPlugin(key) != null;
  }

  public Collection<PluginMetadata> getPluginsMetadata() {
    return getContainer().getComponentByType(PluginRepository.class).getMetadata();
  }


  // SYNTAX HIGHLIGHTING ------------------------------------------------------

  public String colorizeCode(String code, String language) {
    try {
      return getContainer().getComponentByType(CodeColorizers.class).toHtml(code, language);

    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Can not highlight the code, language= " + language, e);
      return code;
    }
  }

  public static String markdownToHtml(String input) {
    return Markdown.convertToHtml(input);
  }


  public List<ViewProxy<Widget>> getWidgets(String resourceScope, String resourceQualifier, String resourceLanguage) {
    return getContainer().getComponentByType(Views.class).getWidgets(resourceScope, resourceQualifier, resourceLanguage);
  }

  public List<ViewProxy<Widget>> getWidgets() {
    return getContainer().getComponentByType(Views.class).getWidgets();
  }

  public ViewProxy<Widget> getWidget(String id) {
    return getContainer().getComponentByType(Views.class).getWidget(id);
  }

  public List<ViewProxy<Page>> getPages(String section, String resourceScope, String resourceQualifier, String resourceLanguage) {
    return getContainer().getComponentByType(Views.class).getPages(section, resourceScope, resourceQualifier, resourceLanguage);
  }

  public List<ViewProxy<Page>> getResourceTabs() {
    return getContainer().getComponentByType(Views.class).getPages(NavigationSection.RESOURCE_TAB, null, null, null);
  }

  public List<ViewProxy<Page>> getResourceTabs(String scope, String qualifier, String language) {
    return getContainer().getComponentByType(Views.class).getPages(NavigationSection.RESOURCE_TAB, scope, qualifier, language);
  }

  public List<ViewProxy<Page>> getResourceTabsForMetric(String scope, String qualifier, String language, String metric) {
    return getContainer().getComponentByType(Views.class).getPagesForMetric(NavigationSection.RESOURCE_TAB, scope, qualifier, language, metric);
  }

  public ViewProxy<Page> getPage(String id) {
    return getContainer().getComponentByType(Views.class).getPage(id);
  }

  public Collection<RubyRailsWebservice> getRubyRailsWebservices() {
    return getContainer().getComponentsByType(RubyRailsWebservice.class);
  }

  public Collection<Language> getLanguages() {
    return getContainer().getComponentsByType(Language.class);
  }

  public Database getDatabase() {
    return getContainer().getComponentByType(Database.class);
  }

  public boolean createDatabase() {
    return getContainer().getComponentByType(DatabaseMigrator.class).createDatabase();
  }

  /* PROFILES CONSOLE : RULES AND METRIC THRESHOLDS */

  public List<RuleRepository> getRuleRepositories() {
    return getContainer().getComponentByType(RulesConsole.class).getRepositories();
  }

  public RuleRepository getRuleRepository(String repositoryKey) {
    return getContainer().getComponentByType(RulesConsole.class).getRepository(repositoryKey);
  }

  public Set<RuleRepository> getRuleRepositoriesByLanguage(String languageKey) {
    return getContainer().getComponentByType(RulesConsole.class).getRepositoriesByLanguage(languageKey);
  }

  public String backupProfile(int profileId) {
    return getContainer().getComponentByType(ProfilesConsole.class).backupProfile(profileId);
  }

  public ValidationMessages restoreProfile(String xmlBackup) {
    return getContainer().getComponentByType(ProfilesConsole.class).restoreProfile(xmlBackup);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    return getContainer().getComponentByType(ProfilesConsole.class).getProfileExportersForLanguage(language);
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    return getContainer().getComponentByType(ProfilesConsole.class).getProfileImportersForLanguage(language);
  }

  public String exportProfile(int profileId, String exporterKey) {
    return getContainer().getComponentByType(ProfilesConsole.class).exportProfile(profileId, exporterKey);
  }

  public ValidationMessages importProfile(String profileName, String language, String importerKey, String fileContent) {
    return getContainer().getComponentByType(ProfilesConsole.class).importProfile(profileName, language, importerKey, fileContent);
  }

  public String getProfileExporterMimeType(String exporterKey) {
    return getContainer().getComponentByType(ProfilesConsole.class).getProfileExporter(exporterKey).getMimeType();
  }

  public void renameProfile(int profileId, String newProfileName) {
    getProfilesManager().renameProfile(profileId, newProfileName);
  }

  public void copyProfile(long profileId, String newProfileName) {
    getProfilesManager().copyProfile((int) profileId, newProfileName);
  }

  public void deleteProfile(long profileId) {
    getProfilesManager().deleteProfile((int) profileId);
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
    return getContainer().getComponentByType(Backup.class);
  }

  private ProfilesManager getProfilesManager() {
    return getContainer().getComponentByType(ProfilesManager.class);
  }

  public void reloadConfiguration() {
    getContainer().getComponentByType(ServerSettings.class).load();
  }

  public Settings getSettings() {
    return getContainer().getComponentByType(Settings.class);
  }

  public String getConfigurationValue(String key) {
    return getContainer().getComponentByType(Configuration.class).getString(key, null);
  }

  public List<InetAddress> getValidInetAddressesForServerId() {
    return getContainer().getComponentByType(ServerIdGenerator.class).getAvailableAddresses();
  }

  public String generateServerId(String organisation, String ipAddress) {
    return getContainer().getComponentByType(ServerIdGenerator.class).generate(organisation, ipAddress);
  }

  public Connection getConnection() {
    try {
      return getContainer().getComponentByType(Database.class).getDataSource().getConnection();
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
      Class aClass = Class.forName(className);
      return getContainer().getComponentByType(aClass);

    } catch (ClassNotFoundException e) {
      LoggerFactory.getLogger(getClass()).error("Component not found: " + className, e);
      return null;
    }
  }

  public Object getComponentByClassname(String pluginKey, String className) {
    Object component = null;
    ComponentContainer container = getContainer();
    Class componentClass = container.getComponentByType(DefaultServerPluginRepository.class).getClass(pluginKey, className);
    if (componentClass != null) {
      component = container.getComponentByType(componentClass);
    }
    return component;
  }

  public String getMessage(String rubyLocale, String key, String defaultValue, Object... parameters) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.message(rubyLocale, key, defaultValue, parameters);
  }

  public String getRuleName(String rubyLocale, String repositoryKey, String key) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.getRuleName(rubyLocale, repositoryKey, key);
  }

  public String getRuleDescription(String rubyLocale, String repositoryKey, String key) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.getRuleDescription(rubyLocale, repositoryKey, key);
  }

  public String getRuleParamDescription(String rubyLocale, String repositoryKey, String key, String paramKey) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.getRuleParamDescription(rubyLocale, repositoryKey, key, paramKey);
  }

  public List<RuleI18nManager.RuleKey> searchRuleName(String rubyLocale, String searchText) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.searchRuleName(rubyLocale, searchText);
  }

  public String getJsL10nDictionnary(String rubyLocale) {
    if (i18n == null) {
      i18n = getContainer().getComponentByType(JRubyI18n.class);
    }
    return i18n.getJsDictionnary(rubyLocale);
  }

  public void indexProjects() {
    getContainer().getComponentByType(ResourceIndexerDao.class).indexProjects();
  }

  public void deleteProject(long rootProjectId) {
    getContainer().getComponentByType(PurgeDao.class).deleteProject(rootProjectId);
  }

  public void logError(String message) {
    LoggerFactory.getLogger(getClass()).error(message);
  }

  public boolean hasSecretKey() {
    return getContainer().getComponentByType(Settings.class).getEncryption().hasSecretKey();
  }

  public String encrypt(String clearText) {
    return getContainer().getComponentByType(Settings.class).getEncryption().encrypt(clearText);
  }

  public String generateRandomSecretKey() {
    return getContainer().getComponentByType(Settings.class).getEncryption().generateRandomSecretKey();
  }
  
  public License parseLicense(String base64) {
    return License.readBase64(base64);
  }


  public ReviewsNotificationManager getReviewsNotificationManager() {
    return getContainer().getComponentByType(ReviewsNotificationManager.class);
  }

  public ComponentContainer getContainer() {
    return Platform.getInstance().getContainer();
  }


}
