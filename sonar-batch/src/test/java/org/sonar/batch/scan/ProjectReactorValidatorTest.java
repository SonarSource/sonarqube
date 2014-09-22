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
package org.sonar.batch.scan;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.component.Component;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectReactorValidatorTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ProjectReactorValidator validator;
  private Settings settings;
  private ResourceDao resourceDao;

  @Before
  public void prepare() {
    settings = new Settings();
    resourceDao = mock(ResourceDao.class);
    validator = new ProjectReactorValidator(settings, resourceDao);
  }

  @Test
  public void not_fail_if_provisioning_enforced_and_project_exists() throws Exception {
    String key = "project-key";
    settings.setProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, true);
    when(resourceDao.findByKey(key)).thenReturn(mock(Component.class));
    ProjectReactor reactor = createProjectReactor(key);
    validator.validate(reactor);
  }

  @Test
  public void not_fail_if_provisioning_enforced_and_project_with_branch_exists() throws Exception {
    String key = "project-key";
    settings.setProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, true);
    when(resourceDao.findByKey(key + ":branch")).thenReturn(mock(Component.class));
    ProjectReactor reactor = createProjectReactor(key, "branch");
    validator.validate(reactor);
  }

  @Test(expected = SonarException.class)
  public void fail_if_provisioning_enforced_and_project_not_provisioned() throws Exception {
    String key = "project-key";
    settings.setProperty(CoreProperties.CORE_PREVENT_AUTOMATIC_PROJECT_CREATION, true);
    when(resourceDao.findByKey(key)).thenReturn(null);
    ProjectReactor reactor = createProjectReactor(key);
    validator.validate(reactor);
  }

  // SONAR-4692
  @Test(expected = SonarException.class)
  public void fail_if_module_part_of_another_project() throws Exception {
    String rootProjectKey = "project-key";
    String moduleKey = "module-key";
    ResourceDto rootResource = mock(ResourceDto.class);

    when(rootResource.getKey()).thenReturn("another-project-key");

    when(resourceDao.getRootProjectByComponentKey(moduleKey)).thenReturn(rootResource);
    ProjectReactor reactor = createProjectReactor(rootProjectKey);
    reactor.getRoot().addSubProject(ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, moduleKey));
    validator.validate(reactor);
  }

  // SONAR-4692
  @Test
  public void not_fail_if_module_part_of_same_project() throws Exception {
    String rootProjectKey = "project-key";
    String moduleKey = "module-key";
    ResourceDto rootResource = mock(ResourceDto.class);

    when(rootResource.getKey()).thenReturn(rootProjectKey);

    when(resourceDao.getRootProjectByComponentKey(moduleKey)).thenReturn(rootResource);
    ProjectReactor reactor = createProjectReactor(rootProjectKey);
    reactor.getRoot().addSubProject(ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, moduleKey));
    validator.validate(reactor);
  }

  // SONAR-4692
  @Test
  public void not_fail_if_new_module() throws Exception {
    String rootProjectKey = "project-key";
    String moduleKey = "module-key";

    when(resourceDao.getRootProjectByComponentKey(moduleKey)).thenReturn(null);

    ProjectReactor reactor = createProjectReactor(rootProjectKey);
    reactor.getRoot().addSubProject(ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, moduleKey));
    validator.validate(reactor);
  }

  // SONAR-4245
  @Test
  public void shouldFailWhenTryingToConvertProjectIntoModule() {
    String rootProjectKey = "project-key";
    String moduleKey = "module-key";
    ResourceDto rootResource = mock(ResourceDto.class);

    when(rootResource.getKey()).thenReturn(moduleKey);

    when(resourceDao.getRootProjectByComponentKey(moduleKey)).thenReturn(rootResource);

    ProjectReactor reactor = createProjectReactor(rootProjectKey);
    reactor.getRoot().addSubProject(ProjectDefinition.create().setProperty(CoreProperties.PROJECT_KEY_PROPERTY, moduleKey));

    thrown.expect(SonarException.class);
    thrown.expectMessage("The project 'module-key' is already defined in SonarQube but not as a module of project 'project-key'. "
      + "If you really want to stop directly analysing project 'module-key', please first delete it from SonarQube and then relaunch the analysis of project 'project-key'.");

    validator.validate(reactor);
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

    thrown.expect(SonarException.class);
    thrown.expectMessage("\"foo$bar\" is not a valid project or module key");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_backslash_in_key() {
    ProjectReactor reactor = createProjectReactor("foo\\bar");

    thrown.expect(SonarException.class);
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
    thrown.expect(SonarException.class);
    thrown.expectMessage("\"bran#ch\" is not a valid branch name");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_colon_in_branch() {
    ProjectReactor reactor = createProjectReactor("foo", "bran:ch");
    thrown.expect(SonarException.class);
    thrown.expectMessage("\"bran:ch\" is not a valid branch name");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_only_digits() {
    ProjectReactor reactor = createProjectReactor("12345");

    thrown.expect(SonarException.class);
    thrown.expectMessage("\"12345\" is not a valid project or module key");
    validator.validate(reactor);
  }

  @Test
  public void fail_with_deprecated_sonar_phase() {
    ProjectReactor reactor = createProjectReactor("foo");
    settings.setProperty("sonar.phase", "phase");

    thrown.expect(SonarException.class);
    thrown.expectMessage("\"sonar.phase\" is deprecated");
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
    ProjectReactor reactor = new ProjectReactor(def);
    settings.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, branch);
    return reactor;
  }

}
