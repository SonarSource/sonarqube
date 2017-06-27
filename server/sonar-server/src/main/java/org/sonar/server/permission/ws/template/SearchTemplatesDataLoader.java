/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.permission.template.CountByTemplateAndPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.template.DefaultTemplatesResolver.ResolvedDefaultTemplates;
import org.sonarqube.ws.client.permission.SearchTemplatesWsRequest;

import static org.sonar.server.permission.ws.template.SearchTemplatesData.builder;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

public class SearchTemplatesDataLoader {
  private final DbClient dbClient;
  private final DefaultTemplatesResolver defaultTemplatesResolver;

  public SearchTemplatesDataLoader(DbClient dbClient, DefaultTemplatesResolver defaultTemplatesResolver) {
    this.dbClient = dbClient;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
  }

  public SearchTemplatesData load(DbSession dbSession, SearchTemplatesWsRequest request) {
    SearchTemplatesData.Builder data = builder();
    List<PermissionTemplateDto> templates = searchTemplates(dbSession, request);
    List<Long> templateIds = Lists.transform(templates, PermissionTemplateDto::getId);

    DefaultTemplates defaultTemplates = checkFoundWithOptional(
        dbClient.organizationDao().getDefaultTemplates(dbSession, request.getOrganizationUuid()),
        "No Default templates for organization with uuid '%s'", request.getOrganizationUuid());
    ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(defaultTemplates);

    data.templates(templates)
      .defaultTemplates(resolvedDefaultTemplates)
      .userCountByTemplateIdAndPermission(userCountByTemplateIdAndPermission(dbSession, templateIds))
      .groupCountByTemplateIdAndPermission(groupCountByTemplateIdAndPermission(dbSession, templateIds))
      .withProjectCreatorByTemplateIdAndPermission(withProjectCreatorsByTemplateIdAndPermission(dbSession, templateIds));

    return data.build();
  }

  private List<PermissionTemplateDto> searchTemplates(DbSession dbSession, SearchTemplatesWsRequest request) {
    return dbClient.permissionTemplateDao().selectAll(dbSession, request.getOrganizationUuid(), request.getQuery());
  }

  private Table<Long, String, Integer> userCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().usersCountByTemplateIdAndPermission(dbSession, templateIds, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateIdAndPermission;
  }

  private Table<Long, String, Integer> groupCountByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Integer> userCountByTemplateIdAndPermission = TreeBasedTable.create();

    dbClient.permissionTemplateDao().groupsCountByTemplateIdAndPermission(dbSession, templateIds, context -> {
      CountByTemplateAndPermissionDto row = context.getResultObject();
      userCountByTemplateIdAndPermission.put(row.getTemplateId(), row.getPermission(), row.getCount());
    });

    return userCountByTemplateIdAndPermission;
  }

  private Table<Long, String, Boolean> withProjectCreatorsByTemplateIdAndPermission(DbSession dbSession, List<Long> templateIds) {
    final Table<Long, String, Boolean> templatePermissionsByTemplateIdAndPermission = TreeBasedTable.create();

    List<PermissionTemplateCharacteristicDto> templatePermissions = dbClient.permissionTemplateCharacteristicDao().selectByTemplateIds(dbSession, templateIds);
    templatePermissions.stream()
      .forEach(templatePermission -> templatePermissionsByTemplateIdAndPermission.put(templatePermission.getTemplateId(), templatePermission.getPermission(),
        templatePermission.getWithProjectCreator()));

    return templatePermissionsByTemplateIdAndPermission;
  }
}
