/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util.user;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonarqube.qa.util.Tester;

/**
 * @deprecated replaced by {@link Tester}
 */
@Deprecated
public interface GroupManagement {
  void createGroup(String name);

  void createGroup(String name, @Nullable String description);

  void removeGroups(List<String> groupNames);

  void removeGroups(String... groupNames);

  Optional<Groups.Group> getGroupByName(String name);

  Groups getGroups();

  void verifyUserGroupMembership(String userLogin, String... groups);

  Groups getUserGroups(String userLogin);

  void associateGroupsToUser(String userLogin, String... groups);
}
