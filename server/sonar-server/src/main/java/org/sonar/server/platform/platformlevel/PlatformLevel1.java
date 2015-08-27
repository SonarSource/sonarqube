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
package org.sonar.server.platform.platformlevel;

import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TempFolderCleaner;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.config.Logback;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DaoModule;
import org.sonar.db.DatabaseChecker;
import org.sonar.db.DefaultDatabase;
import org.sonar.db.MyBatis;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.semaphore.SemaphoresImpl;
import org.sonar.db.version.DatabaseVersion;
import org.sonar.db.version.MigrationStepModule;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.EmbeddedDatabaseFactory;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.DatabaseServerCompatibility;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerSettings;
import org.sonar.server.platform.TempFolderProvider;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.ruby.PlatformRackBridge;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.EsSearchModule;
import org.sonar.server.search.IndexQueue;
import org.sonar.server.user.ThreadLocalUserSession;

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
    add(
      ServerSettings.class,
      ServerImpl.class,
      Logback.class,
      UuidFactoryImpl.INSTANCE,
      EmbeddedDatabaseFactory.class,
      DefaultDatabase.class,
      DatabaseChecker.class,
      MyBatis.class,
      IndexQueue.class,
      DatabaseServerCompatibility.class,
      DatabaseVersion.class,
      PurgeProfiler.class,
      DefaultServerFileSystem.class,
      SemaphoresImpl.class,
      TempFolderCleaner.class,
      new TempFolderProvider(),
      System2.INSTANCE,

      // rack bridges
      PlatformRackBridge.class,

      // user session
      ThreadLocalUserSession.class,

      // DB
      DbClient.class,
      DaoModule.class,

      // Elasticsearch
      EsSearchModule.class,

      // DAOs to be moved to sonar-db
      RuleDao.class,
      ActiveRuleDao.class,

      // rules/qprofiles
      RuleNormalizer.class,
      ActiveRuleNormalizer.class,
      RuleIndex.class,
      ActiveRuleIndex.class,

      // issues
      IssueIndex.class,

      // Classes kept for backward compatibility of plugins/libs (like sonar-license) that are directly calling classes from the core
      org.sonar.core.properties.PropertiesDao.class,
      org.sonar.core.persistence.MyBatis.class);
    addAll(CorePropertyDefinitions.all());
    add(MigrationStepModule.class);
  }

  private void addExtraRootComponents() {
    if (this.extraRootComponents != null) {
      for (Object extraRootComponent : this.extraRootComponents) {
        add(extraRootComponent);
      }
    }
  }
}
