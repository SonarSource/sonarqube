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
import org.sonar.core.measure.db.MeasureFilterDao;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DefaultDatabase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.ComponentIndexDao;
import org.sonar.server.component.db.ComponentLinkDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.measure.custom.persistence.CustomMeasureDao;
import org.sonar.server.dashboard.db.DashboardDao;
import org.sonar.server.dashboard.db.WidgetDao;
import org.sonar.server.dashboard.db.WidgetPropertyDao;
import org.sonar.server.db.DatabaseChecker;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.EmbeddedDatabaseFactory;
import org.sonar.server.db.migrations.MigrationStepModule;
import org.sonar.server.event.db.EventDao;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
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
import org.sonar.server.source.db.FileSourceDao;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.user.db.UserGroupDao;

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
      EmbeddedDatabaseFactory.class,
      DefaultDatabase.class,
      DatabaseChecker.class,
      MyBatis.class,
      IndexQueue.class,
      DatabaseServerCompatibility.class,
      DatabaseVersion.class,
      PurgeProfiler.class,
      DefaultServerFileSystem.class,
      SemaphoreUpdater.class,
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

      // Elasticsearch
      EsSearchModule.class,

      // users
      GroupDao.class,
      UserDao.class,
      UserGroupDao.class,

      // dashboards
      DashboardDao.class,
      WidgetDao.class,
      WidgetPropertyDao.class,

      // rules/qprofiles
      RuleNormalizer.class,
      ActiveRuleNormalizer.class,
      RuleIndex.class,
      ActiveRuleIndex.class,
      RuleDao.class,
      ActiveRuleDao.class,

      // issues
      IssueIndex.class,
      IssueDao.class,

      // measures
      MeasureDao.class,
      MetricDao.class,
      MeasureFilterDao.class,
      CustomMeasureDao.class,

      // components
      ComponentDao.class,
      ComponentIndexDao.class,
      ComponentLinkDao.class,
      SnapshotDao.class,

      EventDao.class,
      ActivityDao.class,
      AnalysisReportDao.class,
      FileSourceDao.class);
    addAll(CorePropertyDefinitions.all());
    add(MigrationStepModule.class);
    addAll(DaoUtils.getDaoClasses());
  }

  private void addExtraRootComponents() {
    if (this.extraRootComponents != null) {
      for (Object extraRootComponent : this.extraRootComponents) {
        add(extraRootComponent);
      }
    }
  }
}
