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
package org.sonar.ce.task.projectanalysis.analysis;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnalysisImplTest {

  private static final long ID = 10;
  private static final String UUID = "uuid ";
  private static final long CREATED_AT = 123456789L;

  @Test
  public void build_snapshot() {
    Analysis analysis = new Analysis.Builder()
      .setUuid(UUID)
      .setCreatedAt(CREATED_AT)
      .build();

    assertThat(analysis.getUuid()).isEqualTo(UUID);
    assertThat(analysis.getCreatedAt()).isEqualTo(CREATED_AT);
  }

  @Test
  public void fail_with_NPE_when_building_snapshot_without_uuid() {
    assertThatThrownBy(() -> new Analysis.Builder().setCreatedAt(CREATED_AT).build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid cannot be null");
  }

  @Test
  public void fail_with_NPE_when_building_snapshot_without_created_at() {
    assertThatThrownBy(() -> new Analysis.Builder().setUuid(UUID).build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("createdAt cannot be null");
  }

  @Test
  public void test_toString() {
    assertThat(new Analysis.Builder()
      .setUuid(UUID)
      .setCreatedAt(CREATED_AT)
      .build()).hasToString("Analysis{uuid='uuid ', createdAt=123456789}");
  }

  @Test
  public void test_equals_and_hascode() {
    Analysis analysis = new Analysis.Builder()
      .setUuid(UUID)
      .setCreatedAt(CREATED_AT)
      .build();
    Analysis sameAnalysis = new Analysis.Builder()
      .setUuid(UUID)
      .setCreatedAt(CREATED_AT)
      .build();
    Analysis otherAnalysis = new Analysis.Builder()
      .setUuid("other uuid")
      .setCreatedAt(CREATED_AT)
      .build();

    assertThat(analysis)
      .isEqualTo(analysis)
      .isEqualTo(sameAnalysis)
      .isNotEqualTo(otherAnalysis)
      .isNotNull()
      .hasSameHashCodeAs(analysis.hashCode())
      .hasSameHashCodeAs(sameAnalysis.hashCode());
    assertThat(analysis.hashCode()).isNotEqualTo(otherAnalysis.hashCode());
  }
}
