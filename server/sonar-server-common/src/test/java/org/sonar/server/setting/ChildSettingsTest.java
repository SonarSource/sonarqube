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
package org.sonar.server.setting;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ChildSettingsTest {
  private static final Random RANDOM = new Random();


  private MapSettings parent = new MapSettings();
  private ChildSettings underTest = new ChildSettings(parent);

  @Test
  public void childSettings_should_retrieve_parent_settings() {
    String multipleValuesKey = secure().nextAlphanumeric(19);
    PropertyDefinition multipleValues = PropertyDefinition.builder(multipleValuesKey).multiValues(true).build();
    MapSettings parent = new MapSettings(new PropertyDefinitions(System2.INSTANCE, Collections.singletonList(multipleValues)));
    ChildSettings underTest = new ChildSettings(parent);

    parent.setProperty(secure().nextAlphanumeric(10), secure().nextAlphanumeric(20));
    parent.setProperty(secure().nextAlphanumeric(11), RANDOM.nextLong());
    parent.setProperty(secure().nextAlphanumeric(12), RANDOM.nextDouble());
    parent.setProperty(secure().nextAlphanumeric(13), RANDOM.nextFloat());
    parent.setProperty(secure().nextAlphanumeric(14), RANDOM.nextBoolean());
    parent.setProperty(secure().nextAlphanumeric(15), RANDOM.nextInt(Integer.MAX_VALUE));
    parent.setProperty(secure().nextAlphanumeric(16), new Date(RANDOM.nextInt()));
    parent.setProperty(secure().nextAlphanumeric(17), new Date(RANDOM.nextInt()), true);
    parent.setProperty(secure().nextAlphanumeric(18), new Date(RANDOM.nextInt()), false);
    parent.setProperty(multipleValuesKey, new String[] {secure().nextAlphanumeric(10), secure().nextAlphanumeric(20)});

    assertThat(underTest.getProperties()).isEqualTo(parent.getProperties());
  }

  @Test
  public void set_will_throw_NPE_if_key_is_null() {
    assertThatThrownBy(() -> underTest.set(null, ""))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key can't be null");
  }

  @Test
  public void set_will_throw_NPE_if_value_is_null() {
    assertThatThrownBy(() -> underTest.set(secure().nextAlphanumeric(10), null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value can't be null");
  }

  @Test
  public void childSettings_override_parent() {
    String key = secure().nextAlphanumeric(10);
    parent.setProperty(key, secure().nextAlphanumeric(20));
    underTest.setProperty(key, secure().nextAlphanumeric(10));

    Optional<String> result = underTest.get(key);
    assertThat(result).isPresent();
    assertThat(result.get()).isNotEqualTo(parent.getString(key));
  }

  @Test
  public void remove_should_not_throw_exception_if_key_is_not_present() {
    underTest.remove(secure().nextAlphanumeric(90));
  }

  @Test
  public void remove_should_remove_value() {
    String key = secure().nextAlphanumeric(10);
    String childValue = secure().nextAlphanumeric(10);

    underTest.set(key, childValue);
    assertThat(underTest.get(key)).isEqualTo(Optional.of(childValue));

    underTest.remove(key);
    assertThat(underTest.get(key)).isEmpty();
  }

  @Test
  public void remove_should_retrieve_parent_value() {
    String key = secure().nextAlphanumeric(10);
    String childValue = secure().nextAlphanumeric(10);
    String parentValue = secure().nextAlphanumeric(10);

    parent.setProperty(key, parentValue);
    underTest.set(key, childValue);
    assertThat(underTest.get(key)).isEqualTo(Optional.of(childValue));

    underTest.remove(key);
    assertThat(underTest.get(key)).isEqualTo(Optional.of(parentValue));
  }

}
