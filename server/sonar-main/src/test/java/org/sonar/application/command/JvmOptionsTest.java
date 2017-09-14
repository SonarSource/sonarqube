/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application.command;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JvmOptionsTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final Random random = new Random();
  private final String randomPropertyName = RandomStringUtils.randomAlphanumeric(3);
  private final Properties properties = new Properties();
  private final Props props = new Props(properties);
  private final JvmOptions underTest = new JvmOptions();

  @Test
  public void constructor_without_arguments_creates_empty_JvmOptions() {
    JvmOptions<JvmOptions> testJvmOptions = new JvmOptions<>();

    assertThat(testJvmOptions.getAll()).isEmpty();
  }

  @Test
  public void constructor_throws_NPE_if_argument_is_null() {
    expectJvmOptionNotNullNPE();

    new JvmOptions(null);
  }

  @Test
  public void constructor_throws_NPE_if_any_argument_is_null() {
    ArrayList<String> nullList = new ArrayList<>();
    nullList.add(null);
    String[] arguments = Stream.of(
      Stream.of("-S1"),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-B" + i),
      nullList.stream(),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-A" + i)).flatMap(s -> s)
      .toArray(String[]::new);

    expectJvmOptionNotNullNPE();

    new JvmOptions(arguments);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void constructor_throws_IAE_if_argument_is_empty(String emptyString) {
    expectJvmOptionNotEmptyAndStartByDashIAE();

    new JvmOptions(emptyString);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void constructor_throws_IAE_if_any_argument_is_empty(String emptyString) {
    String[] arguments = Stream.of(
      Stream.of("-S1"),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-B" + i),
      Stream.of(emptyString),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-A" + i))
      .flatMap(s -> s)
      .toArray(String[]::new);

    expectJvmOptionNotEmptyAndStartByDashIAE();

    new JvmOptions(arguments);
  }

  @Test
  public void constructor_throws_IAE_if_argument_does_not_start_with_dash() {
    expectJvmOptionNotEmptyAndStartByDashIAE();

    new JvmOptions(RandomStringUtils.randomAlphanumeric(3));
  }

  @Test
  public void constructor_throws_IAE_if_any_argument_does_not_start_with_dash() {
    String[] arguments = Stream.of(
      Stream.of("-S1"),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-B" + i),
      Stream.of(RandomStringUtils.randomAlphanumeric(3)),
      IntStream.range(0, random.nextInt(2)).mapToObj(i -> "-A" + i))
      .flatMap(s -> s)
      .toArray(String[]::new);

    expectJvmOptionNotEmptyAndStartByDashIAE();

    new JvmOptions(arguments);
  }

  @Test
  public void add_throws_NPE_if_argument_is_null() {
    expectJvmOptionNotNullNPE();

    underTest.add(null);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void add_throws_IAE_if_argument_is_empty(String emptyString) {
    expectJvmOptionNotEmptyAndStartByDashIAE();

    underTest.add(emptyString);
  }

  @Test
  public void add_throws_IAE_if_argument_does_not_start_with_dash() {
    expectJvmOptionNotEmptyAndStartByDashIAE();

    underTest.add(RandomStringUtils.randomAlphanumeric(3));
  }

  @Test
  public void addFromMandatoryProperty_fails_with_IAE_if_property_does_not_exist() {
    expectMissingPropertyIAE(this.randomPropertyName);

    underTest.addFromMandatoryProperty(props, this.randomPropertyName);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void addFromMandatoryProperty_fails_with_IAE_if_property_contains_an_empty_value(String emptyString) {
    expectMissingPropertyIAE(this.randomPropertyName);

    underTest.addFromMandatoryProperty(props, randomPropertyName);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void addFromMandatoryProperty_adds_single_option_of_property_with_trimming(String emptyString) {
    properties.put(randomPropertyName, emptyString + "-foo" + emptyString);

    underTest.addFromMandatoryProperty(props, randomPropertyName);

    assertThat(underTest.getAll()).containsOnly("-foo");
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void addFromMandatoryProperty_fails_with_IAE_if_property_does_not_start_with_dash_after_trimmed(String emptyString) {
    properties.put(randomPropertyName, emptyString + "foo -bar");

    expectJvmOptionNotEmptyAndStartByDashIAE();

    underTest.addFromMandatoryProperty(props, randomPropertyName);
  }

  @Test
  @UseDataProvider("variousEmptyStrings")
  public void addFromMandatoryProperty_adds_options_of_property_with_trimming(String emptyString) {
    properties.put(randomPropertyName, emptyString + "-foo" + emptyString + " -bar" + emptyString + " -duck" + emptyString);

    underTest.addFromMandatoryProperty(props, randomPropertyName);

    assertThat(underTest.getAll()).containsOnly("-foo", "-bar", "-duck");
  }

  @Test
  public void addFromMandatoryProperty_supports_spaces_inside_options() {
    properties.put(randomPropertyName, "-foo bar -duck");

    underTest.addFromMandatoryProperty(props, randomPropertyName);

    assertThat(underTest.getAll()).containsOnly("-foo bar", "-duck");
  }

  @Test
  public void toString_prints_all_jvm_options() {
    underTest.add("-foo").add("-bar");

    assertThat(underTest.toString()).isEqualTo("[-foo, -bar]");
  }

  private void expectJvmOptionNotNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("a JVM option can't be null");
  }

  private void expectJvmOptionNotEmptyAndStartByDashIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("a JVM option can't be empty and must start with '-'");
  }

  public void expectMissingPropertyIAE(String randomPropertyName) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property: " + randomPropertyName);
  }

  @DataProvider()
  public static Object[][] variousEmptyStrings() {
    return new Object[][] {
      {""},
      {" "},
      {"     "}
    };
  }
}
