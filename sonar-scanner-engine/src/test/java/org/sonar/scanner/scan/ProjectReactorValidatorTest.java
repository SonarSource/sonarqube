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
package org.sonar.scanner.scan;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static org.apache.commons.lang.RandomStringUtils.randomAscii;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ProjectReactorValidatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private GlobalConfiguration settings = mock(GlobalConfiguration.class);
  private ProjectInfo projectInfo = mock(ProjectInfo.class);
  private ProjectReactorValidator underTest = new ProjectReactorValidator(settings);

  @Before
  public void prepare() {
    when(settings.get(anyString())).thenReturn(Optional.empty());
  }

  @Test
  @UseDataProvider("validKeys")
  public void not_fail_with_valid_key(String validKey) {
    underTest.validate(createProjectReactor(validKey));
  }

  @DataProvider
  public static Object[][] validKeys() {
    return new Object[][] {
      {"foo"},
      {"123foo"},
      {"foo123"},
      {"1Z3"},
      {"a123"},
      {"123a"},
      {"1:2"},
      {"3-3"},
      {"-:"},
      {"Foobar2"},
      {"foo.bar"},
      {"foo-bar"},
      {"foo:bar"},
      {"foo_bar"}
    };
  }

  @Test
  public void fail_with_invalid_key() {
    ProjectReactor reactor = createProjectReactor("  ");

    thrown.expect(MessageException.class);
    thrown.expectMessage("\"  \" is not a valid project or module key");
    underTest.validate(reactor);
  }

  @Test
  public void fail_when_branch_name_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("To use the property \"sonar.branch.name\" and analyze branches, Developer Edition or above is required");

    underTest.validate(reactor);
  }

  @Test
  public void fail_when_branch_target_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.BRANCH_TARGET))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("To use the property \"sonar.branch.target\" and analyze branches, Developer Edition or above is required");

    underTest.validate(reactor);
  }

  @Test
  public void fail_when_pull_request_id_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.PULL_REQUEST_KEY))).thenReturn(Optional.of("#1984"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("To use the property \"sonar.pullrequest.key\" and analyze pull requests, Developer Edition or above is required");

    underTest.validate(reactor);
  }

  @Test
  public void fail_when_pull_request_branch_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.PULL_REQUEST_BRANCH))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("To use the property \"sonar.pullrequest.branch\" and analyze pull requests, Developer Edition or above is required");

    underTest.validate(reactor);
  }

  @Test
  public void fail_when_pull_request_base_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(eq(ScannerProperties.PULL_REQUEST_BASE))).thenReturn(Optional.of("feature1"));

    thrown.expect(MessageException.class);
    thrown.expectMessage("To use the property \"sonar.pullrequest.base\" and analyze pull requests, Developer Edition or above is required");

    underTest.validate(reactor);
  }

  @Test
  @UseDataProvider("validVersions")
  public void not_fail_with_valid_version(String validVersion) {
    when(projectInfo.getProjectVersion()).thenReturn(Optional.ofNullable(validVersion));

    underTest.validate(createProjectReactor("foo"));
  }

  @DataProvider
  public static Object[][] validVersions() {
    return new Object[][] {
      {null},
      {"1.0"},
      {"2017-10-16"},
      {randomAscii(100)}
    };
  }

  @Test
  @UseDataProvider("validBuildStrings")
  public void not_fail_with_valid_buildString(String validBuildString) {
    when(projectInfo.getBuildString()).thenReturn(Optional.ofNullable(validBuildString));

    underTest.validate(createProjectReactor("foo"));
  }

  @DataProvider
  public static Object[][] validBuildStrings() {
    return new Object[][] {
      {null},
      {"1.0"},
      {"2017-10-16"},
      {randomAscii(100)}
    };
  }

  private ProjectReactor createProjectReactor(String projectKey, Consumer<ProjectDefinition>... consumers) {
    ProjectDefinition def = ProjectDefinition.create()
      .setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    Arrays.stream(consumers).forEach(c -> c.accept(def));
    return new ProjectReactor(def);
  }
}
