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
package org.sonar.server.component;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.component.Component;
import org.sonar.api.component.RubyComponentService;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.util.RubyUtils;

public class DefaultRubyComponentService implements RubyComponentService {

  private final ResourceDao resourceDao;
  private final DefaultComponentFinder finder;
  private final ComponentService componentService;
  private final PermissionService permissionService;

  public DefaultRubyComponentService(ResourceDao resourceDao, DefaultComponentFinder finder, ComponentService componentService, PermissionService permissionService) {
    this.resourceDao = resourceDao;
    this.finder = finder;
    this.componentService = componentService;
    this.permissionService = permissionService;
  }

  @Override
  @CheckForNull
  public Component findByKey(String key) {
    return resourceDao.selectByKey(key);
  }

  @CheckForNull
  public Component findByUuid(String uuid) {
    Optional<ComponentDto> componentOptional = componentService.getByUuid(uuid);
    return componentOptional.isPresent() ? componentOptional.get() : null;
  }

  /**
   * Be careful when updating this method, it's used by the Views plugin
   */
  @CheckForNull
  public Long createComponent(String key, String name, String qualifier) {
    return createComponent(key, null, name, qualifier);
  }

  @CheckForNull
  public Long createComponent(String key, @Nullable String branch, String name, @Nullable String qualifier) {
    // Sub view should not be created with provisioning. Will be fixed by http://jira.sonarsource.com/browse/VIEWS-296
    if (!Qualifiers.SUBVIEW.equals(qualifier)) {
      ComponentDto componentDto = componentService.create(NewComponent.create(key, name).setQualifier(qualifier).setBranch(branch));
      if (componentDto == null) {
        throw new BadRequestException(String.format("Component not created: %s", key));
      }
      ComponentDto component = (ComponentDto) resourceDao.selectByKey(componentDto.getKey());
      if (component == null) {
        throw new BadRequestException(String.format("Component not created: %s", key));
      }
      permissionService.applyDefaultPermissionTemplate(component.getKey());
      return component.getId();
    }
    return null;
  }

  public DefaultComponentQueryResult find(Map<String, Object> params) {
    ComponentQuery query = toQuery(params);
    List<Component> components = resourceDao.selectProjectsByQualifiers(query.qualifiers());
    return finder.find(query, components);
  }

  public DefaultComponentQueryResult findWithUncompleteProjects(Map<String, Object> params) {
    ComponentQuery query = toQuery(params);
    List<Component> components = resourceDao.selectProjectsIncludingNotCompletedOnesByQualifiers(query.qualifiers());
    return finder.find(query, components);
  }

  public DefaultComponentQueryResult findGhostsProjects(Map<String, Object> params) {
    ComponentQuery query = toQuery(params);
    List<Component> components = resourceDao.selectGhostsProjects(query.qualifiers());
    return finder.find(query, components);
  }

  public List<ResourceDto> findProvisionedProjects(Map<String, Object> params) {
    ComponentQuery query = toQuery(params);
    return resourceDao.selectProvisionedProjects(query.qualifiers());
  }

  public void updateKey(String projectOrModuleKey, String newKey) {
    componentService.updateKey(projectOrModuleKey, newKey);
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(String projectKey, String stringToReplace, String replacementString) {
    return componentService.checkModuleKeysBeforeRenaming(projectKey, stringToReplace, replacementString);
  }

  public void bulkUpdateKey(String projectKey, String stringToReplace, String replacementString) {
    componentService.bulkUpdateKey(projectKey, stringToReplace, replacementString);
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
