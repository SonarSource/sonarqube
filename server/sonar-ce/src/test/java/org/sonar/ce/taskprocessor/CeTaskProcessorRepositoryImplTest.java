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
package org.sonar.ce.taskprocessor;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskProcessorRepositoryImplTest {
  private static final String SOME_CE_TASK_TYPE = "some type";
  private static final String SOME_COMPONENT_KEY = "key";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_accepts_empty_array_argument() {
    new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {});
  }

  @Test
  public void constructor_throws_IAE_if_two_TaskProcessor_handle_the_same_CeTask_type() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(
      "There can be only one CeTaskProcessor instance registered as the processor for CeTask type " + SOME_CE_TASK_TYPE + ". " +
        "More than one found. Please fix your configuration: " + SomeProcessor1.class.getName() + ", " + SomeProcessor2.class.getName());

    new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {
      new SomeProcessor1(SOME_CE_TASK_TYPE),
      new SomeProcessor2(SOME_CE_TASK_TYPE)
    });
  }

  @Test
  public void constructor_throws_IAE_if_multiple_TaskProcessor_overlap_their_supported_CeTask_type() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(
      "There can be only one CeTaskProcessor instance registered as the processor for CeTask type " + SOME_CE_TASK_TYPE + ". " +
        "More than one found. Please fix your configuration: " + SomeProcessor1.class.getName() + ", " + SomeProcessor2.class.getName());

    new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {
      new SomeProcessor2(SOME_CE_TASK_TYPE + "_2", SOME_CE_TASK_TYPE),
      new SomeProcessor1(SOME_CE_TASK_TYPE, SOME_CE_TASK_TYPE + "_3")
    });
  }

  @Test
  public void getForTask_returns_absent_if_repository_is_empty() {
    CeTaskProcessorRepositoryImpl underTest = new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {});

    assertThat(underTest.getForCeTask(createCeTask(SOME_CE_TASK_TYPE, SOME_COMPONENT_KEY))).isEmpty();
  }

  @Test
  public void getForTask_returns_absent_if_repository_does_not_contain_matching_TaskProcessor() {
    CeTaskProcessorRepositoryImpl underTest = new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {
      createCeTaskProcessor(SOME_CE_TASK_TYPE + "_1"),
      createCeTaskProcessor(SOME_CE_TASK_TYPE + "_2", SOME_CE_TASK_TYPE + "_3"),
    });

    assertThat(underTest.getForCeTask(createCeTask(SOME_CE_TASK_TYPE, SOME_COMPONENT_KEY))).isEmpty();
  }

  @Test
  public void getForTask_returns_TaskProcessor_based_on_CeTask_type_only() {
    CeTaskProcessor taskProcessor = createCeTaskProcessor(SOME_CE_TASK_TYPE);
    CeTaskProcessorRepositoryImpl underTest = new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {taskProcessor});

    assertThat(underTest.getForCeTask(createCeTask(SOME_CE_TASK_TYPE, SOME_COMPONENT_KEY)).get()).isSameAs(taskProcessor);
    assertThat(underTest.getForCeTask(createCeTask(SOME_CE_TASK_TYPE, SOME_COMPONENT_KEY + "2")).get()).isSameAs(taskProcessor);
  }

  @Test
  public void getForTask_returns_TaskProcessor_even_if_it_is_not_specific() {
    CeTaskProcessor taskProcessor = createCeTaskProcessor(SOME_CE_TASK_TYPE + "_1", SOME_CE_TASK_TYPE, SOME_CE_TASK_TYPE + "_3");
    CeTaskProcessorRepositoryImpl underTest = new CeTaskProcessorRepositoryImpl(new CeTaskProcessor[] {taskProcessor});

    assertThat(underTest.getForCeTask(createCeTask(SOME_CE_TASK_TYPE, SOME_COMPONENT_KEY)).get()).isSameAs(taskProcessor);
  }

  private CeTaskProcessor createCeTaskProcessor(final String... ceTaskTypes) {
    return new HandleTypeOnlyTaskProcessor(ceTaskTypes);
  }

  private static CeTask createCeTask(String ceTaskType, String key) {
    CeTask.Component component = new CeTask.Component("uuid_" + key, key, "name_" + key);
    return new CeTask.Builder()
      .setOrganizationUuid("org1")
      .setType(ceTaskType)
      .setUuid("task_uuid_" + key)
      .setComponent(component)
      .setMainComponent(component)
      .build();
  }

  private static class HandleTypeOnlyTaskProcessor implements CeTaskProcessor {
    private final String[] ceTaskTypes;

    public HandleTypeOnlyTaskProcessor(String... ceTaskTypes) {
      this.ceTaskTypes = ceTaskTypes;
    }

    @Override
    public Set<String> getHandledCeTaskTypes() {
      return ImmutableSet.copyOf(ceTaskTypes);
    }

    @Override
    public CeTaskResult process(CeTask task) {
      throw new UnsupportedOperationException("Process is not implemented");
    }
  }

  private static class SomeProcessor1 extends HandleTypeOnlyTaskProcessor {
    public SomeProcessor1(String... ceTaskTypes) {
      super(ceTaskTypes);
    }
  }

  private static class SomeProcessor2 extends HandleTypeOnlyTaskProcessor {
    public SomeProcessor2(String... ceTaskTypes) {
      super(ceTaskTypes);
    }
  }
}
