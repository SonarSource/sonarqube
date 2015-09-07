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

package org.sonar.server.permission.ws.template;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.CountByTemplateAndPermissionDto;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.permission.ws.template.DefaultPermissionTemplateFinder.TemplateUuidQualifier;

import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.permission.ws.template.SearchTemplatesData.newBuilder;

public class SearchTemplatesDataLoader {
  private final DbClient dbClient;
  private final DefaultPermissionTemplateFinder defaultPermissionTemplateFinder;

  public SearchTemplatesDataLoader(DbClient dbClient, DefaultPermissionTemplateFinder defaultPermissionTemplateFinder) {
    this.dbClient = dbClient;
    this.defaultPermissionTemplateFinder = defaultPermissionTemplateFinder;
  }

  public SearchTemplatesData load(Request wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchTemplatesData.Builder data = newBuilder();
      List<PermissionTemplateDto> templates = searchTemplates(dbSession, wsRequest);
      List<Long> templateIds = Lists.transform(templates, TemplateToIdFunction.INSTANCE);
      List<TemplateUuidQualifier> defaultTemplates = defaultPermissionTemplateFinder.getDefaultTemplatesByQualifier();

      data.templates(templates)
        .defaultTemplates(defaultTemplates)
        .userCountByTemplateIdAndPermission(userCountByTemplateIdAndPermission(dbSession, templateIds))
        .groupCountByTemplateIdAndPermission(groupCountByTemplateIdAndPermission(dbSession, templateIds));

      return data.build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private List<PermissionTemplateDto> searchTemplates(DbSession dbSession, Request wsRequest) {
    String nameMatch = wsRequest.param(TEXT_QUERY);

    return nameMatch == null ?
      dbClient.permissionTemplateDao().selectAll(dbSession)
      : dbClient.permissionTemplateDao().selectAll(dbSession, nameMatch);
  }

  private Table<Long, String, Integer> userCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().usersCountByTemplateIdAndPermission(dbSession, templateIds, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        CountByTemplateAndPermissionDto row = (CountByTemplateAndPermissionDto) context.getResultObject();
        userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
      }
    });

    return userCountByTemplateIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().groupsCountByTemplateIdAndPermission(dbSession, templateIds, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        CountByTemplateAndPermissionDto row = (CountByTemplateAndPermissionDto) context.getResultObject();
        userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
      }
    });

    return userCountByTemplateIdAndPermission;
  }

  private enum TemplateToIdFunction implements Function<PermissionTemplateDto, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull PermissionTemplateDto template) {
      return template.getId();
    }
  }
}
