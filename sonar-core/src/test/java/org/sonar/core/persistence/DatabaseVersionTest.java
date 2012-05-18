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
package org.sonar.core.persistence;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class DatabaseVersionTest extends DaoTestCase {
  @Test
  public void getVersion() {
    setupData("getVersion");

    Integer version = new DatabaseVersion(getMyBatis()).getVersion();

    assertThat(version, Is.is(123));
  }

  @Test
  public void getVersion_no_rows() {
    setupData("getVersion_no_rows");

    Integer version = new DatabaseVersion(getMyBatis()).getVersion();

    assertThat(version, nullValue());
  }

  @Test
  public void getSonarCoreId() {
    setupData("getSonarCoreId");

    String sonarCoreId = new DatabaseVersion(getMyBatis()).getSonarCoreId();

    assertThat(sonarCoreId, is("123456"));
  }

  @Test
  public void getStatus() {
    assertThat(DatabaseVersion.getStatus(null, 150), is(DatabaseVersion.Status.FRESH_INSTALL));
    assertThat(DatabaseVersion.getStatus(123, 150), is(DatabaseVersion.Status.REQUIRES_UPGRADE));
    assertThat(DatabaseVersion.getStatus(150, 150), is(DatabaseVersion.Status.UP_TO_DATE));
    assertThat(DatabaseVersion.getStatus(200, 150), is(DatabaseVersion.Status.REQUIRES_DOWNGRADE));
  }
}
