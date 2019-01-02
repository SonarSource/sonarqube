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

public class CeTaskBuilder_PostProjectAnalysisTaskTesterTest {
  private static final CeTask.Status SOME_STATUS = CeTask.Status.SUCCESS;
  private static final String SOME_ID = "some id";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PostProjectAnalysisTaskTester.CeTaskBuilder underTest = PostProjectAnalysisTaskTester.newCeTaskBuilder();

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
  public void build_throws_NPE_if_id_is_null() {
    underTest.setStatus(SOME_STATUS);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id cannot be null");

    underTest.build();
  }

  @Test
  public void build_throws_NPE_if_status_is_null() {
    underTest.setId(SOME_ID);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status cannot be null");

    underTest.build();
  }

  @Test
  public void build_returns_new_instance_at_each_call() {
    underTest.setId(SOME_ID).setStatus(SOME_STATUS);

    assertThat(underTest.build()).isNotSameAs(underTest.build());
  }

  @Test
  public void verify_getters() {
    CeTask ceTask = underTest.setId(SOME_ID).setStatus(SOME_STATUS).build();

    assertThat(ceTask.getId()).isEqualTo(SOME_ID);
    assertThat(ceTask.getStatus()).isEqualTo(SOME_STATUS);
  }

  @Test
  public void verify_toString() {
    assertThat(underTest.setId(SOME_ID).setStatus(SOME_STATUS).build().toString()).isEqualTo("CeTask{id='some id', status=SUCCESS}");
  }
}
