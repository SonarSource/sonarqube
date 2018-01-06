/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class QualifiersTest {

  @Test
  public void testRootView() {
    View root = View.createRootView();
    assertThat(Qualifiers.isView(root, true)).isTrue();
    assertThat(Qualifiers.isView(root, false)).isTrue();
    assertThat(Qualifiers.isProject(root, true)).isFalse();
    assertThat(Qualifiers.isProject(root, false)).isFalse();
  }

  @Test
  public void application() {
    View root = View.createRootApp();
    assertThat(Qualifiers.isView(root, true)).isFalse();
    assertThat(Qualifiers.isView(root, false)).isFalse();
    assertThat(Qualifiers.isProject(root, true)).isFalse();
    assertThat(Qualifiers.isProject(root, false)).isFalse();
  }

  @Test
  public void testSubView() {
    View subview = View.createSubView();
    assertThat(Qualifiers.isView(subview, true)).isTrue();
    assertThat(Qualifiers.isView(subview, false)).isFalse();
    assertThat(Qualifiers.isProject(subview, true)).isFalse();
    assertThat(Qualifiers.isProject(subview, false)).isFalse();
  }

  @Test
  public void testProject() {
    ProjectDefinition rootDef = ProjectDefinition.create();
    ProjectDefinition moduleDef = ProjectDefinition.create();
    rootDef.addSubProject(moduleDef);
    Resource root = new Project(rootDef);
    assertThat(Qualifiers.isView(root, true)).isFalse();
    assertThat(Qualifiers.isView(root, false)).isFalse();
    assertThat(Qualifiers.isProject(root, true)).isTrue();
    assertThat(Qualifiers.isProject(root, false)).isTrue();
  }

  @Test
  public void testModule() {
    ProjectDefinition rootDef = ProjectDefinition.create();
    ProjectDefinition moduleDef = ProjectDefinition.create();
    rootDef.addSubProject(moduleDef);
    Resource sub = new Project(moduleDef);
    assertThat(Qualifiers.isView(sub, true)).isFalse();
    assertThat(Qualifiers.isView(sub, false)).isFalse();
    assertThat(Qualifiers.isProject(sub, true)).isTrue();
    assertThat(Qualifiers.isProject(sub, false)).isFalse();
  }

  private static class View extends Resource {

    private String qualifier;

    private View(String qualifier) {
      this.qualifier = qualifier;
    }

    static View createRootView() {
      return new View(Qualifiers.VIEW);
    }

    static View createRootApp() {
      return new View(Qualifiers.APP);
    }

    static View createSubView() {
      return new View(Qualifiers.SUBVIEW);
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getLongName() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public Language getLanguage() {
      return null;
    }

    @Override
    public String getScope() {
      return Scopes.PROJECT;
    }

    @Override
    public String getQualifier() {
      return qualifier;
    }

    @Override
    public Resource getParent() {
      return null;
    }

    @Override
    public boolean matchFilePattern(String antPattern) {
      return false;
    }
  }
}
