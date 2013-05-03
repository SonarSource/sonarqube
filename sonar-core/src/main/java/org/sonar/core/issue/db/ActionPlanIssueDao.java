/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class ActionPlanIssueDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public ActionPlanIssueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public Collection<ActionPlanIssueDto> findByIssueIds(Collection<Long> issueIds, SqlSession session) {
    if (issueIds.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      List<List<Long>> idsPartition = Lists.partition(newArrayList(issueIds), 1000);
      return session.getMapper(ActionPlanIssueMapper.class).findByIssueIds(idsPartition);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  Collection<ActionPlanIssueDto> findByIssueIds(Collection<Long> issueIds) {
    SqlSession session = mybatis.openSession();
    try {
      return findByIssueIds(issueIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
