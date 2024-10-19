/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.issue;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

public interface IssueChangeMapper {

  void insert(IssueChangeDto dto);

  int delete(String key);

  void deleteByUuids(@Param("changeUuids") Collection<String> uuids);

  int update(IssueChangeDto change);

  @CheckForNull
  IssueChangeDto selectByKeyAndType(@Param("key") String key, @Param("changeType") String type);

  /**
   * Issue changes by chronological date of creation
   */
  List<IssueChangeDto> selectByIssuesAndType(@Param("issueKeys") List<String> issueKeys, @Param("changeType") String changeType);

  /**
   * Scrolls through all changes with type {@link IssueChangeDto#TYPE_FIELD_CHANGE diff}, sorted by issue key and
   * then change creation date.
   */
  void scrollDiffChangesOfIssues(@Param("issueKeys") List<String> issueKeys, ResultHandler<IssueChangeDto> handler);

  List<IssueChangeDto> selectByIssues(@Param("issueKeys") List<String> issueKeys);
}
