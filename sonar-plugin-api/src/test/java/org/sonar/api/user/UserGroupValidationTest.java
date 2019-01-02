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
package org.sonar.api.user;

import com.google.common.base.Strings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UserGroupValidationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void fail_when_group_name_is_Anyone() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Anyone group cannot be used");

    UserGroupValidation.validateGroupName("AnyOne");
  }

  @Test
  public void fail_when_group_name_is_empty() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Group name cannot be empty");

    UserGroupValidation.validateGroupName("");
  }

  @Test
  public void fail_when_group_name_contains_only_blank() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Group name cannot be empty");

    UserGroupValidation.validateGroupName("     ");
  }

  @Test
  public void fail_when_group_name_is_too_big() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Group name cannot be longer than 255 characters");

    UserGroupValidation.validateGroupName(Strings.repeat("name", 300));
  }

  @Test
  public void fail_when_group_name_is_null() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Group name cannot be empty");

    UserGroupValidation.validateGroupName(null);
  }
}
