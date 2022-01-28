/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.task;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TaskDefinitionTest {

  @Test
  public void should_build() {
    TaskDefinition def = TaskDefinition.builder().key("foo").taskClass(FooTask.class).description("Foo").build();
    assertThat(def.key()).isEqualTo("foo");
    assertThat(def.description()).isEqualTo("Foo");
    assertThat(def.taskClass()).isEqualTo(FooTask.class);
    assertThat(def.toString()).isEqualTo("Task foo[class=org.sonar.api.task.TaskDefinitionTest$FooTask, desc=Foo]");
  }

  @Test
  public void test_equals_and_hashcode() {
    TaskDefinition def1 = TaskDefinition.builder().key("one").taskClass(FooTask.class).description("Foo").build();
    TaskDefinition def1bis = TaskDefinition.builder().key("one").taskClass(FooTask.class).description("Foo").build();
    TaskDefinition def2 = TaskDefinition.builder().key("two").taskClass(FooTask.class).description("Foo").build();

    assertThat(def1)
      .isEqualTo(def1)
      .isEqualTo(def1bis);
    assertThat(def2)
      .isNotEqualTo(def1)
      .isNotEqualTo("one")
      .isNotNull();

    assertThat(def1)
      .hasSameHashCodeAs(def1)
      .hasSameHashCodeAs(def1bis);
  }

  @Test
  public void test_compare() {
    TaskDefinition foo = TaskDefinition.builder().key("foo").taskClass(FooTask.class).description("Foo").build();
    TaskDefinition bar = TaskDefinition.builder().key("bar").taskClass(FooTask.class).description("Bar").build();

    assertThat(foo.compareTo(bar)).isGreaterThan(0);
    assertThat(foo).isEqualByComparingTo(foo);
    assertThat(bar.compareTo(foo)).isLessThan(0);
  }

  @Test
  public void description_should_be_required() {
    assertThatThrownBy(() -> TaskDefinition.builder().key("foo").taskClass(FooTask.class).build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Description must be set for task 'foo'");
  }

  @Test
  public void key_should_be_required() {
    assertThatThrownBy(() -> TaskDefinition.builder().description("Foo").taskClass(FooTask.class).build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Task key must be set");
  }

  @Test
  public void key_should_not_contain_spaces() {
    assertThatThrownBy(() -> TaskDefinition.builder().key("fo o").description("foo").taskClass(FooTask.class).build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Task key 'fo o' must match " + TaskDefinition.KEY_PATTERN);
  }

  @Test
  public void class_should_be_required() {
    assertThatThrownBy(() -> TaskDefinition.builder().key("foo").description("Foo").build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Class must be set for task 'foo'");
  }

  private static class FooTask implements Task {
    public void execute() {
    }
  }
}
