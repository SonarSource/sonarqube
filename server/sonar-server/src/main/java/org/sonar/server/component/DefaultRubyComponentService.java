/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.component.Component;
import org.sonar.api.component.RubyComponentService;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.util.RubyUtils;

public class DefaultRubyComponentService implements RubyComponentService {

  private final DbClient dbClient;
  private final ResourceDao resourceDao;
  private final ComponentService componentService;
  private final PermissionTemplateService permissionTemplateService;
  private final FavoriteUpdater favoriteUpdater;

  public DefaultRubyComponentService(DbClient dbClient, ResourceDao resourceDao, ComponentService componentService, PermissionTemplateService permissionTemplateService,
    FavoriteUpdater favoriteUpdater) {
    this.dbClient = dbClient;
    this.resourceDao = resourceDao;
    this.componentService = componentService;
    this.permissionTemplateService = permissionTemplateService;
    this.favoriteUpdater = favoriteUpdater;
  }

  @Override
  @CheckForNull
  public Component findByKey(String key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.componentDao().selectByKey(dbSession, key).orNull();
    }
  }

  // Used in rails
  @CheckForNull
  public Component findByUuid(String uuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.componentDao().selectByUuid(dbSession, uuid).orNull();
    }
  }

  // Used in GOV
  @CheckForNull
  public Long createComponent(String key, String name, String qualifier) {
    return createComponent(key, null, name, qualifier);
  }

  // Used in rails
  @CheckForNull
  public Long createComponent(String key, @Nullable String branch, String name, @Nullable String qualifier) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return createComponent(dbSession, key, branch, name, qualifier);
    }
  }

  public long createComponent(DbSession dbSession, String key, @Nullable String branch, String name, @Nullable String qualifier) {
    ComponentDto provisionedComponent = componentService.create(dbSession, NewComponent.create(key, name).setQualifier(qualifier).setBranch(branch));
    permissionTemplateService.applyDefaultPermissionTemplate(dbSession, provisionedComponent.getKey());
    if (Qualifiers.PROJECT.equals(provisionedComponent.qualifier())
      && permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, provisionedComponent)) {
      favoriteUpdater.add(dbSession, provisionedComponent);
      dbSession.commit();
    }

    return provisionedComponent.getId();
  }

  // Used in GOV
  public List<ResourceDto> findProvisionedProjects(Map<String, Object> params) {
    ComponentQuery query = toQuery(params);
    return resourceDao.selectProvisionedProjects(query.qualifiers());
  }

  static ComponentQuery toQuery(Map<String, Object> props) {
    ComponentQuery.Builder builder = ComponentQuery.builder()
      .keys(RubyUtils.toStrings(props.get("keys")))
      .names(RubyUtils.toStrings(props.get("names")))
      .qualifiers(RubyUtils.toStrings(props.get("qualifiers")))
      .pageSize(RubyUtils.toInteger(props.get("pageSize")))
      .pageIndex(RubyUtils.toInteger(props.get("pageIndex")));
    String sort = (String) props.get("sort");
    if (!Strings.isNullOrEmpty(sort)) {
      builder.sort(sort);
      builder.asc(RubyUtils.toBoolean(props.get("asc")));
    }
    return builder.build();
  }

}
