/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.picocontainer.PicoContainer;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.api.Property;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Language;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.web.*;
import org.sonar.jpa.dao.AsyncMeasuresService;
import org.sonar.jpa.dialect.Dialect;
import org.sonar.jpa.session.DatabaseConnector;
import org.sonar.server.configuration.Backup;
import org.sonar.server.configuration.CoreConfiguration;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.filters.Filter;
import org.sonar.server.filters.FilterExecutor;
import org.sonar.server.filters.FilterResult;
import org.sonar.server.platform.Platform;
import org.sonar.server.plugins.PluginClassLoaders;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateFinder;
import org.sonar.server.plugins.UpdateFinderFactory;
import org.sonar.server.rules.ProfilesConsole;
import org.sonar.server.rules.RulesConsole;
import org.sonar.updatecenter.common.Version;

import java.util.Collection;
import java.util.List;

public final class JRubyFacade implements ServerComponent {

  public FilterResult executeFilter(Filter filter) {
    return getContainer().getComponent(FilterExecutor.class).execute(filter);
  }

  /* PLUGINS */
  public void downloadPlugin(String pluginKey, String pluginVersion) {
    getContainer().getComponent(PluginDownloader.class).download(pluginKey, Version.create(pluginVersion));
  }

  public void cancelPluginDownloads() {
    getContainer().getComponent(PluginDownloader.class).cancelDownloads();
  }

  public List<String> getPluginDownloads() {
    return getContainer().getComponent(PluginDownloader.class).getDownloads();
  }

  public UpdateFinder getUpdateFinder(boolean forceReload) {
    return getContainer().getComponent(UpdateFinderFactory.class).getFinder(forceReload);
  }

  public String colorizeCode(String code, String language) {
    try {
      return getContainer().getComponent(CodeColorizers.class).toHtml(code, language);

    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Can not highlight the code, language= " + language, e);
      return code;
    }
  }


  public List<ViewProxy<Widget>> getWidgets(String resourceScope, String resourceQualifier, String resourceLanguage) {
    return getContainer().getComponent(Views.class).getWidgets(resourceScope, resourceQualifier, resourceLanguage);
  }

  public List<ViewProxy<Page>> getPages(String section, String resourceScope, String resourceQualifier, String resourceLanguage) {
    return getContainer().getComponent(Views.class).getPages(section, resourceScope, resourceQualifier, resourceLanguage);
  }

  public List<ViewProxy<Page>> getResourceTabs() {
    return getContainer().getComponent(Views.class).getPages(NavigationSection.RESOURCE_TAB, null, null, null);
  }

  public ViewProxy<Page> getPage(String id) {
    return getContainer().getComponent(Views.class).getPage(id);
  }

  public Collection<RubyRailsWebservice> getRubyRailsWebservices() {
    return getContainer().getComponents(RubyRailsWebservice.class);
  }

  public Collection<Language> getLanguages() {
    return getContainer().getComponents(Language.class);
  }

  public Dialect getDialect() {
    return getContainer().getComponent(DatabaseConnector.class).getDialect();
  }

  public boolean hasPlugin(String key) {
    return getContainer().getComponent(Plugins.class).getPlugin(key) != null;
  }

  public Collection<Plugin> getPlugins() {
    return getContainer().getComponent(Plugins.class).getPlugins();
  }


  /* PROFILES CONSOLE : RULES AND METRIC THRESHOLDS */

  public List<RuleRepository> getRuleRepositories() {
    return getContainer().getComponent(RulesConsole.class).getRepositories();
  }

  public RuleRepository getRuleRepository(String repositoryKey) {
    return getContainer().getComponent(RulesConsole.class).getRepository(repositoryKey);
  }

  public List<RuleRepository> getRuleRepositoriesByLanguage(String languageKey) {
    return getContainer().getComponent(RulesConsole.class).getRepositoriesByLanguage(languageKey);
  }

  public String backupProfile(int profileId) {
    return getContainer().getComponent(ProfilesConsole.class).backupProfile(profileId);
  }

  public ValidationMessages restoreProfile(String xmlBackup) {
    return getContainer().getComponent(ProfilesConsole.class).restoreProfile(xmlBackup);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    return getContainer().getComponent(ProfilesConsole.class).getProfileExportersForLanguage(language);
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    return getContainer().getComponent(ProfilesConsole.class).getProfileImportersForLanguage(language);
  }

  public String exportProfile(int profileId, String exporterKey) {
    return getContainer().getComponent(ProfilesConsole.class).exportProfile(profileId, exporterKey);
  }

  public ValidationMessages importProfile(String profileName, String language, String importerKey, String fileContent) {
    return getContainer().getComponent(ProfilesConsole.class).importProfile(profileName, language, importerKey, fileContent);
  }

  public String getProfileExporterMimeType(String exporterKey) {
    return getContainer().getComponent(ProfilesConsole.class).getProfileExporter(exporterKey).getMimeType();
  }

  public void copyProfile(long profileId, String newProfileName) {
    getProfilesManager().copyProfile((int) profileId, newProfileName);
  }

  public void deleteProfile(long profileId) {
    getProfilesManager().deleteProfile((int) profileId);
  }

  public List<Footer> getWebFooters() {
    return getContainer().getComponents(Footer.class);
  }

  public Backup getBackup() {
    return getContainer().getComponent(Backup.class);
  }

  public void registerAsyncMeasure(long asyncMeasureId) {
    getAsyncMeasuresService().registerMeasure(asyncMeasureId);
  }

  public void deleteAsyncMeasure(long asyncMeasureId) {
    getAsyncMeasuresService().deleteMeasure(asyncMeasureId);
  }

  public Property[] getPluginProperties(Plugin plugin) {
    return getContainer().getComponent(Plugins.class).getProperties(plugin);
  }

  private DefaultRulesManager getRulesManager() {
    return getContainer().getComponent(DefaultRulesManager.class);
  }

  private ProfilesManager getProfilesManager() {
    return getContainer().getComponent(ProfilesManager.class);
  }

  private AsyncMeasuresService getAsyncMeasuresService() {
    return getContainer().getComponent(AsyncMeasuresService.class);
  }

  public void reloadConfiguration() {
    getContainer().getComponent(CoreConfiguration.class).reload();
  }

  public String getConfigurationValue(String key) {
    return getContainer().getComponent(Configuration.class).getString(key, null);
  }

  public Object getCoreComponentByClassname(String className) {
    if (className == null) {
      return null;
    }

    try {
      Class aClass = Class.forName(className);
      return getContainer().getComponent(aClass);

    } catch (ClassNotFoundException e) {
      LoggerFactory.getLogger(getClass()).error("Component not found: " + className, e);
      return null;
    }
  }

  public Object getComponentByClassname(String pluginKey, String className) {
    Object component = null;
    PicoContainer container = getContainer();
    Class componentClass = container.getComponent(PluginClassLoaders.class).getClass(pluginKey, className);
    if (componentClass!=null) {
      component = container.getComponent(componentClass);
    }
    return component;
  }

  public PicoContainer getContainer() {
    return Platform.getInstance().getContainer();
  }
}
