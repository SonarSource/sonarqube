/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class CveCweDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final CveCweDao cveCweDao = db.getDbClient().cveCweDao();

  @Test
  void insert_shouldPersistCveCwe() {
    var cveCweDto = new CveCweDto("CVE_UUID", "CWE-123");

    cveCweDao.insert(db.getSession(), cveCweDto);

    List<Map<String, Object>> result = db.select(db.getSession(), "select * from cve_cwe");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "cve_uuid", cveCweDto.cveUuid(),
        "cwe", cveCweDto.cwe())
    );
  }

  @Test
  void selectByCveUuid_shouldReturnCwesAttachedToCve() {
    String cveUuid = "CVE_UUID";
    cveCweDao.insert(db.getSession(), new CveCweDto(cveUuid, "CWE-123"));
    cveCweDao.insert(db.getSession(), new CveCweDto(cveUuid, "CWE-456"));
    cveCweDao.insert(db.getSession(), new CveCweDto("ANOTHER_CVE_UUID", "CWE-789"));

    Set<String> result = cveCweDao.selectByCveUuid(db.getSession(), cveUuid);

    assertThat(result).containsExactlyInAnyOrder("CWE-123", "CWE-456");
  }

  @Test
  void selectByCveUuid_shouldReturnEmpty_whenNoCweAttachedToCve() {
    Set<String> result = cveCweDao.selectByCveUuid(db.getSession(), "some_uuid");

    assertThat(result).isEmpty();
  }

  @Test
  void deleteByCveUuid_shouldDeleteCwesAttachedToCve() {
    String cveUuid = "CVE_UUID";
    cveCweDao.insert(db.getSession(), new CveCweDto(cveUuid, "CWE-123"));
    cveCweDao.insert(db.getSession(), new CveCweDto(cveUuid, "CWE-456"));
    CveCweDto nonDeletedCwe = new CveCweDto("ANOTHER_CVE_UUID", "CWE-789");
    cveCweDao.insert(db.getSession(), nonDeletedCwe);

    cveCweDao.deleteByCveUuid(db.getSession(), cveUuid);

    List<Map<String, Object>> result = db.select(db.getSession(), "select * from cve_cwe");
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "cve_uuid", nonDeletedCwe.cveUuid(),
        "cwe", nonDeletedCwe.cwe())
    );
  }

}
