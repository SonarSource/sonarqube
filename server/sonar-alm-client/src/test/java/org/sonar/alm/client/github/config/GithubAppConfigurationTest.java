/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github.config;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class GithubAppConfigurationTest {

  @Test
  @UseDataProvider("incompleteConfigurationParametersSonarQube")
  public void isComplete_returns_false_if_configuration_is_incomplete_on_SonarQube(@Nullable Long applicationId, @Nullable String privateKey, @Nullable String apiEndpoint) {
    GithubAppConfiguration underTest = new GithubAppConfiguration(applicationId, privateKey, apiEndpoint);

    assertThat(underTest.isComplete()).isFalse();
  }

  @Test
  @UseDataProvider("incompleteConfigurationParametersSonarQube")
  public void getId_throws_ISE_if_config_is_incomplete(@Nullable Long applicationId, @Nullable String privateKey, @Nullable String apiEndpoint) {
    GithubAppConfiguration underTest = new GithubAppConfiguration(applicationId, privateKey, apiEndpoint);

    assertThatThrownBy(underTest::getId)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Configuration is not complete");
  }

  @Test
  public void getId_returns_applicationId_if_configuration_is_valid() {
    long applicationId = new Random().nextLong();
    GithubAppConfiguration underTest = newValidConfiguration(applicationId);

    assertThat(underTest.getId()).isEqualTo(applicationId);
  }

  @Test
  @UseDataProvider("incompleteConfigurationParametersSonarQube")
  public void getPrivateKeyFile_throws_ISE_if_config_is_incomplete(@Nullable Long applicationId, @Nullable String privateKey, @Nullable String apiEndpoint) {
    GithubAppConfiguration underTest = new GithubAppConfiguration(applicationId, privateKey, apiEndpoint);

    assertThatThrownBy(underTest::getPrivateKey)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Configuration is not complete");
  }

  @DataProvider
  public static Object[][] incompleteConfigurationParametersSonarQube() {
    long applicationId = new Random().nextLong();
    String privateKey = randomAlphabetic(9);
    String apiEndpoint = randomAlphabetic(11);

    return generateNullCombination(new Object[] {
      applicationId,
      privateKey,
      apiEndpoint
    });
  }

  @Test
  public void toString_displays_complete_configuration() {
    long id = 34;
    String privateKey = randomAlphabetic(3);
    String apiEndpoint = randomAlphabetic(7);

    GithubAppConfiguration underTest = new GithubAppConfiguration(id, privateKey, apiEndpoint);

    assertThat(underTest)
      .hasToString(String.format("GithubAppConfiguration{id=%s, privateKey='***(3)***', apiEndpoint='%s'}", id, apiEndpoint));
  }

  @Test
  public void toString_displays_incomplete_configuration() {
    GithubAppConfiguration underTest = new GithubAppConfiguration(null, null, null);

    assertThat(underTest)
      .hasToString("GithubAppConfiguration{id=null, privateKey=null, apiEndpoint=null}");
  }

  @Test
  public void toString_displays_privateKey_as_stars() {
    GithubAppConfiguration underTest = new GithubAppConfiguration(null, randomAlphabetic(555), null);

    assertThat(underTest)
      .hasToString(
        "GithubAppConfiguration{id=null, privateKey='***(555)***', apiEndpoint=null}");
  }

  @Test
  public void equals_is_not_implemented() {
    long applicationId = new Random().nextLong();
    String privateKey = randomAlphabetic(8);
    String apiEndpoint = randomAlphabetic(7);

    GithubAppConfiguration underTest = new GithubAppConfiguration(applicationId, privateKey, apiEndpoint);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isNotEqualTo(new GithubAppConfiguration(applicationId, privateKey, apiEndpoint));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    long applicationId = new Random().nextLong();
    String privateKey = randomAlphabetic(8);
    String apiEndpoint = randomAlphabetic(7);

    GithubAppConfiguration underTest = new GithubAppConfiguration(applicationId, privateKey, apiEndpoint);

    assertThat(underTest).hasSameHashCodeAs(underTest);
    assertThat(underTest.hashCode()).isNotEqualTo(new GithubAppConfiguration(applicationId, privateKey, apiEndpoint));
  }

  private GithubAppConfiguration newValidConfiguration(long applicationId) {
    return new GithubAppConfiguration(applicationId, randomAlphabetic(6), randomAlphabetic(6));
  }

  private static Object[][] generateNullCombination(Object[] objects) {
    Object[][] firstPossibleValues = new Object[][] {
      {null},
      {objects[0]}
    };
    if (objects.length == 1) {
      return firstPossibleValues;
    }

    Object[][] subCombinations = generateNullCombination(ArrayUtils.subarray(objects, 1, objects.length));

    return Stream.of(subCombinations)
      .flatMap(combination -> Stream.of(firstPossibleValues).map(firstValue -> ArrayUtils.addAll(firstValue, combination)))
      .filter(array -> ArrayUtils.contains(array, null))
      .toArray(Object[][]::new);
  }
}
