/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.HashBasedTable;
import org.junit.Test;
import org.sonar.server.common.permission.DefaultTemplatesResolverImpl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public class SearchTemplatesDataTest {

  SearchTemplatesData.Builder underTest = SearchTemplatesData.builder()
    .defaultTemplates(new DefaultTemplatesResolverImpl.ResolvedDefaultTemplates("template_uuid", null, null))
    .templates(singletonList(newPermissionTemplateDto()))
    .userCountByTemplateUuidAndPermission(HashBasedTable.create())
    .groupCountByTemplateUuidAndPermission(HashBasedTable.create())
    .withProjectCreatorByTemplateUuidAndPermission(HashBasedTable.create());

  @Test
  public void fail_if_templates_is_null() {
    assertThatThrownBy(() ->  {
      underTest.templates(null);

      underTest.build();
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_default_templates_are_null() {
    assertThatThrownBy(() ->  {
      underTest.defaultTemplates(null);

      underTest.build();
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_user_count_is_null() {
    assertThatThrownBy(() ->  {
      underTest.userCountByTemplateUuidAndPermission(null);

      underTest.build();
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_group_count_is_null() {
    assertThatThrownBy(() ->  {
      underTest.groupCountByTemplateUuidAndPermission(null);

      underTest.build();
    })
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_with_project_creators_is_null() {
    assertThatThrownBy(() ->  {
      underTest.withProjectCreatorByTemplateUuidAndPermission(null);

      underTest.build();
    })
      .isInstanceOf(IllegalStateException.class);
  }
}
