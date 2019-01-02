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
package org.sonar.server.issue.workflow;

import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransitionTest {

  Condition condition1 = mock(Condition.class);
  Condition condition2 = mock(Condition.class);
  Function function1 = mock(Function.class);
  Function function2 = mock(Function.class);

  @Test
  public void test_builder() {
    Transition transition = Transition.builder("close")
      .from("OPEN").to("CLOSED")
      .conditions(condition1, condition2)
      .functions(function1, function2)
      .requiredProjectPermission(UserRole.ISSUE_ADMIN)
      .build();
    assertThat(transition.key()).isEqualTo("close");
    assertThat(transition.from()).isEqualTo("OPEN");
    assertThat(transition.to()).isEqualTo("CLOSED");
    assertThat(transition.conditions()).containsOnly(condition1, condition2);
    assertThat(transition.functions()).containsOnly(function1, function2);
    assertThat(transition.automatic()).isFalse();
    assertThat(transition.requiredProjectPermission()).isEqualTo(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void test_simplest_transition() {
    Transition transition = Transition.builder("close")
      .from("OPEN").to("CLOSED")
      .build();
    assertThat(transition.key()).isEqualTo("close");
    assertThat(transition.from()).isEqualTo("OPEN");
    assertThat(transition.to()).isEqualTo("CLOSED");
    assertThat(transition.conditions()).isEmpty();
    assertThat(transition.functions()).isEmpty();
    assertThat(transition.requiredProjectPermission()).isNull();
  }

  @Test
  public void key_should_be_set() {
    try {
      Transition.builder("").from("OPEN").to("CLOSED").build();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Transition key must be set");
    }
  }

  @Test
  public void key_should_be_lower_case() {
    try {
      Transition.builder("CLOSE").from("OPEN").to("CLOSED").build();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Transition key must be lower-case");
    }
  }

  @Test
  public void originating_status_should_be_set() {
    try {
      Transition.builder("close").from("").to("CLOSED").build();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Originating status must be set");
    }
  }

  @Test
  public void destination_status_should_be_set() {
    try {
      Transition.builder("close").from("OPEN").to("").build();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Destination status must be set");
    }
  }

  @Test
  public void should_verify_conditions() {
    DefaultIssue issue = new DefaultIssue();
    Transition transition = Transition.builder("close")
      .from("OPEN").to("CLOSED")
      .conditions(condition1, condition2)
      .build();

    when(condition1.matches(issue)).thenReturn(true);
    when(condition2.matches(issue)).thenReturn(false);
    assertThat(transition.supports(issue)).isFalse();

    when(condition1.matches(issue)).thenReturn(true);
    when(condition2.matches(issue)).thenReturn(true);
    assertThat(transition.supports(issue)).isTrue();
  }

  @Test
  public void test_equals_and_hashCode() {
    Transition t1 = Transition.create("resolve", "OPEN", "RESOLVED");
    Transition t2 = Transition.create("resolve", "REOPENED", "RESOLVED");
    Transition t3 = Transition.create("confirm", "OPEN", "CONFIRMED");

    assertThat(t1).isNotEqualTo(t2);
    assertThat(t1).isNotEqualTo(t3);
    assertThat(t1).isEqualTo(t1);

    assertThat(t1.hashCode()).isEqualTo(t1.hashCode());
  }

  @Test
  public void test_toString() {
    Transition t1 = Transition.create("resolve", "OPEN", "RESOLVED");
    assertThat(t1.toString()).isEqualTo("OPEN->resolve->RESOLVED");
  }

  @Test
  public void test_automatic_transition() {
    Transition transition = Transition.builder("close")
      .from("OPEN").to("CLOSED")
      .automatic()
      .build();
    assertThat(transition.automatic()).isTrue();
  }
}
