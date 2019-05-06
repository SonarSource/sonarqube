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
package org.sonar.server.webhook;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_NPE_when_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid must not be null");

    new Analysis(null, 1_990L, "abcde");
  }

  @Test
  public void test_bean() {
    Analysis analysis = new Analysis("u1", 1_990L, "abcde");
    assertThat(analysis.getUuid()).isEqualTo("u1");
    assertThat(analysis.getDate()).isEqualTo(1_990L);
    assertThat(analysis.getRevision()).hasValue("abcde");

    analysis = new Analysis("u1", 1_990L, null);
    assertThat(analysis.getRevision()).isEmpty();
  }
}
