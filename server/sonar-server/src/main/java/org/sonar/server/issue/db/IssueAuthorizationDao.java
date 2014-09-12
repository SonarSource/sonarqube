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
import com.google.common.collect.ImmutableList;
import org.apache.ibatis.session.ResultContext;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.db.IssueAuthorizationDto;
import org.sonar.core.issue.db.IssueAuthorizationMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.DbSynchronizationHandler;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.UpsertDto;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class IssueAuthorizationDao extends BaseDao<IssueAuthorizationMapper, IssueAuthorizationDto, String> implements DaoComponent {

  public static final String PROJECT_KEY = "project";

  public IssueAuthorizationDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public IssueAuthorizationDao(System2 system) {
    super(IndexDefinition.ISSUES_AUTHORIZATION, IssueAuthorizationMapper.class, system);
  }

  @Override
  protected IssueAuthorizationDto doGetNullableByKey(DbSession session, String key) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  protected DbSynchronizationHandler getSynchronizationResultHandler(final DbSession session) {
    return new DbSynchronizationHandler() {
      private final Map<String, IssueAuthorizationDto> authorizationDtoMap = new HashMap<String, IssueAuthorizationDto>();

      @Override
      public void handleResult(ResultContext context) {
        Map<String, Object> row = (Map<String, Object>) context.getResultObject();
        String project = (String) row.get("project");
        String user = (String) row.get("user");
        String group = (String) row.get("permission_group");
        Date updatedAt = (Date) row.get("updated_at");
        IssueAuthorizationDto issueAuthorizationDto = authorizationDtoMap.get(project);
        if (issueAuthorizationDto == null) {
          issueAuthorizationDto = new IssueAuthorizationDto()
            .setProject(project)
            .setPermission(UserRole.USER);
          issueAuthorizationDto.setUpdatedAt(updatedAt);
        }
        if (group != null) {
          issueAuthorizationDto.addGroup(group);
        }
        if (user != null) {
          issueAuthorizationDto.addUser(user);
        }
        authorizationDtoMap.put(project, issueAuthorizationDto);
      }

      @Override
      public void enqueueCollected() {
        for (IssueAuthorizationDto authorization : authorizationDtoMap.values()) {
          session.enqueue(new UpsertDto<IssueAuthorizationDto>(getIndexType(), authorization, true));
        }
      }
    };
  }

  @Override
  protected Map getSynchronizationParams(Date date, Map<String, String> params) {
    Map<String, Object> finalParams = super.getSynchronizationParams(date, params);
    finalParams.put("permission", UserRole.USER);
    finalParams.put("anyone", DefaultGroups.ANYONE);
    finalParams.put(PROJECT_KEY, params.get(PROJECT_KEY));
    return finalParams;
  }

  @Override
  public List<IssueAuthorizationDto> findAfterDate(DbSession session, Date date) {

    Map<String, Object> params = newHashMap();
    params.put("date", date);
    params.put("permission", UserRole.USER);
    params.put("anyone", DefaultGroups.ANYONE);

    Map<String, IssueAuthorizationDto> authorizationDtoMap = newHashMap();

    List<Map<String, Object>> rows = session.selectList("org.sonar.core.issue.db.IssueAuthorizationMapper.selectAfterDate", params);
    for (Map<String, Object> row : rows) {
      String project = (String) row.get("project");
      String user = (String) row.get("user");
      String group = (String) row.get("permission_group");
      Date updatedAt = (Date) row.get("updated_at");
      IssueAuthorizationDto issueAuthorizationDto = authorizationDtoMap.get(project);
      if (issueAuthorizationDto == null) {
        issueAuthorizationDto = new IssueAuthorizationDto()
          .setProject(project)
          .setPermission(UserRole.USER);
        issueAuthorizationDto.setUpdatedAt(updatedAt);
      }
      if (group != null) {
        issueAuthorizationDto.addGroup(group);
      }
      if (user != null) {
        issueAuthorizationDto.addUser(user);
      }
      authorizationDtoMap.put(project, issueAuthorizationDto);
    }

    return ImmutableList.<IssueAuthorizationDto>copyOf(authorizationDtoMap.values());
  }

  protected void doDeleteByKey(DbSession session, String key) {
    // Nothing to do on db side, only remove the index (done in BaseDao)
  }

}
