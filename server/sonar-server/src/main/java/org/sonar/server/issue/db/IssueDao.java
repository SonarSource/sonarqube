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
package org.sonar.server.issue.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.IndexDefinition;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class IssueDao extends BaseDao<IssueMapper, IssueDto, String> implements DaoComponent {

  public static final String PROJECT_KEY = "project";

  public IssueDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public IssueDao(System2 system) {
    super(IndexDefinition.ISSUES, IssueMapper.class, system);
  }

  @Override
  protected IssueDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  protected List<IssueDto> doGetByKeys(DbSession session, Collection<String> keys) {
    return mapper(session).selectByKeys(keys);
  }

  @Override
  protected IssueDto doUpdate(DbSession session, IssueDto issue) {
    mapper(session).update(issue);
    return issue;
  }

  @Override
  protected IssueDto doInsert(DbSession session, IssueDto issue) {
    Preconditions.checkNotNull(issue.getKey(), "Cannot insert Issue with empty key!");
    Preconditions.checkNotNull(issue.getComponentId(), "Cannot insert Issue with no Component!");
    mapper(session).insert(issue);
    return issue;
  }

  @Override
  protected String getSynchronizationStatementName() {
    return "selectAfterDate";
  }

  @Override
  protected Map<String, Object> getSynchronizationParams(Date date, Map<String, String> params) {
    Map<String, Object> finalParams = super.getSynchronizationParams(date, params);
    finalParams.put(PROJECT_KEY, params.get(PROJECT_KEY));
    return finalParams;
  }
}
