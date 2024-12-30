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
package org.sonar.ce.task;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class CeTaskComponentTest {

  @Test
  @UseDataProvider("nullOrEmpty")
  public void constructor_fails_with_NPE_if_uuid_is_null_or_empty(String str) {
    assertThatThrownBy(() -> new CeTask.Component(str, "foo", "bar"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid can't be null nor empty");
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void constructor_considers_empty_as_null_and_accept_it_for_key(String str) {
    CeTask.Component underTest = new CeTask.Component("foo", str, "bar");

    assertThat(underTest.getKey()).isEmpty();
  }

  @Test
  @UseDataProvider("nullOrEmpty")
  public void constructor_considers_empty_as_null_and_accept_it_for_name(String str) {
    CeTask.Component underTest = new CeTask.Component("foo", "bar", str);

    assertThat(underTest.getName()).isEmpty();
  }

  @Test
  public void equals_is_based_on_all_fields() {
    String uuid = secure().nextAlphabetic(2);
    String key = secure().nextAlphabetic(3);
    String name = secure().nextAlphabetic(4);
    String somethingElse = secure().nextAlphabetic(5);
    CeTask.Component underTest = new CeTask.Component(uuid, key, name);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new CeTask.Component(uuid, key, name))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new CeTask.Component(somethingElse, key, name))
      .isNotEqualTo(new CeTask.Component(uuid, somethingElse, name))
      .isNotEqualTo(new CeTask.Component(uuid, key, somethingElse))
      .isNotEqualTo(new CeTask.Component(uuid, key, null));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    String uuid = secure().nextAlphabetic(2);
    String key = secure().nextAlphabetic(3);
    String name = secure().nextAlphabetic(4);
    String somethingElse = secure().nextAlphabetic(5);
    CeTask.Component underTest = new CeTask.Component(uuid, key, name);

    assertThat(underTest)
      .hasSameHashCodeAs(underTest)
      .hasSameHashCodeAs(new CeTask.Component(uuid, key, name));
    assertThat(underTest.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new CeTask.Component(somethingElse, key, name).hashCode())
      .isNotEqualTo(new CeTask.Component(uuid, somethingElse, name).hashCode())
      .isNotEqualTo(new CeTask.Component(uuid, key, somethingElse).hashCode())
      .isNotEqualTo(new CeTask.Component(uuid, key, null).hashCode());
  }

  @DataProvider
  public static Object[][] nullOrEmpty() {
    return new Object[][] {
      {null},
      {""},
    };
  }
}
