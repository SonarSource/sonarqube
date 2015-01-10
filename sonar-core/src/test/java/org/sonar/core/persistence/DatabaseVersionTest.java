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
package org.sonar.core.persistence;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseVersionTest extends AbstractDaoTestCase {
  @Test
  public void getVersion() {
    setupData("getVersion");

    Integer version = new DatabaseVersion(getMyBatis()).getVersion();

    assertThat(version).isEqualTo(123);
  }

  @Test
  public void getVersion_no_rows() {
    setupData("getVersion_no_rows");

    Integer version = new DatabaseVersion(getMyBatis()).getVersion();

    assertThat(version).isNull();
  }

  @Test
  public void getStatus() {
    assertThat(DatabaseVersion.getStatus(null, 150)).isEqualTo(DatabaseVersion.Status.FRESH_INSTALL);
    assertThat(DatabaseVersion.getStatus(123, 150)).isEqualTo(DatabaseVersion.Status.REQUIRES_UPGRADE);
    assertThat(DatabaseVersion.getStatus(150, 150)).isEqualTo(DatabaseVersion.Status.UP_TO_DATE);
    assertThat(DatabaseVersion.getStatus(200, 150)).isEqualTo(DatabaseVersion.Status.REQUIRES_DOWNGRADE);
  }
}
