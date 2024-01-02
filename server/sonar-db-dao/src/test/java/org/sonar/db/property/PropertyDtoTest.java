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
package org.sonar.db.property;

import com.google.common.base.Strings;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropertyDtoTest {

  PropertyDto underTest = new PropertyDto();

  @Test
  public void testEquals() {
    assertThat(new PropertyDto().setKey("123").setComponentUuid("uuid123")).isEqualTo(new PropertyDto().setKey("123").setComponentUuid("uuid123"));
    assertThat(new PropertyDto().setKey("1234").setComponentUuid("uuid123")).isNotEqualTo(new PropertyDto().setKey("123").setComponentUuid("uuid123"));
    assertThat(new PropertyDto().setKey("1234").setComponentUuid("uuid123")).isNotNull();
    assertThat(new PropertyDto().setKey("1234").setComponentUuid("uuid123")).isNotEqualTo(new Object());
  }

  @Test
  public void testHashCode() {
    assertThat(new PropertyDto().setKey("123").setComponentUuid("uuid123"))
      .hasSameHashCodeAs(new PropertyDto().setKey("123").setComponentUuid("uuid123"));
  }

  @Test
  public void testToString() {
    assertThat(new PropertyDto().setKey("foo:bar").setValue("value").setComponentUuid("uuid123").setUserUuid("456"))
      .hasToString("PropertyDto{foo:bar, value, uuid123, 456}");
  }

  @Test
  public void fail_if_key_longer_than_512_characters() {
    String veryLongKey = Strings.repeat("a", 513);

    assertThatThrownBy(() -> underTest.setKey(veryLongKey))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Setting key length (513) is longer than the maximum authorized (512). '" + veryLongKey + "' was provided");
  }
}
