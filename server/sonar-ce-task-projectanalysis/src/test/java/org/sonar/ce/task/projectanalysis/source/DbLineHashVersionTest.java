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
package org.sonar.ce.task.projectanalysis.source;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.source.LineHashVersion;

import static org.assertj.core.api.Assertions.assertThat;

public class DbLineHashVersionTest {
  @Rule
  public DbTester db = DbTester.create();

  private DbLineHashVersion underTest = new DbLineHashVersion(db.getDbClient());

  @Test
  public void hasLineHashWithSignificantCode_should_return_true() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    db.fileSources().insertFileSource(file, dto -> dto.setLineHashesVersion(LineHashVersion.WITH_SIGNIFICANT_CODE.getDbValue()));
    Component component = ReportComponent.builder(Component.Type.FILE, 1).setKey("key").setUuid(file.uuid()).build();
    assertThat(underTest.hasLineHashesWithSignificantCode(component)).isTrue();
  }

  @Test
  public void hasLineHashWithSignificantCode_should_return_false_if_file_is_not_found() {
    Component component = ReportComponent.builder(Component.Type.FILE, 1).setKey("key").setUuid("123").build();
    assertThat(underTest.hasLineHashesWithSignificantCode(component)).isFalse();
  }

  @Test
  public void should_cache_line_hash_version_from_db() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project));

    db.fileSources().insertFileSource(file, dto -> dto.setLineHashesVersion(LineHashVersion.WITH_SIGNIFICANT_CODE.getDbValue()));
    Component component = ReportComponent.builder(Component.Type.FILE, 1).setKey("key").setUuid(file.uuid()).build();
    assertThat(underTest.hasLineHashesWithSignificantCode(component)).isTrue();

    assertThat(db.countRowsOfTable("file_sources")).isOne();
    db.executeUpdateSql("delete from file_sources");
    db.commit();
    assertThat(db.countRowsOfTable("file_sources")).isZero();

    // still true because it uses cache
    assertThat(underTest.hasLineHashesWithSignificantCode(component)).isTrue();
  }
}
