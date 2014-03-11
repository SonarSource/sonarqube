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
package org.sonar.api.resources;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class QualifiersTest {

  @Test
  public void testRootView() {
    View root = View.createRootView();
    assertThat(Qualifiers.isView(root, true), is(true));
    assertThat(Qualifiers.isView(root, false), is(true));
    assertThat(Qualifiers.isProject(root, true), is(false));
    assertThat(Qualifiers.isProject(root, false), is(false));
  }

  @Test
  public void testSubView() {
    View subview = View.createSubView();
    assertThat(Qualifiers.isView(subview, true), is(true));
    assertThat(Qualifiers.isView(subview, false), is(false));
    assertThat(Qualifiers.isProject(subview, true), is(false));
    assertThat(Qualifiers.isProject(subview, false), is(false));
  }

  @Test
  public void testProject() {
    Resource root = new Project("foo");
    assertThat(Qualifiers.isView(root, true), is(false));
    assertThat(Qualifiers.isView(root, false), is(false));
    assertThat(Qualifiers.isProject(root, true), is(true));
    assertThat(Qualifiers.isProject(root, false), is(true));
  }

  @Test
  public void testModule() {
    Resource sub = new Project("sub").setParent(new Project("root"));
    assertThat(Qualifiers.isView(sub, true), is(false));
    assertThat(Qualifiers.isView(sub, false), is(false));
    assertThat(Qualifiers.isProject(sub, true), is(true));
    assertThat(Qualifiers.isProject(sub, false), is(false));
  }

  private static class View extends Resource {

    private String qualifier;

    private View(String qualifier) {
      this.qualifier = qualifier;
    }

    static View createRootView() {
      return new View(Qualifiers.VIEW);
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


