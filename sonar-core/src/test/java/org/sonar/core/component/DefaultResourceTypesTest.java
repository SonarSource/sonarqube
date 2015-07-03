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
package org.sonar.core.component;

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResourceTypesTest {
  @Test
  public void provide_types() {
    ResourceTypeTree tree = DefaultResourceTypes.get();

    assertThat(tree.getTypes()).hasSize(7);
    assertThat(tree.getChildren(Qualifiers.PROJECT)).containsExactly(Qualifiers.MODULE);
  }

  @Test
  public void projects_should_be_available_for_global_widgets() {
    ResourceTypeTree tree = DefaultResourceTypes.get();

    ResourceType projectResourceType = tree.getTypes().get(0);

    assertThat(projectResourceType.getQualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(projectResourceType.getBooleanProperty("supportsGlobalDashboards")).isTrue();
  }
}
