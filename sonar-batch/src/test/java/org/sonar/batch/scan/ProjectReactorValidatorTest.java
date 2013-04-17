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
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.SonarException;

public class ProjectReactorValidatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private ProjectReactorValidator validator;

  @Before
  public void prepare() {
    validator = new ProjectReactorValidator();
  }

  @Test
  public void should_not_fail_with_valid_data() {
    ProjectReactor reactor = createProjectReactor("foo");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_alphanumeric() {
    ProjectReactor reactor = createProjectReactor("Foobar2");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_dot() {
    ProjectReactor reactor = createProjectReactor("foo.bar");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_dash() {
    ProjectReactor reactor = createProjectReactor("foo-bar");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_colon() {
    ProjectReactor reactor = createProjectReactor("foo:bar");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_underscore() {
    ProjectReactor reactor = createProjectReactor("foo_bar");
    validator.validate(reactor);
  }

  @Test
  public void should_fail_with_invalid_key() {
    String projectKey = "foo$bar";
    ProjectReactor reactor = createProjectReactor(projectKey);

    thrown.expect(SonarException.class);
    thrown.expectMessage("foo$bar is not a valid project or module key");
    validator.validate(reactor);
  }

  private ProjectReactor createProjectReactor(String projectKey) {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    ProjectReactor reactor = new ProjectReactor(def);
    return reactor;
  }

}
