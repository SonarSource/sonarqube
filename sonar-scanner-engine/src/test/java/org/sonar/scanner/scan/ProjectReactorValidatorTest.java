/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.analysis.DefaultAnalysisMode;

public class ProjectReactorValidatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ProjectReactorValidator validator;
  private DefaultAnalysisMode mode;

  @Before
  public void prepare() {
    mode = mock(DefaultAnalysisMode.class);
    validator = new ProjectReactorValidator(mode);
  }

  @Test
  public void not_fail_with_valid_key() {
    validator.validate(createProjectReactor("foo"));
    validator.validate(createProjectReactor("123foo"));
    validator.validate(createProjectReactor("foo123"));
    validator.validate(createProjectReactor("1Z3"));
    validator.validate(createProjectReactor("a123"));
    validator.validate(createProjectReactor("123a"));
    validator.validate(createProjectReactor("1:2"));
    validator.validate(createProjectReactor("3-3"));
    validator.validate(createProjectReactor("-:"));
  }

  @Test
  public void allow_slash_issues_mode() {
    when(mode.isIssues()).thenReturn(true);
    validator.validate(createProjectReactor("project/key"));

    when(mode.isIssues()).thenReturn(false);
    thrown.expect(MessageException.class);
    thrown.expectMessage("is not a valid project or module key");
    validator.validate(createProjectReactor("project/key"));
  }

  @Test
  public void not_fail_with_alphanumeric_key() {
    ProjectReactor reactor = createProjectReactor("Foobar2");
    validator.validate(reactor);
  }

  @Test
  public void should_not_fail_with_dot_key() {
    ProjectReactor reactor = createProjectReactor("foo.bar");
    validator.validate(reactor);
  }

  @Test
  public void not_fail_with_dash_key() {
    ProjectReactor reactor = createProjectReactor("foo-bar");
    validator.validate(reactor);
  }

  @Test
  public void not_fail_with_colon_key() {
    ProjectReactor reactor = createProjectReactor("foo:bar");
    validator.validate(reactor);
  }

  @Test
  public void not_fail_with_underscore_key() {
    ProjectReactor reactor = createProjectReactor("foo_bar");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_invalid_key() {
    ProjectReactor reactor = createProjectReactor("foo$bar");

    thrown.expect(MessageException.class);
    thrown.expectMessage("\"foo$bar\" is not a valid project or module key");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_backslash_in_key() {
    ProjectReactor reactor = createProjectReactor("foo\\bar");

    thrown.expect(MessageException.class);
    thrown.expectMessage("\"foo\\bar\" is not a valid project or module key");
    validator.validate(reactor);
  }

  @Test
  public void not_fail_with_valid_branch() {
    validator.validate(createProjectReactor("foo", "branch"));
    validator.validate(createProjectReactor("foo", "Branch2"));
    validator.validate(createProjectReactor("foo", "bra.nch"));
    validator.validate(createProjectReactor("foo", "bra-nch"));
    validator.validate(createProjectReactor("foo", "1"));
    validator.validate(createProjectReactor("foo", "bra_nch"));
  }

  @Test
  public void fail_with_invalid_branch() {
    ProjectReactor reactor = createProjectReactor("foo", "bran#ch");
    thrown.expect(MessageException.class);
    thrown.expectMessage("\"bran#ch\" is not a valid branch name");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_colon_in_branch() {
    ProjectReactor reactor = createProjectReactor("foo", "bran:ch");
    thrown.expect(MessageException.class);
    thrown.expectMessage("\"bran:ch\" is not a valid branch name");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_only_digits() {
    ProjectReactor reactor = createProjectReactor("12345");

    thrown.expect(MessageException.class);
    thrown.expectMessage("\"12345\" is not a valid project or module key");
    validator.validate(reactor);
  }

  private ProjectReactor createProjectReactor(String projectKey) {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    ProjectReactor reactor = new ProjectReactor(def);
    return reactor;
  }

  private ProjectReactor createProjectReactor(String projectKey, String branch) {
    ProjectDefinition def = ProjectDefinition.create()
      .setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey)
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, branch);
    return new ProjectReactor(def);
  }

}
