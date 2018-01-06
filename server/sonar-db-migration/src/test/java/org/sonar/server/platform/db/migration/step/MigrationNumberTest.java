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
package org.sonar.server.platform.db.migration.step;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.test.TestUtils;

public class MigrationNumberTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_is_private() {
    TestUtils.hasOnlyPrivateConstructors(MigrationNumber.class);
  }

  @Test
  public void validate_throws_IAE_if_argument_is_less_then_0() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Migration number must be >= 0");

    MigrationNumber.validate(-(Math.abs(new Random().nextInt()) + 1));
  }

  @Test
  public void validate_accepts_0() {
    MigrationNumber.validate(0);
  }

  @Test
  public void validate_accepts_any_positive_long() {
    MigrationNumber.validate(Math.abs(new Random().nextInt()));
  }
}
