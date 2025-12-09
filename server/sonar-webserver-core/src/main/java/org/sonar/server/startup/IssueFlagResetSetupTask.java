/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.startup;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.property.InternalProperties;

import static org.sonar.api.SonarEdition.COMMUNITY;

/**
 * Reset FROM_SONARQUBE_UPDATE flag on issues when SonarQube version changes.
 * This task runs only on the startup leader node and manages version tracking and flag reset based on semantic version comparison.
 */
@ServerSide
public class IssueFlagResetSetupTask implements Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueFlagResetSetupTask.class);
  private static final String SONARQUBE_CURRENT_VERSION = "sonarqube.currentVersion";

  private final DbClient dbClient;
  private final InternalProperties internalProperties;
  private final SonarQubeVersion sonarQubeVersion;
  private final SonarRuntime sonarRuntime;

  public IssueFlagResetSetupTask(DbClient dbClient, InternalProperties internalProperties, 
    SonarQubeVersion sonarQubeVersion, SonarRuntime sonarRuntime) {
    this.dbClient = dbClient;
    this.internalProperties = internalProperties;
    this.sonarQubeVersion = sonarQubeVersion;
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void start() {
    if (sonarRuntime.getEdition().equals(COMMUNITY)) {
      LOGGER.debug("Skipping IssueFlagResetSetupTask - requires Developer edition or above");
      return;
    }
    
    Version runtimeVersion = sonarQubeVersion.get();
    String runtimeVersionString = runtimeVersion.toString();
    
    Optional<String> currentVersion = internalProperties.read(SONARQUBE_CURRENT_VERSION);
    
    LOGGER.info("Runtime SonarQube version: {}, stored current: {}",
      runtimeVersionString, currentVersion.orElse("not set"));

    if (currentVersion.isEmpty()) {
      LOGGER.info("Setting initial SonarQube current version to {}", runtimeVersionString);
      internalProperties.write(SONARQUBE_CURRENT_VERSION, runtimeVersionString);
      return;
    }

    try {
      Version storedVersion = Version.parse(currentVersion.get());
      if (isMajorOrMinorVersionChange(runtimeVersion, storedVersion)) {
        updateCurrentVersion(runtimeVersionString);
        resetFromSonarQubeUpdateFlag();
      }
    } catch (Exception e) {
      LOGGER.warn("Unable to parse stored version '{}', updating to current version '{}'", 
        currentVersion.get(), runtimeVersionString, e);
      updateCurrentVersion(runtimeVersionString);
    }
  }

  private static boolean isMajorOrMinorVersionChange(Version newVersion, Version oldVersion) {
    try {
      int newMajor = newVersion.major();
      int newMinor = newVersion.minor();
      int oldMajor = oldVersion.major();
      int oldMinor = oldVersion.minor();
      
      return newMajor != oldMajor || newMinor != oldMinor;
    } catch (Exception e) {
      LOGGER.warn("Unable to parse version numbers for major/minor comparison: old='{}', new='{}'", oldVersion, newVersion, e);
      return false;
    }
  }

  private void updateCurrentVersion(String currentVersion) {
    internalProperties.write(SONARQUBE_CURRENT_VERSION, currentVersion);
  }

  private void resetFromSonarQubeUpdateFlag() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      int updatedRows = dbClient.issueDao().resetFlagFromSonarQubeUpdate(dbSession);
      dbSession.commit();
      LOGGER.info("Reset FROM_SONARQUBE_UPDATE flag to false for {} issues", updatedRows);
    }
  }


  @Override
  public void stop() {
    // nothing to do
  }
}
