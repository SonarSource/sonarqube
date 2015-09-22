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
package org.sonar.server.computation.log;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.ce.CeTaskTypes;

import static org.assertj.core.api.Assertions.assertThat;

public class LogFileRefTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void equals_hashCode() {
    LogFileRef ref1 = new LogFileRef(CeTaskTypes.REPORT, "UUID_1", "COMPONENT_1");
    LogFileRef ref1bis = new LogFileRef(CeTaskTypes.REPORT, "UUID_1", "COMPONENT_1");
    LogFileRef ref2 = new LogFileRef(CeTaskTypes.REPORT, "UUID_2", "COMPONENT_1");

    assertThat(ref1.equals(ref1)).isTrue();
    assertThat(ref1.equals(ref1bis)).isTrue();
    assertThat(ref1.equals(ref2)).isFalse();
    assertThat(ref1.equals(null)).isFalse();
    assertThat(ref1.equals("UUID_1")).isFalse();

    assertThat(ref1.hashCode()).isEqualTo(ref1bis.hashCode());
  }

  @Test
  public void getRelativePath() {
    assertThat(new LogFileRef("TYPE_1", "UUID_1", "COMPONENT_1").getRelativePath()).isEqualTo("TYPE_1/COMPONENT_1/UUID_1.log");
    assertThat(new LogFileRef("TYPE_1", "UUID_1", null).getRelativePath()).isEqualTo("TYPE_1/UUID_1.log");
  }

  @Test
  public void do_not_accept_invalid_task_type() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'foo/bar' is not a valid filename for Compute Engine logs");

    new LogFileRef("foo/bar", "UUID", null);
  }

  @Test
  public void do_not_accept_invalid_uuid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'foo/bar' is not a valid filename for Compute Engine logs");

    new LogFileRef("REPORT", "foo/bar", null);
  }

  @Test
  public void do_not_accept_invalid_component_uuid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'foo/bar' is not a valid filename for Compute Engine logs");

    new LogFileRef("REPORT", "UUID", "foo/bar");
  }

  @Test
  public void filename_must_support_uuid() {
    String uuid = "AU-Tpxb-_iU5OvuD2FLy";
    assertThat(LogFileRef.requireValidFilename(uuid)).isEqualTo(uuid);
  }
}
