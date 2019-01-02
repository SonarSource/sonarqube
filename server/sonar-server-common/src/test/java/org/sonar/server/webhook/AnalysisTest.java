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

import java.util.Date;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_NPE_when_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid must not be null");

    new Analysis(null, 1_990L);
  }

  @Test
  public void test_equality() {
    String uuid = randomAlphanumeric(35);
    long date = new Random().nextLong();
    Analysis underTest = new Analysis(uuid, date);

    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest.getUuid()).isEqualTo(uuid);
    assertThat(underTest.getDate()).isEqualTo(new Date(date));

    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Analysis(uuid + "1", date));
    assertThat(underTest).isNotEqualTo(new Analysis(uuid, date + 1_000L));
  }

  @Test
  public void test_hashcode() {
    String uuid = randomAlphanumeric(35);
    long date = new Random().nextLong();
    Analysis underTest = new Analysis(uuid, date);

    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Analysis(uuid + "1", date).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Analysis(uuid, date + 1_000).hashCode());
  }
}
