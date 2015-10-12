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
package org.sonar.plugins.design.batch;

import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectDsmDecoratorTest {

  @Test
  public void disableOnViews() {
    assertThat(new ProjectDsmDecorator(null).shouldExecuteOnProject(new Project("foo"))).isTrue();

    Project view = mock(Project.class);
    when(view.getScope()).thenReturn(Scopes.PROJECT);
    when(view.getQualifier()).thenReturn(Qualifiers.VIEW);
    assertThat(new ProjectDsmDecorator(null).shouldExecuteOnProject(view)).isFalse();

    Project subview = mock(Project.class);
    when(subview.getScope()).thenReturn(Scopes.PROJECT);
    when(subview.getQualifier()).thenReturn(Qualifiers.SUBVIEW);
    assertThat(new ProjectDsmDecorator(null).shouldExecuteOnProject(subview)).isFalse();
  }

}
