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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisPropertyDtoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AnalysisPropertyDto underTest;

  @Test
  public void null_key_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key cannot be null");

    underTest.setKey(null);
  }

  @Test
  public void null_value_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("value cannot be null");

    underTest.setValue(null);
  }

  @Test
  public void null_uuid_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid cannot be null");

    underTest.setUuid(null);
  }

  @Test
  public void null_snapshot_uuid_should_throw_NPE() {
    underTest = new AnalysisPropertyDto();

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("snapshotUuid cannot be null");

    underTest.setSnapshotUuid(null);
  }

  @Test
  public void test_equality() {
    underTest = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(40))
      .setSnapshotUuid(randomAlphanumeric(40))
      .setKey(randomAlphanumeric(512))
      .setValue(randomAlphanumeric(10000));

    assertThat(underTest).isEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue()));

    assertThat(underTest).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid("1" + underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue()));

    assertThat(underTest).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid("1" + underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue()));

    assertThat(underTest).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey("1" + underTest.getKey())
        .setValue(underTest.getValue()));

    assertThat(underTest).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue("1" + underTest.getValue()));
  }

  @Test
  public void test_hashcode() {
    underTest = new AnalysisPropertyDto()
      .setUuid(randomAlphanumeric(40))
      .setSnapshotUuid(randomAlphanumeric(40))
      .setKey(randomAlphanumeric(512))
      .setValue(randomAlphanumeric(10000));

    assertThat(underTest.hashCode()).isEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid("1" + underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid("1" + underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey("1" + underTest.getKey())
        .setValue(underTest.getValue())
        .hashCode());

    assertThat(underTest.hashCode()).isNotEqualTo(
      new AnalysisPropertyDto()
        .setUuid(underTest.getUuid())
        .setSnapshotUuid(underTest.getSnapshotUuid())
        .setKey(underTest.getKey())
        .setValue("1" + underTest.getValue())
        .hashCode());
  }
}
