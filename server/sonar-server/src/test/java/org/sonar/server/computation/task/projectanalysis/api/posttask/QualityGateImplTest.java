/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.api.posttask;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.posttask.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QualityGateImplTest {
  private static final String SOME_ID = "some id";
  private static final String SOME_NAME = "some name";
  private static final QualityGate.Status SOME_STATUS = QualityGate.Status.OK;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private QualityGate.Condition condition = mock(QualityGate.Condition.class);
  private QualityGateImpl underTest = new QualityGateImpl(SOME_ID, SOME_NAME, SOME_STATUS, ImmutableList.of(condition));

  @Test
  public void constructor_throws_NPE_if_id_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id can not be null");

    new QualityGateImpl(null, SOME_NAME, SOME_STATUS, Collections.emptyList());
  }

  @Test
  public void constructor_throws_NPE_if_name_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can not be null");

    new QualityGateImpl(SOME_ID, null, SOME_STATUS, Collections.emptyList());
  }

  @Test
  public void constructor_throws_NPE_if_status_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can not be null");

    new QualityGateImpl(SOME_ID, SOME_NAME, null, Collections.emptyList());
  }

  @Test
  public void constructor_throws_NPE_if_conditions_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("conditions can not be null");

    new QualityGateImpl(SOME_ID, SOME_NAME, SOME_STATUS, null);
  }

  @Test
  public void verify_getters() {
    List<QualityGate.Condition> conditions = ImmutableList.of(condition);

    QualityGateImpl underTest = new QualityGateImpl(SOME_ID, SOME_NAME, SOME_STATUS, conditions);

    assertThat(underTest.getId()).isEqualTo(SOME_ID);
    assertThat(underTest.getName()).isEqualTo(SOME_NAME);
    assertThat(underTest.getStatus()).isEqualTo(SOME_STATUS);
    assertThat(underTest.getConditions()).isEqualTo(conditions);
  }

  @Test
  public void verify_toString() {
    when(condition.toString()).thenReturn("{Condition}");

    assertThat(underTest.toString())
      .isEqualTo("QualityGateImpl{id='some id', name='some name', status=OK, conditions=[{Condition}]}");
  }
}
