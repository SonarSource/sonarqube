/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;

/**
 * Check the runtime Java version, and log deprecation warnings if needed.
 */
class JREDeprecationChecker implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(JREDeprecationChecker.class);

  private final AnalysisWarnings analysisWarnings;
  private final System2 system2;

  JREDeprecationChecker(AnalysisWarnings analysisWarnings, System2 system2) {
    this.analysisWarnings = analysisWarnings;
    this.system2 = system2;
  }

  @Override
  public void start() {
    String javaVersion = system2.property("java.version");
    if (javaVersion != null) {
      try {
        String[] versionElements = javaVersion.split("\\.");
        int majorVersion = Integer.parseInt(versionElements[0]);
        if (majorVersion < 21) {
          String msg = "Java " + majorVersion + " scanner support ends with SonarQube 2026.3 (July 2026). " +
            "Please upgrade to Java 21 or newer, or use JRE auto-provisioning to keep this requirement always up to date.";
          LOG.warn(msg);
          analysisWarnings.addUnique(msg);
        }
      } catch (NumberFormatException e) {
        // ignore malformed version
      }
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
