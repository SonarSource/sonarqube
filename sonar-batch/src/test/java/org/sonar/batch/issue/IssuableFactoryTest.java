/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.File;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.core.component.ResourceComponent;
import org.sonar.java.api.JavaClass;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuableFactoryTest {

  ModuleIssues moduleIssues = mock(ModuleIssues.class);

  @Test
  public void file_should_be_issuable() throws Exception {
    IssuableFactory factory = new IssuableFactory(moduleIssues);
    Component component = new ResourceComponent(new File("foo/bar.c"));
    Issuable issuable = factory.loadPerspective(Issuable.class, component);

    assertThat(issuable).isNotNull();
    assertThat(issuable.component()).isSameAs(component);
    assertThat(issuable.issues()).isEmpty();
  }

  @Test
  public void project_should_be_issuable() throws Exception {
    IssuableFactory factory = new IssuableFactory(moduleIssues);
    Component component = new ResourceComponent(new Project("Foo"));
    Issuable issuable = factory.loadPerspective(Issuable.class, component);

    assertThat(issuable).isNotNull();
    assertThat(issuable.component()).isSameAs(component);
    assertThat(issuable.issues()).isEmpty();
  }

  @Test
  public void java_file_should_be_issuable() throws Exception {
    IssuableFactory factory = new IssuableFactory(moduleIssues);
    Component component = new ResourceComponent(new JavaFile("bar.Foo"));
    Issuable issuable = factory.loadPerspective(Issuable.class, component);

    assertThat(issuable).isNotNull();
    assertThat(issuable.component()).isSameAs(component);
    assertThat(issuable.issues()).isEmpty();
  }

  @Test
  public void java_class_should_not_be_issuable() throws Exception {
    IssuableFactory factory = new IssuableFactory(moduleIssues);
    Component component = new ResourceComponent(JavaClass.create("bar", "Foo"));
    Issuable issuable = factory.loadPerspective(Issuable.class, component);

    assertThat(issuable).isNull();
  }
}
