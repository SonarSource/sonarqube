/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.purge;

import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeMapperIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession;
  private PurgeMapper purgeMapper;

  @Before
  public void setUp() {
    dbSession = db.getDbClient().openSession(false);
    purgeMapper = dbSession.getMapper(PurgeMapper.class);
  }

  @After
  public void tearDown() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_returns_empty_when_table_is_empty() {
    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid("foo")).isEmpty();
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_returns_project_with_specified_uuid() {
    ComponentDto project = randomPublicOrPrivateProject();

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(project.uuid()))
      .containsOnly(project.uuid());
  }

  private ComponentDto randomPublicOrPrivateProject() {
    return new Random().nextBoolean() ? db.components().insertPrivateProject().getMainBranchComponent() : db.components().insertPublicProject().getMainBranchComponent();
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_returns_view_with_specified_uuid() {
    ComponentDto view = db.components().insertPrivatePortfolio();

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(view.uuid()))
      .containsOnly(view.uuid());
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_returns_application_with_specified_uuid() {
    ComponentDto view = db.components().insertPublicApplication().getMainBranchComponent();

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(view.uuid()))
      .containsOnly(view.uuid());
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_returns_subviews_with_specified_project_uuid_and_view() {
    ComponentDto view = db.components().insertPublicPortfolio();
    ComponentDto subview1 = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));
    ComponentDto subview2 = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));
    ComponentDto subview3 = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(view.uuid()))
      .containsOnly(view.uuid(), subview1.uuid(), subview2.uuid(), subview3.uuid());
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_does_not_return_project_copy_with_specified_project_uuid() {
    ComponentDto privateProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto view = db.components().insertPrivatePortfolio();
    db.components().insertComponent(ComponentTesting.newProjectCopy("a", view, privateProject));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(view.uuid()))
      .containsOnly(view.uuid());
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_does_not_return_directory_with_specified_uuid() {
    ComponentDto privateProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(ComponentTesting.newDirectory(privateProject, "A/B"));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(directory.uuid()))
      .isEmpty();
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_does_not_return_file_with_specified_uuid() {
    ComponentDto privateProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(privateProject));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(file.uuid()))
      .isEmpty();
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_does_not_return_subview_with_specified_uuid() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubPortfolio(view));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(subview.uuid()))
      .isEmpty();
  }

  @Test
  public void selectRootAndSubviewsByProjectUuid_does_not_return_technicalCopy_with_specified_uuid() {
    ComponentDto privateProject = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto technicalCopy = db.components().insertComponent(ComponentTesting.newProjectCopy("a", view, privateProject));

    assertThat(purgeMapper.selectRootAndSubviewsByProjectUuid(technicalCopy.uuid()))
      .isEmpty();
  }
}
