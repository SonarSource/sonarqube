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

package org.sonar.server.permission;

import org.junit.Test;
import org.sonar.db.permission.PermissionQuery;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;

public class PermissionQueryParserTest {

  @Test
  public void to_query_with_all_params() {
    Map<String, Object> params = newHashMap();
    params.put("permission", "admin");
    params.put("template", "my_template_key");
    params.put("component", "org.sample.Sample");
    params.put("query", "text");
    params.put("selected", "all");
    params.put("page", 2);
    params.put("pageSize", 50);
    PermissionQuery query = PermissionQueryParser.toQuery(params);

    assertThat(query.permission()).isEqualTo("admin");
    assertThat(query.component()).isEqualTo("org.sample.Sample");
    assertThat(query.template()).isEqualTo("my_template_key");
    assertThat(query.search()).isEqualTo("text");
    assertThat(query.pageSize()).isEqualTo(50);
    assertThat(query.pageIndex()).isEqualTo(2);
    assertThat(query.membership()).isEqualTo(PermissionQuery.ANY);
  }

  @Test
  public void to_query_with_include_membership_parameter() {
    Map<String, Object> params = newHashMap();
    params.put("permission", "admin");
    params.put("selected", "selected");

    assertThat(PermissionQueryParser.toQuery(params).membership()).isEqualTo(PermissionQuery.IN);
  }

  @Test
  public void to_query_with_exclude_membership_parameter() {
    Map<String, Object> params = newHashMap();
    params.put("permission", "admin");
    params.put("selected", "deselected");

    assertThat(PermissionQueryParser.toQuery(params).membership()).isEqualTo(PermissionQuery.OUT);
  }

  @Test
  public void to_query_with_any_membership_parameter() {
    Map<String, Object> params = newHashMap();
    params.put("permission", "admin");
    params.put("selected", "all");

    assertThat(PermissionQueryParser.toQuery(params).membership()).isEqualTo(PermissionQuery.ANY);
  }

}
