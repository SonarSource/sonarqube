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
package org.sonar.server.project;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ProjectQGChangeEventListenerTest {

  private final DbClient dbClient = mock(DbClient.class);

  private final SnapshotDao snapshotDao = mock(SnapshotDao.class);
  private final EventDao eventDao = mock(EventDao.class);
  private final ComponentDao componentDao = mock(ComponentDao.class);

  private final Configuration projectConfiguration = mock(Configuration.class);
  private final BranchDto branch = new BranchDto();
  private final Condition defaultCondition = new Condition("bugs", Condition.Operator.GREATER_THAN, "10");

  private final ProjectQGChangeEventListener underTest = new ProjectQGChangeEventListener(dbClient);

  private ProjectDto project;
  private SnapshotDto analysis;

  @Before
  public void before() {
    project = new ProjectDto();
    project.setUuid("uuid");
    analysis = new SnapshotDto();

    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.eventDao()).thenReturn(eventDao);
    when(dbClient.snapshotDao()).thenReturn(snapshotDao);

    when(dbClient.openSession(false)).thenReturn(mock(DbSession.class));
  }

  @Test
  public void onIssueChanges_givenEmptyEvent_doNotInteractWithDatabase() {
    Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier = Optional::empty;

    QGChangeEvent qualityGateEvent = new QGChangeEvent(project, branch, analysis, projectConfiguration, Metric.Level.OK, qualityGateSupplier);

    underTest.onIssueChanges(qualityGateEvent, Set.of());

    verifyNoInteractions(dbClient);
  }

  @Test
  public void onIssueChanges_givenEventWithNoPreviousStatus_doNotInteractWithDatabase() {
    EvaluatedQualityGate value = EvaluatedQualityGate.newBuilder()
      .setQualityGate(createDefaultQualityGate())
      .addEvaluatedCondition(createEvaluatedCondition())
      .setStatus(Metric.Level.ERROR)
      .build();
    Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier = () -> Optional.of(value);

    QGChangeEvent qualityGateEvent = new QGChangeEvent(project, branch, analysis, projectConfiguration, null, qualityGateSupplier);

    underTest.onIssueChanges(qualityGateEvent, Set.of());

    verifyNoInteractions(dbClient);
  }

  @Test
  public void onIssueChanges_givenCurrentStatusTheSameAsPrevious_doNotInteractWithDatabase() {
    EvaluatedQualityGate value = EvaluatedQualityGate.newBuilder()
      .setQualityGate(createDefaultQualityGate())
      .addEvaluatedCondition(createEvaluatedCondition())
      .setStatus(Metric.Level.ERROR)
      .build();
    Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier = () -> Optional.of(value);

    QGChangeEvent qualityGateEvent = new QGChangeEvent(project, branch, analysis, projectConfiguration, Metric.Level.ERROR, qualityGateSupplier);

    underTest.onIssueChanges(qualityGateEvent, Set.of());

    verifyNoInteractions(dbClient);
  }

  @Test
  public void onIssueChanges_whenValidEvent_insertEventAndSnapshotToDatabase() {
    EvaluatedQualityGate value = EvaluatedQualityGate.newBuilder()
      .setQualityGate(createDefaultQualityGate())
      .addEvaluatedCondition(createEvaluatedCondition())
      .setStatus(Metric.Level.OK)
      .build();
    Supplier<Optional<EvaluatedQualityGate>> qualityGateSupplier = () -> Optional.of(value);

    QGChangeEvent qualityGateEvent = new QGChangeEvent(project, branch, analysis, projectConfiguration, Metric.Level.ERROR, qualityGateSupplier);

    ComponentDto projectComponentDto = new ComponentDto();
    projectComponentDto.setQualifier("TRK");
    projectComponentDto.setUuid("uuid");
    when(componentDao.selectByBranchUuid(anyString(), any())).thenReturn(List.of(projectComponentDto));

    underTest.onIssueChanges(qualityGateEvent, Set.of());

    verify(eventDao, times(1)).insert(any(), any());
    verify(snapshotDao, times(1)).insert(any(DbSession.class), any(SnapshotDto.class));
  }

  private EvaluatedCondition createEvaluatedCondition() {
    return new EvaluatedCondition(defaultCondition, EvaluatedCondition.EvaluationStatus.ERROR, "5");
  }

  private QualityGate createDefaultQualityGate() {
    return new QualityGate("id", "name", Set.of(defaultCondition));
  }

}
