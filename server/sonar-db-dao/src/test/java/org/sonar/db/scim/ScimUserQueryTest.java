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
package org.sonar.db.scim;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class ScimUserQueryTest {

  @DataProvider
  public static Object[][] filterData() {
    ScimUserQuery queryWithUserName = ScimUserQuery.builder().userName("test.user@okta.local").build();
    ScimUserQuery emptyQuery = ScimUserQuery.builder().build();
    return new Object[][]{
      {"userName eq \"test.user@okta.local\"", queryWithUserName},
      {"  userName eq \"test.user@okta.local\"  ", queryWithUserName},
      {"userName     eq     \"test.user@okta.local\"", queryWithUserName},
      {"UsERnaMe eq \"test.user@okta.local\"", queryWithUserName},
      {"userName EQ \"test.user@okta.local\"", queryWithUserName},
      {null, emptyQuery},
      {"", emptyQuery}
    };
  }

  @Test
  @UseDataProvider("filterData")
  public void fromScimFilter_shouldCorrectlyResolveProperties(String filter, ScimUserQuery expected) {
    ScimUserQuery scimUserQuery = ScimUserQuery.fromScimFilter(filter);

    assertThat(scimUserQuery).usingRecursiveComparison().isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] unsupportedFilterData() {
    return new Object[][]{
      {"otherProp eq \"test.user@okta.local\""},
      {"userName eq \"test.user@okta.local\" or userName eq \"test.user2@okta.local\""},
      {"userName eq \"test.user@okta.local\" and email eq \"test.user2@okta.local\""},
      {"userName eq \"test.user@okta.local\"xjdkfgldkjfhg"}
    };
  }

  @Test
  @UseDataProvider("unsupportedFilterData")
  public void fromScimFilter_shouldThrowAnException(String filter) {
    assertThatThrownBy(() -> ScimUserQuery.fromScimFilter(filter))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format("Unsupported filter value: %s. Format should be 'userName eq \"username\"'", filter));
  }

  @Test
  public void empty_shouldHaveNoProperties() {
    ScimUserQuery scimUserQuery = ScimUserQuery.empty();

    assertThat(scimUserQuery.getUserName()).isNull();
  }

}
