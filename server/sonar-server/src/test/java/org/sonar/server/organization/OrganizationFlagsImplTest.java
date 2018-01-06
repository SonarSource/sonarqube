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
package org.sonar.server.organization;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationFlagsImplTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OrganizationFlagsImpl underTest = new OrganizationFlagsImpl(db.getDbClient());

  @Test
  public void isEnabled_returns_false_by_default() {
    assertThat(underTest.isEnabled(db.getSession())).isFalse();
    verifyInternalProperty(null);
  }

  @Test
  public void enable_does_enable_support_by_inserting_internal_property() {
    underTest.enable(db.getSession());

    assertThat(underTest.isEnabled(db.getSession())).isTrue();
    verifyInternalProperty("true");
  }

  @Test
  public void checkEnabled_throws_IllegalStateException_if_feature_is_disabled() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization support is disabled");

    underTest.checkEnabled(db.getSession());
  }

  @Test
  public void checkEnabled_succeeds_if_feature_is_enabled() {
    underTest.enable(db.getSession());

    underTest.checkEnabled(db.getSession());
  }

  private void verifyInternalProperty(@Nullable String expectedValue) {
    db.properties().verifyInternal("organization.enabled", expectedValue);
  }
}
