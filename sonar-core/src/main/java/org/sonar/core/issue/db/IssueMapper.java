/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue.db;

import org.apache.ibatis.annotations.Param;
import org.sonar.api.issue.IssueQuery;
import org.sonar.core.rule.RuleDto;

import javax.annotation.Nullable;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface IssueMapper {

  IssueDto selectByKey(String key);

  List<IssueDto> selectByKeys(Collection<String> keys);

  /**
   * Return a paginated list of authorized issue ids for a user.
   * If the role is null, then the authorisation check is disabled.
   */
  List<IssueDto> selectIssueIds(@Param("query") IssueQuery query, @Param("componentRootKeys") Collection<String> componentRootKeys,
    @Nullable @Param("userId") Integer userId, @Nullable @Param("role") String role,
    @Param("maxResults") Integer maxResult);

  /**
   * Return a none paginated list of authorized issues for a user.
   * If the role is null, then the authorisation check is disabled.
   */
  List<IssueDto> selectIssues(@Param("query") IssueQuery query, @Param("componentRootKeys") Collection<String> componentRootKeys,
    @Nullable @Param("userId") Integer userId, @Nullable @Param("role") String role);

  List<RuleDto> findRulesByComponent(@Param("componentKey") String componentKey, @Nullable @Param("createdAt") Date createdAtOrAfter);

  List<String> findSeveritiesByComponent(@Param("componentKey") String componentKey, @Nullable @Param("createdAt") Date createdAtOrAfter);

  void insert(IssueDto issue);

  int update(IssueDto issue);

  int updateIfBeforeSelectedDate(IssueDto issue);

  List<IssueDto> selectAfterDate(@Param("date") Timestamp timestamp, @Nullable @Param("project") String project);
}
