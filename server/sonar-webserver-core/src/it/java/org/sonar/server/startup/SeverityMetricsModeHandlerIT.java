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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.GlobalPropertyChangeHandler.PropertyChange;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUE_SEVERITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_SEVERITY_KEY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

class SeverityMetricsModeHandlerIT {

  @RegisterExtension
  DbTester dbTester = DbTester.create();

  private final SeverityMetricsModeHandler underTest = new SeverityMetricsModeHandler(dbTester.getDbClient());

  @Test
  void onChange_whenKeyIsNotMQRMode_shouldIgnore() {
    dbTester.measures().insertMetric(m -> m.setKey(NEW_BUGS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_VULNERABILITIES_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_CODE_SMELLS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_RELIABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_SECURITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));

    underTest.onChange(PropertyChange.create("some.other.property", "true"));

    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_BUGS_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_VULNERABILITIES_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_CODE_SMELLS_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_RELIABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_SECURITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
  }

  @Test
  void onChange_whenSwitchingToMQRMode_shouldEnableMQRMetricsAndDisableStandardMetrics() {
    dbTester.measures().insertMetric(m -> m.setKey(NEW_BUGS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_VULNERABILITIES_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_CODE_SMELLS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_RELIABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_SECURITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));

    underTest.onChange(PropertyChange.create(MULTI_QUALITY_MODE_ENABLED, "true"));

    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_BUGS_SEVERITY_KEY).isEnabled()).isFalse();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_VULNERABILITIES_SEVERITY_KEY).isEnabled()).isFalse();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_CODE_SMELLS_SEVERITY_KEY).isEnabled()).isFalse();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_RELIABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_SECURITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isTrue();
  }

  @Test
  void onChange_whenSwitchingToStandardMode_shouldEnableStandardMetricsAndDisableMQRMetrics() {
    dbTester.measures().insertMetric(m -> m.setKey(NEW_BUGS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_VULNERABILITIES_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_CODE_SMELLS_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_RELIABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_SECURITY_ISSUE_SEVERITY_KEY).setEnabled(true));
    dbTester.measures().insertMetric(m -> m.setKey(NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).setEnabled(true));

    underTest.onChange(PropertyChange.create(MULTI_QUALITY_MODE_ENABLED, "false"));

    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_BUGS_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_VULNERABILITIES_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_CODE_SMELLS_SEVERITY_KEY).isEnabled()).isTrue();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_RELIABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isFalse();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_SECURITY_ISSUE_SEVERITY_KEY).isEnabled()).isFalse();
    assertThat(dbTester.getDbClient().metricDao().selectByKey(dbTester.getSession(), NEW_MAINTAINABILITY_ISSUE_SEVERITY_KEY).isEnabled()).isFalse();
  }
}
