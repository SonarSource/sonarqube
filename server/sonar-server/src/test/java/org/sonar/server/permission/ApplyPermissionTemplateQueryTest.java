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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplyPermissionTemplateQueryTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_populate_with_params() throws Exception {

    Map<String, Object> params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", Lists.newArrayList("1", "2", "3"));

    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);

    assertThat(query.getTemplateKey()).isEqualTo("my_template_key");
    assertThat(query.getSelectedComponents()).containsOnly("1", "2", "3");
  }

  @Test
  public void should_invalidate_query_with_empty_name() throws Exception {

    throwable.expect(BadRequestException.class);
    throwable.expectMessage("Permission template is mandatory");

    Map<String, Object> params = Maps.newHashMap();
    params.put("template_key", "");
    params.put("components", Lists.newArrayList("1", "2", "3"));

    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    query.validate();
  }

  @Test
  public void should_invalidate_query_with_no_components() throws Exception {

    throwable.expect(BadRequestException.class);
    throwable.expectMessage("Please provide at least one entry to which the permission template should be applied");

    Map<String, Object> params = Maps.newHashMap();
    params.put("template_key", "my_template_key");
    params.put("components", Collections.EMPTY_LIST);

    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    query.validate();
  }
}
