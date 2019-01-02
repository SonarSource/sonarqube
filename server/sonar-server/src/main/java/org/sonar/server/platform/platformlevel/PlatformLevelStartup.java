/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.es.IndexerStartupTask;
import org.sonar.server.organization.DefaultOrganizationEnforcer;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.platform.web.RegisterServletFilters;
import org.sonar.server.qualitygate.RegisterQualityGates;
import org.sonar.server.qualityprofile.BuiltInQProfileInsertImpl;
import org.sonar.server.qualityprofile.BuiltInQProfileLoader;
import org.sonar.server.qualityprofile.BuiltInQProfileUpdateImpl;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesUpdateListener;
import org.sonar.server.qualityprofile.RegisterQualityProfiles;
import org.sonar.server.rule.RegisterRules;
import org.sonar.server.rule.WebServerRuleFinder;
import org.sonar.server.startup.GeneratePluginIndex;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.startup.RegisterPermissionTemplates;
import org.sonar.server.startup.RegisterPlugins;
import org.sonar.server.startup.RenameDeprecatedPropertyKeys;
import org.sonar.server.user.DoPrivileged;
import org.sonar.server.user.ThreadLocalUserSession;

public class PlatformLevelStartup extends PlatformLevel {
  public PlatformLevelStartup(PlatformLevel parent) {
    super("startup tasks", parent);
  }

  @Override
  protected void configureLevel() {
    add(GeneratePluginIndex.class,
      RegisterPlugins.class,
      ServerLifecycleNotifier.class,
      DefaultOrganizationEnforcer.class);

    addIfStartupLeader(
      IndexerStartupTask.class,
      RegisterMetrics.class,
      RegisterQualityGates.class,
      RegisterRules.class);
    add(BuiltInQProfileLoader.class);
    addIfStartupLeader(
      BuiltInQualityProfilesUpdateListener.class,
      BuiltInQProfileInsertImpl.class,
      BuiltInQProfileUpdateImpl.class,
      RegisterQualityProfiles.class,
      RegisterPermissionTemplates.class,
      RenameDeprecatedPropertyKeys.class);

    // RegisterServletFilters makes the WebService engine of Level4 served by the MasterServletFilter, therefor it
    // must be started after all the other startup tasks
    add(RegisterServletFilters.class);
  }

  @Override
  public PlatformLevel start() {
    DoPrivileged.execute(new DoPrivileged.Task(get(ThreadLocalUserSession.class)) {
      @Override
      protected void doPrivileged() {
        PlatformLevelStartup.super.start();
        getOptional(IndexerStartupTask.class).ifPresent(IndexerStartupTask::execute);
        get(ServerLifecycleNotifier.class).notifyStart();
        get(ProcessCommandWrapper.class).notifyOperational();
        get(WebServerRuleFinder.class).stopCaching();
        Loggers.get(PlatformLevelStartup.class)
          .info("Running {} Edition", get(PlatformEditionProvider.class).get().map(EditionProvider.Edition::getLabel).orElse(""));
      }
    });

    return this;
  }
}
