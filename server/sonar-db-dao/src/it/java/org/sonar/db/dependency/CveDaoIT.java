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
package org.sonar.db.dependency;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class CveDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final CveDao cveDao = db.getDbClient().cveDao();

  @Test
  void insert_shouldPersistCve() {
    var cveDto = new CveDto("CVE_UUID",
      "CVE-2021-12345",
      "Some CVE description",
      7.5,
      0.00109,
      0.447540000,
      System.currentTimeMillis() - 2_000,
      System.currentTimeMillis() - 1_500,
      System.currentTimeMillis() - 1_000,
      System.currentTimeMillis() - 500);

    cveDao.insert(db.getSession(), cveDto);

    List<Map<String, Object>> result = db.select(db.getSession(), "select * from cves");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "uuid", cveDto.uuid(),
        "id", cveDto.id(),
        "description", cveDto.description(),
        "cvss_score", cveDto.cvssScore(),
        "epss_score", cveDto.epssScore(),
        "epss_percentile", cveDto.epssPercentile(),
        "published_at", cveDto.publishedAt(),
        "last_modified_at", cveDto.lastModifiedAt(),
        "created_at", cveDto.createdAt(),
        "updated_at", cveDto.updatedAt())
    );
  }

  @Test
  void selectById_shouldReturnCve() {
    String cveId = "CVE-2021-12345";
    var cveDto = new CveDto("CVE_UUID",
      cveId,
      "Some CVE description",
      7.5,
      0.00109,
      0.447540000,
      System.currentTimeMillis() - 2_000,
      System.currentTimeMillis() - 1_500,
      System.currentTimeMillis() - 1_000,
      System.currentTimeMillis() - 500);
    cveDao.insert(db.getSession(), cveDto);

    CveDto result = cveDao.selectById(db.getSession(), cveId)
      .orElseGet(() -> fail("Cve not found"));

    assertThat(result).usingRecursiveComparison().isEqualTo(cveDto);
  }

  @Test
  void update_shouldUpdateCveButCreationDate() {
    CveDto insertedDto = new CveDto("uuid-1", "CVE-1", "Some CVE description 1", 1.0, 2.0, 3.0, 4L, 5L, 6L, 7L);
    cveDao.insert(db.getSession(), insertedDto);
    CveDto updatedDto = new CveDto("uuid-1", "CVE-2", "Some CVE description 2", 7.0, 1.0, 2.0, 3L, 4L, 5L, 6L);

    cveDao.update(db.getSession(), updatedDto);

    CveDto result = cveDao.selectById(db.getSession(), updatedDto.id()).orElseGet(() -> fail("CVE not found in database"));
    assertThat(result).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(updatedDto);
    assertThat(result.createdAt()).isEqualTo(insertedDto.createdAt());
  }

}
