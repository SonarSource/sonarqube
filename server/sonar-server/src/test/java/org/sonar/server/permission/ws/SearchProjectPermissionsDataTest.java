/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.permission.ws;

import com.google.common.collect.HashBasedTable;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentDto;

public class SearchProjectPermissionsDataTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fail_if_no_projects() {
    expectedException.expect(IllegalStateException.class);

    SearchProjectPermissionsData.newBuilder()
      .groupCountByProjectIdAndPermission(HashBasedTable.<Long, String, Integer>create())
      .userCountByProjectIdAndPermission(HashBasedTable.<Long, String, Integer>create())
      .build();
  }

  @Test
  public void fail_if_no_group_count() {
    expectedException.expect(IllegalStateException.class);

    SearchProjectPermissionsData.newBuilder()
      .rootComponents(Collections.<ComponentDto>emptyList())
      .userCountByProjectIdAndPermission(HashBasedTable.<Long, String, Integer>create())
      .build();
  }

  @Test
  public void fail_if_no_user_count() {
    expectedException.expect(IllegalStateException.class);

    SearchProjectPermissionsData.newBuilder()
      .rootComponents(Collections.<ComponentDto>emptyList())
      .groupCountByProjectIdAndPermission(HashBasedTable.<Long, String, Integer>create())
      .build();
  }
}
