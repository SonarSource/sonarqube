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

import java.time.Clock;
import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.internal.ApiVersion;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.server.config.ConfigurationProvider;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DatabaseChecker;
import org.sonar.db.DbClient;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.process.NetworkUtilsImpl;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.app.ProcessCommandWrapperImpl;
import org.sonar.server.app.RestartFlagHolderImpl;
import org.sonar.server.app.WebServerProcessLogging;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.LogServerVersion;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.ServerFileSystemImpl;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.platform.UrlSettings;
import org.sonar.server.platform.WebServerImpl;
import org.sonar.server.platform.db.EmbeddedDatabaseFactory;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.EsSearchModule;
import org.sonar.server.setting.ThreadLocalSettings;
import org.sonar.server.user.SystemPasscodeImpl;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.util.OkHttpClientProvider;

public class PlatformLevel1 extends PlatformLevel {
  private final Platform platform;
  private final Properties properties;
  @Nullable
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
    Version apiVersion = ApiVersion.load(System2.INSTANCE);
    add(
      new SonarQubeVersion(apiVersion),
      SonarRuntimeImpl.forSonarQube(apiVersion, SonarQubeSide.SERVER),
      ThreadLocalSettings.class,
      new ConfigurationProvider(),
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
      DatabaseChecker.class,
      // must instantiate deprecated class in 5.2 and only this one (and not its replacement)
      // to avoid having two SqlSessionFactory instances
      org.sonar.core.persistence.MyBatis.class,
      PurgeProfiler.class,
      ServerFileSystemImpl.class,
      TempFolderCleaner.class,
      new TempFolderProvider(),
      System2.INSTANCE,
      Clock.systemDefaultZone(),

      // user session
      ThreadLocalUserSession.class,
      SystemPasscodeImpl.class,

      // DB
      DBSessionsImpl.class,
      DbClient.class,
      DaoModule.class,

      // Elasticsearch
      EsSearchModule.class,

      // rules/qprofiles
      RuleIndex.class,

      // issues
      IssueIndex.class,

      new OkHttpClientProvider(),
      // Classes kept for backward compatibility of plugins/libs (like sonar-license) that are directly calling classes from the core
      org.sonar.core.properties.PropertiesDao.class);
    addAll(CorePropertyDefinitions.all());

    // cluster
    add(WebServerImpl.class);
  }

  private void addExtraRootComponents() {
    if (this.extraRootComponents != null) {
      for (Object extraRootComponent : this.extraRootComponents) {
        add(extraRootComponent);
      }
    }
  }
}
