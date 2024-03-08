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
package org.sonar.core.sarif;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.sonar.core.sarif.SarifVersionValidator.SUPPORTED_SARIF_VERSIONS;
import static org.sonar.core.sarif.SarifVersionValidator.UNSUPPORTED_VERSION_MESSAGE_TEMPLATE;

@RunWith(DataProviderRunner.class)
public class SarifVersionValidatorTest {

  @Test
  @UseDataProvider("unsupportedSarifVersions")
  public void sarif_version_validation_fails_if_version_is_not_supported(String version) {
    assertThatThrownBy(() -> SarifVersionValidator.validateSarifVersion(version))
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessage(format(UNSUPPORTED_VERSION_MESSAGE_TEMPLATE, version));
  }

  @Test
  @UseDataProvider("supportedSarifVersions")
  public void sarif_version_validation_succeeds_if_version_is_supported(String version) {
    assertThatCode(() -> SarifVersionValidator.validateSarifVersion(version))
      .doesNotThrowAnyException();
  }

  @DataProvider
  public static List<String> unsupportedSarifVersions() {
    List<String> unsupportedVersions = generateRandomUnsupportedSemanticVersions(10);
    unsupportedVersions.add(null);
    return unsupportedVersions;
  }

  @DataProvider
  public static Set<String> supportedSarifVersions() {
    return SUPPORTED_SARIF_VERSIONS;
  }

  private static List<String> generateRandomUnsupportedSemanticVersions(int amount) {
    return Stream
      .generate(SarifVersionValidatorTest::generateRandomSemanticVersion)
      .takeWhile(SarifVersionValidatorTest::isUnsupportedVersion)
      .limit(amount)
      .collect(Collectors.toList());
 }

  private static String generateRandomSemanticVersion() {
    return IntStream
      .rangeClosed(1, 3)
      .mapToObj(x -> RandomStringUtils.randomNumeric(1))
      .collect(Collectors.joining("."));
  }

  private static boolean isUnsupportedVersion(String version) {
    return !SUPPORTED_SARIF_VERSIONS.contains(version);
  }

}
