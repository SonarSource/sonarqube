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
package org.sonar.api.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {

  private DumpMapConfiguration underTest = new DumpMapConfiguration();

  @Test
  public void getBoolean_supports_heading_and_or_trailing_whitespaces() {
    boolean value = new Random().nextBoolean();

    verifySupportHeadAndOrTrailingWhitespaces(value, Configuration::getBoolean);
  }

  @Test
  public void getBoolean_returns_false_if_value_is_not_true() {
    verifyBooleanFalse("false");
    verifyBooleanFalse("False");
    verifyBooleanFalse("FALSE");
    verifyBooleanFalse("  false  ");
    verifyBooleanFalse("foo");
    verifyBooleanFalse("xxx");
    verifyBooleanFalse("___");
    verifyBooleanFalse("yes");
    verifyBooleanFalse("no");
  }

  @Test
  public void getBoolean_returns_true_if_value_is_true_ignore_case() {
    verifyBooleanTrue("true");
    verifyBooleanTrue("TRUE");
    verifyBooleanTrue("True");
    verifyBooleanTrue(" True ");
  }

  private void verifyBooleanFalse(String value) {
    underTest.put("foo", value);
    assertThat(underTest.getBoolean("foo")).hasValue(false);
  }

  private void verifyBooleanTrue(String value) {
    underTest.put("foo", value);
    assertThat(underTest.getBoolean("foo")).hasValue(true);
  }

  @Test
  public void getInt() {
    int value = new Random().nextInt();

    verifySupportHeadAndOrTrailingWhitespaces(value, Configuration::getInt);
  }

  @Test
  public void getLong() {
    long value = new Random().nextLong();

    verifySupportHeadAndOrTrailingWhitespaces(value, Configuration::getLong);
  }

  @Test
  public void getFloat() {
    float value = new Random().nextFloat();

    verifySupportHeadAndOrTrailingWhitespaces(value, Configuration::getFloat);
  }

  @Test
  public void getDouble() {
    double value = new Random().nextDouble();

    verifySupportHeadAndOrTrailingWhitespaces(value, Configuration::getDouble);
  }

  private <T> void verifySupportHeadAndOrTrailingWhitespaces(T value, BiFunction<Configuration, String, Optional<T>> t) {
    String randomKey = RandomStringUtils.randomAlphabetic(3);
    String randomNumberOfWhitespaces = StringUtils.repeat(" ", 1 + new Random().nextInt(10));

    assertThat(t.apply(underTest.put(randomKey, randomNumberOfWhitespaces + String.valueOf(value)), randomKey)).isEqualTo(Optional.of(value));
    assertThat(t.apply(underTest.put(randomKey, String.valueOf(value) + randomNumberOfWhitespaces), randomKey)).isEqualTo(Optional.of(value));
    assertThat(t.apply(underTest.put(randomKey, randomNumberOfWhitespaces + String.valueOf(value) + randomNumberOfWhitespaces), randomKey)).isEqualTo(Optional.of(value));
  }

  private static class DumpMapConfiguration implements Configuration {
    private final Map<String, String> keyValues = new HashMap<>();

    public Configuration put(String key, String value) {
      keyValues.put(key, value.trim());
      return this;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(keyValues.get(key));
    }

    @Override
    public boolean hasKey(String key) {
      throw new UnsupportedOperationException("hasKey not implemented");
    }

    @Override
    public String[] getStringArray(String key) {
      throw new UnsupportedOperationException("getStringArray not implemented");
    }
  }
}
