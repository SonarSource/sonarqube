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
package org.sonar.server.qualitygate.notification;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.notification.NotificationService;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailQGChangeEventListenerTest {

  @RegisterExtension
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final NotificationService notificationService = mock(NotificationService.class);
  private final QualityGateConditionFormatter conditionFormatter = mock(QualityGateConditionFormatter.class);
  private final EmailQGChangeEventListener underTest = new EmailQGChangeEventListener(notificationService, db.getDbClient(), conditionFormatter);

  @BeforeEach
  void setUp() {
    db.measures().insertMetric(m -> m.setKey("reliability_rating")
      .setShortName("Reliability Rating")
      .setValueType(org.sonar.api.measures.Metric.ValueType.RATING.name()));
    db.measures().insertMetric(m -> m.setKey("security_rating")
      .setShortName("Security Rating")
      .setValueType(org.sonar.api.measures.Metric.ValueType.RATING.name()));

    when(conditionFormatter.buildAlertText(any(), any())).thenReturn("formatted alert text");
  }

  static Stream<Arguments> qgStatusChanges() {
    return Stream.of(
      arguments(Metric.Level.ERROR, Metric.Level.OK, "OK", "Failed", "false"),
      arguments(Metric.Level.OK, Metric.Level.ERROR, "ERROR", "Passed", "false"),
      arguments(null, Metric.Level.ERROR, "ERROR", null, "true"));
  }

  @ParameterizedTest
  @MethodSource("qgStatusChanges")
  void onIssueChanges_sends_email_when_qg_status_changes(@Nullable Metric.Level previousStatus, Metric.Level newStatus,
    String expectedAlertLevel, @Nullable String expectedPreviousAlertLevel, String expectedIsNewAlert) {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).orElseThrow();

    EvaluatedQualityGate evaluatedQG = mock(EvaluatedQualityGate.class);
    when(evaluatedQG.getStatus()).thenReturn(newStatus);

    Configuration config = mock(Configuration.class);
    QGChangeEvent event = new QGChangeEvent(project, branch, null, config, previousStatus, () -> Optional.of(evaluatedQG));

    underTest.onIssueChanges(event, emptySet());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<QGChangeNotification>> notificationCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(notificationService).deliverEmails(notificationCaptor.capture());

    QGChangeNotification notification = notificationCaptor.getValue().iterator().next();
    assertThat(notification.getType()).isEqualTo("alerts");
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_KEY)).isEqualTo(project.getKey());
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_PROJECT_NAME)).isEqualTo(project.getName());
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_ALERT_LEVEL)).isEqualTo(expectedAlertLevel);
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_PREVIOUS_ALERT_LEVEL)).isEqualTo(expectedPreviousAlertLevel);
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_IS_NEW_ALERT)).isEqualTo(expectedIsNewAlert);
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_BRANCH)).isEqualTo(branch.getKey());
    assertThat(notification.getFieldValue(QGChangeNotification.FIELD_IS_MAIN_BRANCH)).isEqualTo("true");
  }

  @Test
  void onIssueChanges_does_not_send_email_when_qg_status_unchanged() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).orElseThrow();

    EvaluatedQualityGate evaluatedQG = mock(EvaluatedQualityGate.class);
    when(evaluatedQG.getStatus()).thenReturn(Metric.Level.OK);

    Configuration config = mock(Configuration.class);
    QGChangeEvent event = new QGChangeEvent(project, branch, null, config, Metric.Level.OK, () -> Optional.of(evaluatedQG));

    underTest.onIssueChanges(event, emptySet());

    verify(notificationService, never()).deliverEmails(any());
  }

  @Test
  void onIssueChanges_does_not_send_email_when_no_evaluated_qg() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).orElseThrow();

    Configuration config = mock(Configuration.class);
    QGChangeEvent event = new QGChangeEvent(project, branch, null, config, Metric.Level.ERROR, Optional::empty);

    underTest.onIssueChanges(event, emptySet());

    verify(notificationService, never()).deliverEmails(any());
  }

  @Test
  void onIssueChanges_includes_rating_metrics_in_notification() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project.getUuid()).orElseThrow();

    EvaluatedQualityGate evaluatedQG = mock(EvaluatedQualityGate.class);
    when(evaluatedQG.getStatus()).thenReturn(Metric.Level.OK);

    Configuration config = mock(Configuration.class);
    QGChangeEvent event = new QGChangeEvent(project, branch, null, config, Metric.Level.ERROR, () -> Optional.of(evaluatedQG));

    underTest.onIssueChanges(event, emptySet());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<QGChangeNotification>> notificationCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(notificationService).deliverEmails(notificationCaptor.capture());

    QGChangeNotification notification = notificationCaptor.getValue().iterator().next();
    String ratingMetrics = notification.getFieldValue(QGChangeNotification.FIELD_RATING_METRICS);
    // Should contain metric display names (short names) not keys, to match CE behavior
    assertThat(ratingMetrics).contains("Reliability Rating", "Security Rating");
  }

}
