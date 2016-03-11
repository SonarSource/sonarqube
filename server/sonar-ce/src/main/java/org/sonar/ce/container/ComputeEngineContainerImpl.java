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
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.UriReader;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.PluginClassloaderFactory;
import org.sonar.core.platform.PluginLoader;
import org.sonar.core.platform.SonarQubeVersionProvider;
import org.sonar.core.util.DefaultHttpDownloader;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DatabaseChecker;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.version.DatabaseVersion;
import org.sonar.db.version.MigrationStepModule;
import org.sonar.process.Props;
import org.sonar.server.computation.property.CePropertyDefinitions;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.DatabaseServerCompatibility;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.platform.ServerIdGenerator;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerSettings;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.plugins.InstalledPluginReferentialFactory;
import org.sonar.server.plugins.ServerExtensionInstaller;
import org.sonar.server.plugins.ServerPluginJarExploder;
import org.sonar.server.plugins.ServerPluginRepository;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.EsSearchModule;
import org.sonar.server.startup.ServerMetadataPersister;

public class ComputeEngineContainerImpl implements ComputeEngineContainer {
  private static final Object[] LEVEL_1_COMPONENTS = new Object[] {
    new SonarQubeVersionProvider(),
    ServerSettings.class,
    ServerImpl.class,
    UuidFactoryImpl.INSTANCE,
    // no EmbeddedDatabaseFactory.class, creating H2 DB if responsibility of WebServer
    DefaultDatabase.class,
    DatabaseChecker.class,
    // must instantiate deprecated class in 5.2 and only this one (and not its replacement)
    // to avoid having two SqlSessionFactory instances
    org.sonar.core.persistence.MyBatis.class,
    DatabaseServerCompatibility.class,
    DatabaseVersion.class,
    PurgeProfiler.class,
    DefaultServerFileSystem.class,
    // no TempFolderCleaner.class, responsibility of Web Server
    new TempFolderProvider(),
    System2.INSTANCE,

    // DB
    DbClient.class,
    DaoModule.class,
    MigrationStepModule.class,

    // Elasticsearch
    EsSearchModule.class,

    // rules/qprofiles
    RuleIndex.class,
    ActiveRuleIndex.class,

    // issues
    IssueIndex.class,

    // Classes kept for backward compatibility of plugins/libs (like sonar-license) that are directly calling classes from the core
    org.sonar.core.properties.PropertiesDao.class
  };
  private static final Object[] LEVEL_2_COMPONENTS = new Object[] {
    DefaultServerUpgradeStatus.class,
    // no DatabaseMigrator.class, responsibility of Web Server

    // plugins
    PluginClassloaderFactory.class,
    ServerPluginJarExploder.class,
    PluginLoader.class,
    ServerPluginRepository.class,
    InstalledPluginReferentialFactory.class,
    ServerExtensionInstaller.class,

    // depends on plugins
    // RailsAppsDeployer.class,
    // JRubyI18n.class,
    DefaultI18n.class, // used by RuleI18nManager
    RuleI18nManager.class, // used by DebtRulesXMLImporter
    Durations.class, // used in Web Services and DebtCalculator
  };
  private static final Object[] LEVEL_3_COMPONENTS = new Object[] {
    PersistentSettings.class,
    ServerMetadataPersister.class,
    DefaultHttpDownloader.class,
    UriReader.class,
    ServerIdGenerator.class
  };

  private final ComponentContainer componentContainer;

  public ComputeEngineContainerImpl() {
    this.componentContainer = new ComponentContainer();
  }

  @Override
  public ComputeEngineContainer configure(Props props) {
    this.componentContainer
      .add(props.rawProperties())
      .add(LEVEL_1_COMPONENTS)
      .add(toArray(CorePropertyDefinitions.all()))
      .add(toArray(CePropertyDefinitions.all()))
      .add(LEVEL_2_COMPONENTS)
      .add(LEVEL_3_COMPONENTS);

    configureFromModules();

    return this;
  }

  private static Object[] toArray(List<?> list) {
    return list.toArray(new Object[list.size()]);
  }

  private void configureFromModules() {
    List<Module> modules = this.componentContainer.getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(this.componentContainer);
    }
  }

  @Override
  public ComputeEngineContainer start() {
    this.componentContainer.startComponents();
    return this;
  }

  @Override
  public ComputeEngineContainer stop() {
    this.componentContainer.stopComponents();
    return this;
  }

  @VisibleForTesting
  protected ComponentContainer getComponentContainer() {
    return componentContainer;
  }
}
