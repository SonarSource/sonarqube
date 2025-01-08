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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

@RunWith(DataProviderRunner.class)
public class ProjectLifeCycleListenersImplTest {

  private final ProjectLifeCycleListener listener1 = mock(ProjectLifeCycleListener.class);
  private final ProjectLifeCycleListener listener2 = mock(ProjectLifeCycleListener.class);
  private final ProjectLifeCycleListener listener3 = mock(ProjectLifeCycleListener.class);
  private final ProjectLifeCycleListenersImpl underTestNoListeners = new ProjectLifeCycleListenersImpl();
  private final ProjectLifeCycleListenersImpl underTestWithListeners = new ProjectLifeCycleListenersImpl(
    new ProjectLifeCycleListener[] {listener1, listener2, listener3});

  @Test
  public void onProjectsDeleted_throws_NPE_if_set_is_null() {
    assertThatThrownBy(() -> underTestWithListeners.onProjectsDeleted(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("projects can't be null");
  }

  @Test
  public void onProjectsDeleted_throws_NPE_if_set_is_null_even_if_no_listeners() {
    assertThatThrownBy(() -> underTestNoListeners.onProjectsDeleted(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("projects can't be null");
  }

  @Test
  public void onProjectsDeleted_has_no_effect_if_set_is_empty() {
    underTestNoListeners.onProjectsDeleted(Collections.emptySet());

    underTestWithListeners.onProjectsDeleted(Collections.emptySet());
    verifyNoInteractions(listener1, listener2, listener3);
  }

  @Test
  @UseDataProvider("oneOrManyDeletedProjects")
  public void onProjectsDeleted_does_not_fail_if_there_is_no_listener(Set<DeletedProject> projects) {
    assertThatCode(() -> underTestNoListeners.onProjectsDeleted(projects)).doesNotThrowAnyException();
  }

  @Test
  @UseDataProvider("oneOrManyDeletedProjects")
  public void onProjectsDeleted_calls_all_listeners_in_order_of_addition_to_constructor(Set<DeletedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyDeletedProjects")
  public void onProjectsDeleted_calls_all_listeners_even_if_one_throws_an_Exception(Set<DeletedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new RuntimeException("Faking listener2 throwing an exception"))
      .when(listener2)
      .onProjectsDeleted(any());

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyDeletedProjects")
  public void onProjectsDeleted_calls_all_listeners_even_if_one_throws_an_Error(Set<DeletedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new Error("Faking listener2 throwing an Error"))
      .when(listener2)
      .onProjectsDeleted(any());

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void onProjectBranchesChanged_throws_NPE_if_set_is_null() {
    assertThatThrownBy(() -> underTestWithListeners.onProjectBranchesChanged(null, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("projects can't be null");
  }

  @Test
  public void onProjectBranchesChanged_throws_NPE_if_set_is_null_even_if_no_listeners() {
    assertThatThrownBy(() -> underTestNoListeners.onProjectBranchesChanged(null, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("projects can't be null");
  }

  @Test
  public void onProjectBranchesChanged_has_no_effect_if_set_is_empty() {
    underTestNoListeners.onProjectBranchesChanged(Collections.emptySet(), emptySet());

    underTestWithListeners.onProjectBranchesChanged(Collections.emptySet(), emptySet());
    verifyNoInteractions(listener1, listener2, listener3);
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectBranchesChanged_does_not_fail_if_there_is_no_listener(Set<Project> projects) {
     assertThatNoException().isThrownBy(()-> underTestNoListeners.onProjectBranchesChanged(projects, emptySet()));
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectBranchesChanged_calls_all_listeners_in_order_of_addition_to_constructor(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);

    underTestWithListeners.onProjectBranchesChanged(projects, emptySet());

    inOrder.verify(listener1).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener2).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener3).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectBranchesChanged_calls_all_listeners_even_if_one_throws_an_Exception(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new RuntimeException("Faking listener2 throwing an exception"))
      .when(listener2)
      .onProjectBranchesChanged(any(), anySet());

    underTestWithListeners.onProjectBranchesChanged(projects, emptySet());

    inOrder.verify(listener1).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener2).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener3).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectBranchesChanged_calls_all_listeners_even_if_one_throws_an_Error(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new Error("Faking listener2 throwing an Error"))
      .when(listener2)
      .onProjectBranchesChanged(any(), anySet());

    underTestWithListeners.onProjectBranchesChanged(projects, emptySet());

    inOrder.verify(listener1).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener2).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verify(listener3).onProjectBranchesChanged(same(projects), eq(emptySet()));
    inOrder.verifyNoMoreInteractions();
  }

  @DataProvider
  public static Object[][] oneOrManyProjects() {
    return new Object[][] {
      {singleton(newUniqueProject())},
      {IntStream.range(0, 1 + new Random().nextInt(10)).mapToObj(i -> newUniqueProject()).collect(Collectors.toSet())}
    };
  }

  @DataProvider
  public static Object[][] oneOrManyDeletedProjects() {
    return new Object[][] {
      {singleton(newUniqueProject())},
      {IntStream.range(0, 1 + new Random().nextInt(10)).mapToObj(i -> new DeletedProject(newUniqueProject(), "branch_" + i))
        .collect(Collectors.toSet())}
    };
  }

  @Test
  public void onProjectsRekeyed_throws_NPE_if_set_is_null() {
    assertThatThrownBy(() -> underTestWithListeners.onProjectsRekeyed(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("rekeyedProjects can't be null");
  }

  @Test
  public void onProjectsRekeyed_throws_NPE_if_set_is_null_even_if_no_listeners() {
    assertThatThrownBy(() -> underTestNoListeners.onProjectsRekeyed(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("rekeyedProjects can't be null");
  }

  @Test
  public void onProjectsRekeyed_has_no_effect_if_set_is_empty() {
    underTestNoListeners.onProjectsRekeyed(Collections.emptySet());

    underTestWithListeners.onProjectsRekeyed(Collections.emptySet());
    verifyNoInteractions(listener1, listener2, listener3);
  }

  @Test
  @UseDataProvider("oneOrManyRekeyedProjects")
  public void onProjectsRekeyed_does_not_fail_if_there_is_no_listener(Set<RekeyedProject> projects) {
    assertThatNoException().isThrownBy(() -> underTestNoListeners.onProjectsRekeyed(projects));
  }

  @Test
  @UseDataProvider("oneOrManyRekeyedProjects")
  public void onProjectsRekeyed_calls_all_listeners_in_order_of_addition_to_constructor(Set<RekeyedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);

    underTestWithListeners.onProjectsRekeyed(projects);

    inOrder.verify(listener1).onProjectsRekeyed(same(projects));
    inOrder.verify(listener2).onProjectsRekeyed(same(projects));
    inOrder.verify(listener3).onProjectsRekeyed(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyRekeyedProjects")
  public void onProjectsRekeyed_calls_all_listeners_even_if_one_throws_an_Exception(Set<RekeyedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new RuntimeException("Faking listener2 throwing an exception"))
      .when(listener2)
      .onProjectsRekeyed(any());

    underTestWithListeners.onProjectsRekeyed(projects);

    inOrder.verify(listener1).onProjectsRekeyed(same(projects));
    inOrder.verify(listener2).onProjectsRekeyed(same(projects));
    inOrder.verify(listener3).onProjectsRekeyed(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyRekeyedProjects")
  public void onProjectsRekeyed_calls_all_listeners_even_if_one_throws_an_Error(Set<RekeyedProject> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new Error("Faking listener2 throwing an Error"))
      .when(listener2)
      .onProjectsRekeyed(any());

    underTestWithListeners.onProjectsRekeyed(projects);

    inOrder.verify(listener1).onProjectsRekeyed(same(projects));
    inOrder.verify(listener2).onProjectsRekeyed(same(projects));
    inOrder.verify(listener3).onProjectsRekeyed(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @DataProvider
  public static Object[][] oneOrManyRekeyedProjects() {
    return new Object[][] {
      {singleton(newUniqueRekeyedProject())},
      {IntStream.range(0, 1 + new Random().nextInt(10)).mapToObj(i -> newUniqueRekeyedProject()).collect(Collectors.toSet())}
    };
  }

  private static Project newUniqueProject() {
    return Project.from(newPrivateProjectDto());
  }

  private static int counter = 3_989;

  private static RekeyedProject newUniqueRekeyedProject() {
    int base = counter++;
    Project project = Project.from(newPrivateProjectDto());
    return new RekeyedProject(project, base + "_old_key");
  }
}
