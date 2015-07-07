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
package org.sonar.db.property;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyDtoTest {

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
    assertThat(new PropertyDto().setKey("foo:bar").setValue("value").setResourceId(123L).setUserId(456L).toString()).isEqualTo("PropertyDto{foo:bar, value, 123, 456}");
  }
}
