/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.time.Clock;
import java.util.Properties;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.extension.CoreExtensionRepositoryImpl;
import org.sonar.core.extension.CoreExtensionsLoader;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.MyBatis;
import org.sonar.db.StartMyBatis;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.app.ProcessCommandWrapperImpl;
import org.sonar.server.app.RestartFlagHolderImpl;
import org.sonar.server.app.WebServerProcessLogging;
import org.sonar.server.config.ConfigurationProvider;
import org.sonar.server.es.EsModule;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.platform.DefaultNodeInformation;
import org.sonar.server.platform.ContainerSupportImpl;
import org.sonar.server.platform.LogServerVersion;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.ServerFileSystemImpl;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.platform.UrlSettings;
import org.sonar.server.platform.WebCoreExtensionsInstaller;
import org.sonar.server.platform.db.EmbeddedDatabaseFactory;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.setting.ThreadLocalSettings;
import org.sonar.server.user.SystemPasscodeImpl;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.util.GlobalLockManagerImpl;
import org.sonar.server.util.OkHttpClientProvider;
import org.sonar.server.util.Paths2Impl;
import org.sonar.server.util.TempFolderCleaner;

import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.PlatformLevelPredicates.hasPlatformLevel;

public class PlatformLevel1 extends PlatformLevel {
  private final Platform platform;
  private final Properties properties;
  private final Object[] extraRootComponents;

  public PlatformLevel1(Platform platform, Properties properties, Object... extraRootComponents) {
    super("level1");
    this.platform = platform;
    this.properties = properties;
    this.extraRootComponents = extraRootComponents;
  }

  @Override
  public void configureLevel() {
    add(platform, properties);
    addExtraRootComponents();
    Version apiVersion = MetadataLoader.loadApiVersion(System2.INSTANCE);
    Version sqVersion = MetadataLoader.loadSQVersion(System2.INSTANCE);
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);

    add(
      new SonarQubeVersion(sqVersion),
      SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.SERVER, edition),
      ThreadLocalSettings.class,
      ConfigurationProvider.class,
      LogServerVersion.class,
      ProcessCommandWrapperImpl.class,
      RestartFlagHolderImpl.class,
      UuidFactoryImpl.INSTANCE,
      NetworkUtilsImpl.INSTANCE,
      UrlSettings.class,
      EmbeddedDatabaseFactory.class,
      LogbackHelper.class,
      WebServerProcessLogging.class,
      DefaultDatabase.class,
      MyBatis.class,
      StartMyBatis.class,
      PurgeProfiler.class,
      ServerFileSystemImpl.class,
      TempFolderCleaner.class,
      new TempFolderProvider(),
      System2.INSTANCE,
      Paths2Impl.getInstance(),
      ContainerSupportImpl.class,
      Clock.systemDefaultZone(),

      // user session
      ThreadLocalUserSession.class,
      SystemPasscodeImpl.class,

      // DB
      DBSessionsImpl.class,
      DbClient.class,
      new DaoModule(),

      // Elasticsearch
      WebAuthorizationTypeSupport.class,
      new EsModule(),

      // rules/qprofiles
      RuleIndex.class,

      // issues
      IssueIndex.class,
      IssueIndexSyncProgressChecker.class,

      GlobalLockManagerImpl.class,

      new OkHttpClientProvider(),

      CoreExtensionRepositoryImpl.class,
      CoreExtensionsLoader.class,
      WebCoreExtensionsInstaller.class);
    addAll(CorePropertyDefinitions.all());

    // cluster
    add(DefaultNodeInformation.class);
  }

  private void addExtraRootComponents() {
    if (this.extraRootComponents != null) {
      for (Object extraRootComponent : this.extraRootComponents) {
        add(extraRootComponent);
      }
    }
  }

  @Override
  public PlatformLevel start() {
    PlatformLevel start = super.start();
    get(CoreExtensionsLoader.class)
      .load();
    get(WebCoreExtensionsInstaller.class)
      .install(getContainer(), hasPlatformLevel(1), noAdditionalSideFilter());
    if (getOptional(AuditPersister.class).isEmpty()) {
      add(NoOpAuditPersister.class);
    }

    return start;
  }
}
