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
package org.sonar.api.ce.posttask;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityGateBuilder_PostProjectAnalysisTaskTesterTest {
  private static final String SOME_NAME = "some name";
  private static final QualityGate.Status SOME_STATUS = QualityGate.Status.ERROR;
  private static final String SOME_ID = "some id";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private QualityGate.Condition condition1 = mock(QualityGate.Condition.class);
  private QualityGate.Condition condition2 = mock(QualityGate.Condition.class);
  private PostProjectAnalysisTaskTester.QualityGateBuilder underTest = PostProjectAnalysisTaskTester.newQualityGateBuilder();

  @Test
  public void setId_throws_NPE_if_id_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id cannot be null");

    underTest.setId(null);
  }

  @Test
  public void setStatus_throws_NPE_if_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status cannot be null");

    underTest.setStatus(null);
  }

  @Test
  public void setName_throws_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name cannot be null");

    underTest.setName(null);
  }

  @Test
  public void addCondition_throws_NPE_if_condition_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition cannot be null");

    underTest.add(null);
  }

  @Test
  public void clearConditions_does_not_raise_any_error_if_there_is_no_condition_in_builder() {
    underTest.clearConditions();
  }

  @Test
  public void clearConditions_removes_all_conditions_from_builder() {
    underTest.setId(SOME_ID).setStatus(SOME_STATUS).setName(SOME_NAME).add(condition1).add(condition2);

    assertThat(underTest.build().getConditions()).containsOnly(condition1, condition2);

    underTest.clearConditions();

    assertThat(underTest.build().getConditions()).isEmpty();
  }

  @Test
  public void build_throws_NPE_if_id_is_null() {
    underTest.setStatus(SOME_STATUS).setName(SOME_NAME);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id cannot be null");

    underTest.build();
  }

  @Test
  public void build_throws_NPE_if_status_is_null() {
    underTest.setId(SOME_ID).setName(SOME_NAME);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status cannot be null");

    underTest.build();
  }

  @Test
  public void build_throws_NPE_if_name_is_null() {
    underTest.setId(SOME_ID).setStatus(SOME_STATUS);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name cannot be null");

    underTest.build();
  }

  @Test
  public void build_returns_new_instance_at_each_call() {
    underTest.setId(SOME_ID).setStatus(SOME_STATUS).setName(SOME_NAME);

    assertThat(underTest.build()).isNotSameAs(underTest.build());
  }

  @Test
  public void verify_getters() {
    QualityGate qualityGate = underTest.setId(SOME_ID).setStatus(SOME_STATUS).setName(SOME_NAME).add(condition1).add(condition2).build();

    assertThat(qualityGate.getId()).isEqualTo(SOME_ID);
    assertThat(qualityGate.getStatus()).isEqualTo(SOME_STATUS);
    assertThat(qualityGate.getName()).isEqualTo(SOME_NAME);
    assertThat(qualityGate.getConditions()).containsOnly(condition1, condition2);
  }

  @Test
  public void verify_toString() {
    when(condition1.toString()).thenReturn("condition1");
    when(condition2.toString()).thenReturn("condition2");

    assertThat(underTest.setId(SOME_ID).setStatus(SOME_STATUS).setName(SOME_NAME).add(condition1).add(condition2).build().toString())
      .isEqualTo("QualityGate{id='some id', name='some name', status=ERROR, conditions=[condition1, condition2]}");
  }
}
