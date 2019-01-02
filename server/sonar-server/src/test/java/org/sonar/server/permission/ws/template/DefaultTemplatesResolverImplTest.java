/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.organization.DefaultTemplates;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTemplatesResolverImplTest {

  private static final ResourceTypes RESOURCE_TYPES_WITHOUT_VIEWS = new ResourceTypes(new ResourceTypeTree[] {
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()
  });
  private static final ResourceTypes RESOURCE_TYPES_WITH_VIEWS = new ResourceTypes(new ResourceTypeTree[] {
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build(),
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.VIEW).build()).build()
  });
  private DefaultTemplatesResolverImpl underTestWithoutViews = new DefaultTemplatesResolverImpl(RESOURCE_TYPES_WITHOUT_VIEWS);
  private DefaultTemplatesResolverImpl underTestWithViews = new DefaultTemplatesResolverImpl(RESOURCE_TYPES_WITH_VIEWS);

  @Test
  public void project_is_project_of_DefaultTemplates_no_matter_if_views_is_installed() {
    Stream.of(
      new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid(null),
      new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid("bar")).forEach(
        defaultTemplates -> {
          assertThat(underTestWithoutViews.resolve(defaultTemplates).getProject()).isEqualTo("foo");
          assertThat(underTestWithViews.resolve(defaultTemplates).getProject()).isEqualTo("foo");
        });
  }

  @Test
  public void view_is_empty_no_matter_view_in_DefaultTemplates_if_views_is_not_installed() {
    DefaultTemplates defaultTemplatesNoView = new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid(null);
    DefaultTemplates defaultTemplatesView = new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid("bar");

    assertThat(underTestWithoutViews.resolve(defaultTemplatesNoView).getApplication()).isEmpty();
    assertThat(underTestWithoutViews.resolve(defaultTemplatesView).getApplication()).isEmpty();
  }

  @Test
  public void view_is_project_of_DefaultTemplates_if_view_in_DefaultTemplates_is_null_and_views_is_installed() {
    DefaultTemplates defaultTemplates = new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid(null);

    assertThat(underTestWithViews.resolve(defaultTemplates).getApplication()).contains("foo");
  }

  @Test
  public void view_is_view_of_DefaultTemplates_if_view_in_DefaultTemplates_is_not_null_and_views_is_installed() {
    DefaultTemplates defaultTemplates = new DefaultTemplates().setProjectUuid("foo").setApplicationsUuid("bar");

    assertThat(underTestWithViews.resolve(defaultTemplates).getApplication()).contains("bar");
  }
}
