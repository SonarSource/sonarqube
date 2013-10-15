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
package org.sonar.server.component;

import com.google.common.base.Strings;
import org.sonar.api.component.Component;
import org.sonar.api.component.RubyComponentService;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Scopes;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.util.RubyUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultRubyComponentService implements RubyComponentService {

  private final ResourceDao resourceDao;
  private final DefaultComponentFinder finder;
  private final ResourceIndexerDao resourceIndexerDao;
  private final I18n i18n;

  public DefaultRubyComponentService(ResourceDao resourceDao, DefaultComponentFinder finder, ResourceIndexerDao resourceIndexerDao, I18n i18n) {
    this.resourceDao = resourceDao;
    this.finder = finder;
    this.resourceIndexerDao = resourceIndexerDao;
    this.i18n = i18n;
  }

  @Override
  public Component<?> findByKey(String key) {
    return resourceDao.findByKey(key);
  }

  public void createComponent(String kee, String name, String qualifier) {
    ComponentDto component = (ComponentDto)resourceDao.findByKey(kee);
    if (component != null) {
      throw new BadRequestException(formatMessage("Could not create %s, key already exists: %s", qualifier, kee));
    }
    checkKeyFormat(qualifier, kee);

    resourceDao.insertOrUpdate(
      new ResourceDto()
        .setKey(kee)
        .setName(name)
        .setLongName(name)
        .setScope(Scopes.PROJECT)
        .setQualifier(qualifier)
        .setCreatedAt(new Date()));
    component = (ComponentDto)resourceDao.findByKey(kee);
    if (component == null) {
      throw new BadRequestException(String.format("%s not created: %s", null, kee));
    }
    resourceIndexerDao.indexResource(component.getId());
  }

  public void updateComponent(Long id, String key, String name) {
    ResourceDto resource = resourceDao.getResource(id);
    if (resource == null) {
      throw new NotFoundException();
    }
    checkKeyFormat(resource.getQualifier(), key);

    resourceDao.insertOrUpdate(resource.setKey(key).setName(name));
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

  private void checkKeyFormat(String qualifier, String kee) {
    if (!ComponentKeys.isValidModuleKey(kee)) {
      throw new BadRequestException(formatMessage("Malformed key for %s: %s", qualifier, kee));
    }
  }

  private String formatMessage(String message, String qualifier, String key) {
    return String.format(message, i18n.message(Locale.getDefault(), "qualifier."+qualifier, "Project"), key);
  }
}
