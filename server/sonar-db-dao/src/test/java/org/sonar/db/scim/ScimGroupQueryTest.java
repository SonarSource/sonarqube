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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@RunWith(DataProviderRunner.class)
public class ScimGroupQueryTest {

  @DataProvider
  public static Object[][] filterData() {
    ScimGroupQuery queryWithDisplayName = new ScimGroupQuery("testGroup");
    return new Object[][] {
      {"displayName eq \"testGroup\"", queryWithDisplayName},
      {"  displayName eq \"testGroup\"  ", queryWithDisplayName},
      {"displayName     eq     \"testGroup\"", queryWithDisplayName},
      {"DIsPlaynaMe eq \"testGroup\"", queryWithDisplayName},
      {"displayName EQ \"testGroup\"", queryWithDisplayName},
      {null, ScimGroupQuery.ALL},
      {"", ScimGroupQuery.ALL}
    };
  }

  @Test
  @UseDataProvider("filterData")
  public void fromScimFilter_shouldCorrectlyResolveProperties(String filter, ScimGroupQuery expected) {
    ScimGroupQuery scimGroupQuery = ScimGroupQuery.fromScimFilter(filter);

    assertThat(scimGroupQuery).usingRecursiveComparison().isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] unsupportedFilterData() {
    return new Object[][] {
      {"otherProp eq \"testGroup\""},
      {"displayName eq \"testGroup\" or displayName eq \"testGroup2\""},
      {"displayName eq \"testGroup\" and email eq \"test.user2@okta.local\""},
      {"displayName eq \"testGroup\"xjdkfgldkjfhg"}
    };
  }

  @Test
  @UseDataProvider("unsupportedFilterData")
  public void fromScimFilter_shouldThrowAnException(String filter) {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> ScimGroupQuery.fromScimFilter(filter))
      .withMessage(format("Unsupported filter or value: %s. The only supported filter and operator is 'displayName eq \"displayName\"", filter));
  }

  @Test
  public void empty_shouldHaveNoProperties() {
    ScimGroupQuery scimGroupQuery = ScimGroupQuery.ALL;

    assertThat(scimGroupQuery.getDisplayName()).isNull();
  }

}
