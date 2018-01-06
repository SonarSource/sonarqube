/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeableAnalysisDtoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testEquals() {
    PurgeableAnalysisDto dto1 = new PurgeableAnalysisDto().setAnalysisUuid("u3");
    PurgeableAnalysisDto dto2 = new PurgeableAnalysisDto().setAnalysisUuid("u4");
    assertThat(dto1.equals(dto2)).isFalse();
    assertThat(dto2.equals(dto1)).isFalse();
    assertThat(dto1.equals(dto1)).isTrue();
    assertThat(dto1.equals(new PurgeableAnalysisDto().setAnalysisUuid("u3"))).isTrue();
    assertThat(dto1.equals("bi_bop_a_lou_la")).isFalse();
    assertThat(dto1.equals(null)).isFalse();
  }

  @Test
  public void testHasCode() {
    PurgeableAnalysisDto dto = new PurgeableAnalysisDto().setAnalysisUuid("u3");
    assertThat(dto.hashCode()).isEqualTo(dto.hashCode());

    // no uuid => NPE
    dto = new PurgeableAnalysisDto();

    expectedException.expect(NullPointerException.class);
    dto.hashCode();
  }

  @Test
  public void testToString() {
    PurgeableAnalysisDto dto = new PurgeableAnalysisDto().setAnalysisUuid("u3");
    assertThat(dto.toString()).isNotEmpty();
  }
}
