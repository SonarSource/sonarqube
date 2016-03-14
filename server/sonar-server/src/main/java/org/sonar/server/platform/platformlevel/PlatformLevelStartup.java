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
package org.sonar.server.platform.platformlevel;

import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.issue.filter.RegisterIssueFilters;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.qualitygate.RegisterQualityGates;
import org.sonar.server.qualityprofile.RegisterQualityProfiles;
import org.sonar.server.rule.RegisterRules;
import org.sonar.server.search.IndexSynchronizer;
import org.sonar.server.startup.ClearRulesOverloadedDebt;
import org.sonar.server.startup.DisplayLogOnDeprecatedProjects;
import org.sonar.server.startup.FeedUsersLocalStartupTask;
import org.sonar.server.startup.GeneratePluginIndex;
import org.sonar.server.startup.LogServerId;
import org.sonar.server.startup.RegisterDashboards;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.startup.RegisterNewMeasureFilters;
import org.sonar.server.startup.RegisterPermissionTemplates;
import org.sonar.server.startup.RegisterServletFilters;
import org.sonar.server.startup.RenameDeprecatedPropertyKeys;
import org.sonar.server.startup.RenameIssueWidgets;
import org.sonar.server.user.DoPrivileged;
import org.sonar.server.user.ThreadLocalUserSession;

public class PlatformLevelStartup extends PlatformLevel {
  public PlatformLevelStartup(PlatformLevel parent) {
    super("startup tasks", parent);
  }

  @Override
  protected void configureLevel() {
    add(
      IndexSynchronizer.class,
      RegisterMetrics.class,
      RegisterQualityGates.class,
      RegisterRules.class,
      RegisterQualityProfiles.class,
      GeneratePluginIndex.class,
      RegisterNewMeasureFilters.class,
      RegisterDashboards.class,
      RegisterPermissionTemplates.class,
      RenameDeprecatedPropertyKeys.class,
      LogServerId.class,
      RegisterServletFilters.class,
      RegisterIssueFilters.class,
      RenameIssueWidgets.class,
      ServerLifecycleNotifier.class,
      DisplayLogOnDeprecatedProjects.class,
      ClearRulesOverloadedDebt.class,
      FeedUsersLocalStartupTask.class
    );
  }

  @Override
  public PlatformLevel start() {
    DoPrivileged.execute(new DoPrivileged.Task(getComponentByType(ThreadLocalUserSession.class)) {
      @Override
      protected void doPrivileged() {
        PlatformLevelStartup.super.start();
        getComponentByType(IndexSynchronizer.class).execute();
        getComponentByType(ServerLifecycleNotifier.class).notifyStart();
        getComponentByType(ProcessCommandWrapper.class).notifyOperational();
      }
    });

    return this;
  }
}
