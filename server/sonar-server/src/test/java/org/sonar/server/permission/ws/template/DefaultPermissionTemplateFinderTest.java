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

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.server.permission.ws.template.DefaultPermissionTemplateFinder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;

public class DefaultPermissionTemplateFinderTest {

  ResourceTypes resourceTypes = mock(ResourceTypes.class);
  Settings settings = new Settings();

  DefaultPermissionTemplateFinder underTest;

  @Before
  public void setUp() {
    underTest = new DefaultPermissionTemplateFinder(settings, resourceTypes);
    settings
      .setProperty(DEFAULT_TEMPLATE_PROPERTY, "default-template-uuid")
      .setProperty(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT), "default-project-template-uuid")
      .setProperty(defaultRootQualifierTemplateProperty("DEV"), "default-dev-template-uuid")
      .setProperty(defaultRootQualifierTemplateProperty(Qualifiers.VIEW), "default-view-template-uuid");
    when(resourceTypes.getRoots()).thenReturn(rootResourceTypes());
  }

  @Test
  public void get_default_template_uuids_in_settings() {
    Set<String> result = underTest.getDefaultTemplateUuids();

    assertThat(result).containsOnly("default-project-template-uuid", "default-view-template-uuid", "default-dev-template-uuid");
  }

  @Test
  public void get_default_template_uuid_if_no_property() {
    settings = new Settings();
    settings.setProperty(DEFAULT_TEMPLATE_PROPERTY, "default-template-uuid");
    underTest = new DefaultPermissionTemplateFinder(settings, resourceTypes);

    Set<String> result = underTest.getDefaultTemplateUuids();

    assertThat(result).containsOnly("default-template-uuid");
  }

  private static List<ResourceType> rootResourceTypes() {
    ResourceType project = ResourceType.builder(Qualifiers.PROJECT).build();
    ResourceType view = ResourceType.builder(Qualifiers.VIEW).build();
    ResourceType dev = ResourceType.builder("DEV").build();

    return asList(project, view, dev);
  }
}
