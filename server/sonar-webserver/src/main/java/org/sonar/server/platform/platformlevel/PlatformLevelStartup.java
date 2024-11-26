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

import org.slf4j.LoggerFactory;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.authentication.DefaultAdminCredentialsVerifierImpl;
import org.sonar.server.ce.queue.CeQueueCleaner;
import org.sonar.server.es.IndexerStartupTask;
import org.sonar.server.platform.ServerLifecycleNotifier;
import org.sonar.server.platform.web.RegisterServletFilters;
import org.sonar.server.plugins.DetectPluginChange;
import org.sonar.server.plugins.PluginConsentVerifier;
import org.sonar.server.qualitygate.RegisterQualityGates;
import org.sonar.server.qualityprofile.RegisterQualityProfiles;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileInsertImpl;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileLoader;
import org.sonar.server.qualityprofile.builtin.BuiltInQProfileUpdateImpl;
import org.sonar.server.qualityprofile.builtin.BuiltInQualityProfilesUpdateListener;
import org.sonar.server.rule.AdvancedRuleDescriptionSectionsGenerator;
import org.sonar.server.rule.LegacyHotspotRuleDescriptionSectionsGenerator;
import org.sonar.server.rule.LegacyIssueRuleDescriptionSectionsGenerator;
import org.sonar.server.rule.RuleDescriptionSectionsGeneratorResolver;
import org.sonar.server.rule.WebServerRuleFinder;
import org.sonar.server.rule.registration.NewRuleCreator;
import org.sonar.server.rule.registration.QualityProfileChangesUpdater;
import org.sonar.server.rule.registration.RulesKeyVerifier;
import org.sonar.server.rule.registration.RulesRegistrant;
import org.sonar.server.rule.registration.StartupRuleUpdater;
import org.sonar.server.startup.RegisterMetrics;
import org.sonar.server.startup.RegisterPermissionTemplates;
import org.sonar.server.startup.RegisterPlugins;
import org.sonar.server.startup.RenameDeprecatedPropertyKeys;
import org.sonar.server.startup.UpgradeSuggestionsCleaner;
import org.sonar.server.user.DoPrivileged;
import org.sonar.server.user.ThreadLocalUserSession;

public class PlatformLevelStartup extends PlatformLevel {
  private AddIfStartupLeaderAndPluginsChanged addIfPluginsChanged;

  public PlatformLevelStartup(PlatformLevel parent) {
    super("startup tasks", parent);
  }

  @Override
  protected void configureLevel() {
    add(ServerLifecycleNotifier.class);

    addIfStartupLeader(
      IndexerStartupTask.class);
    addIfStartupLeaderAndPluginsChanged(
      RuleDescriptionSectionsGeneratorResolver.class,
      AdvancedRuleDescriptionSectionsGenerator.class,
      LegacyHotspotRuleDescriptionSectionsGenerator.class,
      LegacyIssueRuleDescriptionSectionsGenerator.class,
      RulesRegistrant.class,
      NewRuleCreator.class,
      RulesKeyVerifier.class,
      StartupRuleUpdater.class,
      QualityProfileChangesUpdater.class,
      RegisterMetrics.class,
      RegisterQualityGates.class,
      BuiltInQProfileLoader.class);
    addIfStartupLeader(
      BuiltInQualityProfilesUpdateListener.class,
      BuiltInQProfileUpdateImpl.class);
    addIfStartupLeaderAndPluginsChanged(
      BuiltInQProfileInsertImpl.class,
      RegisterQualityProfiles.class);
    addIfStartupLeader(
      RegisterPermissionTemplates.class,
      RenameDeprecatedPropertyKeys.class,
      CeQueueCleaner.class,
      UpgradeSuggestionsCleaner.class,
      PluginConsentVerifier.class);
    add(RegisterPlugins.class,
      // RegisterServletFilters makes the WebService engine of Level4 served by the MasterServletFilter, therefore it
      // must be started after all the other startup tasks
      RegisterServletFilters.class
    );
  }

  /**
   * Add a component to container only if plugins have changed since last start.
   *
   * @throws IllegalStateException if called from PlatformLevel3 or below, plugin info is loaded yet
   */
  AddIfStartupLeaderAndPluginsChanged addIfStartupLeaderAndPluginsChanged(Object... objects) {
    if (addIfPluginsChanged == null) {
      this.addIfPluginsChanged = new AddIfStartupLeaderAndPluginsChanged(getWebServer().isStartupLeader() && anyPluginChanged());
    }
    addIfPluginsChanged.ifAdd(objects);
    return addIfPluginsChanged;
  }

  private boolean anyPluginChanged() {
    return parent.getOptional(DetectPluginChange.class)
      .map(DetectPluginChange::anyPluginChanged)
      .orElseThrow(() -> new IllegalStateException("DetectPluginChange not available in the container yet"));
  }

  public final class AddIfStartupLeaderAndPluginsChanged extends AddIf {
    private AddIfStartupLeaderAndPluginsChanged(boolean condition) {
      super(condition);
    }
  }

  @Override
  public PlatformLevel start() {
    DoPrivileged.execute(new DoPrivileged.Task(parent.get(ThreadLocalUserSession.class)) {
      @Override
      protected void doPrivileged() {
        PlatformLevelStartup.super.start();
        getOptional(IndexerStartupTask.class).ifPresent(IndexerStartupTask::execute);
        get(ServerLifecycleNotifier.class).notifyStart();
        get(ProcessCommandWrapper.class).notifyOperational();
        get(WebServerRuleFinder.class).stopCaching();
        LoggerFactory.getLogger(PlatformLevelStartup.class)
          .info("Running {} Edition", get(PlatformEditionProvider.class).get().map(EditionProvider.Edition::getLabel).orElse(""));
        get(DefaultAdminCredentialsVerifierImpl.class).runAtStart();
      }
    });

    return this;
  }
}
