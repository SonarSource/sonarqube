/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Set;
import org.sonar.api.config.GlobalPropertyChangeHandler;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.Boolean.parseBoolean;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_SEVERITY_KEY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

@ServerSide
public class SeverityMetricsModeHandler extends GlobalPropertyChangeHandler {

  static final Set<String> STANDARD_SEVERITY_METRIC_KEYS = Set.of(
    NEW_BUGS_SEVERITY_KEY,
    NEW_VULNERABILITIES_SEVERITY_KEY,
    NEW_CODE_SMELLS_SEVERITY_KEY);

  static final Set<String> MQR_SEVERITY_METRIC_KEYS = Set.of(
    NEW_RELIABILITY_ISSUE_SEVERITY_KEY,
    NEW_SECURITY_ISSUE_SEVERITY_KEY,
    NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY);

  private final DbClient dbClient;

  public SeverityMetricsModeHandler(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void onChange(PropertyChange change) {
    if (!MULTI_QUALITY_MODE_ENABLED.equals(change.getKey())) {
      return;
    }
    boolean mqrModeEnabled = parseBoolean(change.getNewValue());
    try (DbSession session = dbClient.openSession(false)) {
      updateSeverityMetrics(session, mqrModeEnabled);
      session.commit();
    }
  }

  void updateSeverityMetrics(DbSession session, boolean mqrModeEnabled) {
    Set<String> keysToEnable = mqrModeEnabled ? MQR_SEVERITY_METRIC_KEYS : STANDARD_SEVERITY_METRIC_KEYS;
    Set<String> keysToDisable = mqrModeEnabled ? STANDARD_SEVERITY_METRIC_KEYS : MQR_SEVERITY_METRIC_KEYS;
    keysToEnable.forEach(key -> dbClient.metricDao().enableByKey(session, key));
    keysToDisable.forEach(key -> dbClient.metricDao().disableByKey(session, key));
  }
}
