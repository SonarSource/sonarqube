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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Set;
import org.junit.Test;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentsWithUnprocessedIssuesTest {


  ComponentsWithUnprocessedIssues sut = new ComponentsWithUnprocessedIssues();

  @Test
  public void set_uuids() {
    sut.setUuids(newHashSet("ABCD", "EFGH"));

    assertThat(sut.getUuids()).containsOnly("ABCD", "EFGH");
  }

  @Test
  public void set_uuids_makes_a_copy_of_input_issues() {
    Set<String> issues = newHashSet("ABCD", "EFGH");
    sut.setUuids(issues);

    assertThat(sut.getUuids()).containsOnly("ABCD", "EFGH");

    // Remove a element from the list, number of issues from the queue should remain the same
    issues.remove("ABCD");
    assertThat(sut.getUuids()).containsOnly("ABCD", "EFGH");
  }

  @Test
  public void fail_with_NPE_when_setting_null_uuids() {
    assertThatThrownBy(() -> sut.setUuids(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Uuids cannot be null");
  }

  @Test
  public void fail_with_ISE_when_setting_uuids_twice() {
    assertThatThrownBy(() -> {
      sut.setUuids(newHashSet("ABCD"));
      sut.setUuids(newHashSet("EFGH"));
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Uuids have already been initialized");
  }

  @Test
  public void remove_uuid() {
    sut.setUuids(newHashSet("ABCD", "EFGH"));
    sut.remove("ABCD");

    assertThat(sut.getUuids()).containsOnly("EFGH");
  }

  @Test
  public void fail_with_ISE_when_removing_uuid_and_not_initialized() {
    assertThatThrownBy(() -> sut.remove("ABCD"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Uuids have not been initialized yet");
  }

  @Test
  public void fail_with_ISE_when_getting_uuid_and_not_initialized() {
    assertThatThrownBy(() -> sut.getUuids())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Uuids have not been initialized yet");
  }
}
