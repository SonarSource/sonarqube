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

public class ProjectBuilder_PostProjectAnalysisTaskTesterTest {
  private static final String SOME_NAME = "some name";
  private static final String SOME_KEY = "some key";
  private static final String SOME_UUID = "some uuid";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PostProjectAnalysisTaskTester.ProjectBuilder underTest = PostProjectAnalysisTaskTester.newProjectBuilder();

  @Test
  public void setKey_throws_NPE_if_key_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key cannot be null");

    underTest.setKey(null);
  }

  @Test
  public void setName_throws_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name cannot be null");

    underTest.setName(null);
  }

  @Test
  public void setUuid_throws_NPE_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid cannot be null");

    underTest.setUuid(null);
  }

  @Test
  public void build_throws_NPE_if_key_is_null() {
    underTest.setUuid(SOME_UUID).setName(SOME_NAME);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key cannot be null");

    underTest.build();
  }

  @Test
  public void build_throws_NPE_if_name_is_null() {
    underTest.setUuid(SOME_UUID).setKey(SOME_KEY);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name cannot be null");


    underTest.build();
  }

  @Test
  public void build_throws_NPE_if_uuid_is_null() {
    underTest.setKey(SOME_KEY).setName(SOME_NAME);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid cannot be null");

    underTest.build();
  }

  @Test
  public void build_returns_new_instance_at_each_call() {
    underTest.setUuid(SOME_UUID).setKey(SOME_KEY).setName(SOME_NAME);

    assertThat(underTest.build()).isNotSameAs(underTest.build());
  }

  @Test
  public void verify_getters() {
    Project project = underTest.setUuid(SOME_UUID).setKey(SOME_KEY).setName(SOME_NAME).build();

    assertThat(project.getUuid()).isEqualTo(SOME_UUID);
    assertThat(project.getKey()).isEqualTo(SOME_KEY);
    assertThat(project.getName()).isEqualTo(SOME_NAME);
  }

  @Test
  public void verify_toString() {
    assertThat(underTest.setUuid(SOME_UUID).setKey(SOME_KEY).setName(SOME_NAME).build().toString())
      .isEqualTo("Project{uuid='some uuid', key='some key', name='some name'}");
  }
}
