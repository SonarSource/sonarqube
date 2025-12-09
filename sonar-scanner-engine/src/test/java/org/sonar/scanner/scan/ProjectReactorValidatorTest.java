/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.core.documentation.DefaultDocumentationLinkGenerator;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.ScannerProperties.BRANCHES_DOC_LINK_SUFFIX;

class ProjectReactorValidatorTest {

  private final GlobalConfiguration settings = mock(GlobalConfiguration.class);
  private final ProjectInfo projectInfo = mock(ProjectInfo.class);
  private final DefaultDocumentationLinkGenerator defaultDocumentationLinkGenerator = mock(DefaultDocumentationLinkGenerator.class);
  private final ProjectReactorValidator underTest = new ProjectReactorValidator(settings, defaultDocumentationLinkGenerator);
  private static final String LINK_TO_DOC = "link_to_documentation";

  @BeforeEach
  void prepare() {
    when(settings.get(anyString())).thenReturn(Optional.empty());
    when(defaultDocumentationLinkGenerator.getDocumentationLink(BRANCHES_DOC_LINK_SUFFIX)).thenReturn(LINK_TO_DOC);
  }

  @ParameterizedTest
  @MethodSource("validKeys")
  void not_fail_with_valid_key(String validKey) {
    ProjectReactor projectReactor = createProjectReactor(validKey);
    underTest.validate(projectReactor);
  }

  private static Stream<String> validKeys() {
    return Stream.of(
      "foo",
      "123foo",
      "foo123",
      "1Z3",
      "a123",
      "123a",
      "1:2",
      "3-3",
      "-:",
      "Foobar2",
      "foo.bar",
      "foo-bar",
      "foo:bar",
      "foo_bar"
    );
  }

  @Test
  void fail_when_invalid_key() {
    ProjectReactor reactor = createProjectReactor("foo$bar");

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("\"foo$bar\" is not a valid project key. Allowed characters are alphanumeric,"
        + " '-', '_', '.' and ':', with at least one non-digit.");
  }

  @Test
  void fail_when_only_digits() {
    ProjectReactor reactor = createProjectReactor("12345");

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("\"12345\" is not a valid project key. Allowed characters are alphanumeric, "
        + "'-', '_', '.' and ':', with at least one non-digit.");
  }

  @Test
  void fail_when_backslash_in_key() {
    ProjectReactor reactor = createProjectReactor("foo\\bar");

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("\"foo\\bar\" is not a valid project key. Allowed characters are alphanumeric, "
        + "'-', '_', '.' and ':', with at least one non-digit.");
  }

  @Test
  void fail_when_branch_name_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(ScannerProperties.BRANCH_NAME)).thenReturn(Optional.of("feature1"));

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining(format("To use the property \"sonar.branch.name\" and analyze branches, Developer Edition or above is required. See %s for more information.",
        LINK_TO_DOC));
  }

  @Test
  void fail_when_pull_request_id_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(ScannerProperties.PULL_REQUEST_KEY)).thenReturn(Optional.of("#1984"));

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining(format("To use the property \"sonar.pullrequest.key\" and analyze pull requests, Developer Edition or above is required. See %s for more information.",
        LINK_TO_DOC));
  }

  @Test
  void fail_when_pull_request_branch_is_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(ScannerProperties.PULL_REQUEST_BRANCH)).thenReturn(Optional.of("feature1"));

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining(format("To use the property \"sonar.pullrequest.branch\" and analyze pull requests, Developer Edition or above is required. See %s for more information.",
        LINK_TO_DOC));
  }

  @Test
  void fail_when_pull_request_base_specified_but_branch_plugin_not_present() {
    ProjectDefinition def = ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectReactor reactor = new ProjectReactor(def);

    when(settings.get(ScannerProperties.PULL_REQUEST_BASE)).thenReturn(Optional.of("feature1"));

    assertThatThrownBy(() -> underTest.validate(reactor))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining(format("To use the property \"sonar.pullrequest.base\" and analyze pull requests, Developer Edition or above is required. See %s for more information.",
        LINK_TO_DOC));
  }

  @ParameterizedTest
  @MethodSource("validVersions")
  void not_fail_with_valid_version(@Nullable String validVersion) {
    when(projectInfo.getProjectVersion()).thenReturn(Optional.ofNullable(validVersion));

    ProjectReactor projectReactor = createProjectReactor("foo");
    underTest.validate(projectReactor);
  }

  private static Stream<String> validVersions() {
    return Stream.of(
      null,
      "1.0",
      "2017-10-16",
      secure().nextAscii(100)
    );
  }

  @ParameterizedTest
  @MethodSource("validBuildStrings")
  void not_fail_with_valid_buildString(@Nullable String validBuildString) {
    when(projectInfo.getBuildString()).thenReturn(Optional.ofNullable(validBuildString));

    ProjectReactor projectReactor = createProjectReactor("foo");
    underTest.validate(projectReactor);
  }

  private static Stream<String> validBuildStrings() {
    return Stream.of(
      null,
      "1.0",
      "2017-10-16",
      secure().nextAscii(100)
    );
  }

  private ProjectReactor createProjectReactor(String projectKey, Consumer<ProjectDefinition>... consumers) {
    ProjectDefinition def = ProjectDefinition.create()
      .setProperty(CoreProperties.PROJECT_KEY_PROPERTY, projectKey);
    Arrays.stream(consumers).forEach(c -> c.accept(def));
    return new ProjectReactor(def);
  }
}
