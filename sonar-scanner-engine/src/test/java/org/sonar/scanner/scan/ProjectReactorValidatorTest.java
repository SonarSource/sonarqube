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
package org.sonar.scanner.scan;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectReactorValidatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private AnalysisMode mode;
  private ProjectReactorValidator validator;
  private GlobalConfiguration settings;

  @Before
  public void prepare() {
    mode = mock(AnalysisMode.class);
    settings = mock(GlobalConfiguration.class);
    when(settings.get(anyString())).thenReturn(Optional.empty());
    validator = new ProjectReactorValidator(mode, settings);
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

  @Test
  public void fail_when_branch_name_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("the branch plugin is required but not installed");

    validator.validate(reactor);
  }

  @Test
  public void fail_when_branch_target_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.BRANCH_TARGET))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("the branch plugin is required but not installed");

    validator.validate(reactor);
  }

  @Test
  public void not_fail_with_valid_version() {
    validator.validate(createProjectReactor("foo", def -> def.setVersion("1.0")));
    validator.validate(createProjectReactor("foo", def -> def.setVersion("2017-10-16")));
    validator.validate(createProjectReactor("foo", def -> def.setVersion(repeat("a", 100))));
  }

  @Test
  public void fail_with_too_long_version() {
    ProjectReactor reactor = createProjectReactor("foo", def -> def.setVersion(repeat("a", 101)));

    thrown.expect(MessageException.class);
    thrown.expectMessage("\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\" is not a valid version name for module \"foo\". " +
      "The maximum length for version numbers is 100 characters.");

    validator.validate(reactor);
  }

  private ProjectReactor createProjectReactor(String projectKey, String branch) {
    return createProjectReactor(projectKey, def -> def
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, branch));
  }

  private ProjectReactor createProjectReactor(String projectKey, Consumer<ProjectDefinition>... consumers) {
    ProjectDefinition def = ProjectDefinition.create()
      .setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    Arrays.stream(consumers).forEach(c -> c.accept(def));
    return new ProjectReactor(def);
  }
}
