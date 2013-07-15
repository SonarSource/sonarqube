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
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.util.RubyUtils;

import java.util.Map;

public class DefaultRubyComponentService implements RubyComponentService {

  private final ResourceDao resourceDao;
  private final DefaultComponentFinder finder;

  public DefaultRubyComponentService(ResourceDao resourceDao, DefaultComponentFinder finder) {
    this.resourceDao = resourceDao;
    this.finder = finder;
  }

  @Override
  public Component findByKey(String key) {
    return resourceDao.findByKey(key);
  }

  public DefaultComponentQueryResult find(Map<String, Object> params) {
    return finder.find(toQuery(params));
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
