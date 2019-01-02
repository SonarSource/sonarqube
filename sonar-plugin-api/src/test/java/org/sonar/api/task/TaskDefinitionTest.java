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
package org.sonar.api.task;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskDefinitionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

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

    assertThat(def1).isEqualTo(def1);
    assertThat(def1).isEqualTo(def1bis);
    assertThat(def2).isNotEqualTo(def1);
    assertThat(def2).isNotEqualTo("one");
    assertThat(def2).isNotEqualTo(null);

    assertThat(def1.hashCode()).isEqualTo(def1.hashCode());
    assertThat(def1.hashCode()).isEqualTo(def1bis.hashCode());
  }

  @Test
  public void test_compare() {
    TaskDefinition foo = TaskDefinition.builder().key("foo").taskClass(FooTask.class).description("Foo").build();
    TaskDefinition bar = TaskDefinition.builder().key("bar").taskClass(FooTask.class).description("Bar").build();

    assertThat(foo.compareTo(bar)).isGreaterThan(0);
    assertThat(foo.compareTo(foo)).isEqualTo(0);
    assertThat(bar.compareTo(foo)).isLessThan(0);
  }

  @Test
  public void description_should_be_required() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Description must be set for task 'foo'");
    TaskDefinition.builder().key("foo").taskClass(FooTask.class).build();
  }

  @Test
  public void key_should_be_required() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Task key must be set");
    TaskDefinition.builder().description("Foo").taskClass(FooTask.class).build();
  }

  @Test
  public void key_should_not_contain_spaces() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Task key 'fo o' must match " + TaskDefinition.KEY_PATTERN);
    TaskDefinition.builder().key("fo o").description("foo").taskClass(FooTask.class).build();
  }

  @Test
  public void class_should_be_required() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Class must be set for task 'foo'");
    TaskDefinition.builder().key("foo").description("Foo").build();
  }

  private static class FooTask implements Task {
    public void execute() {
    }
  }
}
