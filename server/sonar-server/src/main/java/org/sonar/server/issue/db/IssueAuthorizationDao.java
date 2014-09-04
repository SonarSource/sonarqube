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
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.IssueAuthorizationDto;
import org.sonar.core.issue.db.IssueAuthorizationMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.IndexDefinition;

import java.util.Date;

public class IssueAuthorizationDao extends BaseDao<IssueAuthorizationMapper, IssueAuthorizationDto, String> implements DaoComponent {

  public IssueAuthorizationDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public IssueAuthorizationDao(System2 system) {
    super(IndexDefinition.ISSUES_AUTHORIZATION, IssueAuthorizationMapper.class, system);
  }

  @Override
  protected IssueAuthorizationDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected IssueAuthorizationDto doUpdate(DbSession session, IssueAuthorizationDto issueAuthorization) {
    // TODO ?
    // mapper(session).update(issueAuthorization);
    return issueAuthorization;
  }

  @Override
  protected IssueAuthorizationDto doInsert(DbSession session, IssueAuthorizationDto issueAuthorization) {
    // TODO ?
    // Preconditions.checkNotNull(issueAuthorization.getKey(), "Cannot insert IssueAuthorization with empty key!");
    // Preconditions.checkNotNull(issueAuthorization.getPermission(), "Cannot insert IssueAuthorization with no permission!");
    // mapper(session).insert(issueAuthorization);
    return issueAuthorization;
  }

  @Override
  protected Iterable<IssueAuthorizationDto> findAfterDate(DbSession session, Date date) {
    // TODO ?
    // return mapper(session).selectAfterDate(new Timestamp(date.getTime()));
    return null;
  }
}
