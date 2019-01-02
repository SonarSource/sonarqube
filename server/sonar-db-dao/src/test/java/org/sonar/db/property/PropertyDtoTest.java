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
package org.sonar.db.property;

import com.google.common.base.Strings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyDtoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  PropertyDto underTest = new PropertyDto();

  @Test
  public void testEquals() {
    assertThat(new PropertyDto().setKey("123").setResourceId(123L)).isEqualTo(new PropertyDto().setKey("123").setResourceId(123L));
    assertThat(new PropertyDto().setKey("1234").setResourceId(123L)).isNotEqualTo(new PropertyDto().setKey("123").setResourceId(123L));
    assertThat(new PropertyDto().setKey("1234").setResourceId(123L)).isNotEqualTo(null);
    assertThat(new PropertyDto().setKey("1234").setResourceId(123L)).isNotEqualTo(new Object());
  }

  @Test
  public void testHashCode() {
    assertThat(new PropertyDto().setKey("123").setResourceId(123L).hashCode()).isNotNull();
    assertThat(new PropertyDto().setKey("123").setResourceId(123L).hashCode())
      .isEqualTo(new PropertyDto().setKey("123").setResourceId(123L).hashCode());
  }

  @Test
  public void testToString() {
    assertThat(new PropertyDto().setKey("foo:bar").setValue("value").setResourceId(123L).setUserId(456).toString()).isEqualTo("PropertyDto{foo:bar, value, 123, 456}");
  }

  @Test
  public void fail_if_key_longer_than_512_characters() {
    String veryLongKey = Strings.repeat("a", 513);
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Setting key length (513) is longer than the maximum authorized (512). '" + veryLongKey + "' was provided");

    underTest.setKey(veryLongKey);
  }
}
