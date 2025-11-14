/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.process.Props;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class EsJvmOptionsTest {
  @Rule
  public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final Properties properties = new Properties();

  @Before
  public void before() {
    properties.put("sonar.path.logs", "path_to_logs");
  }

  @Test
  public void constructor_sets_mandatory_JVM_options() throws IOException {
    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .containsExactlyInAnyOrder(
        "-XX:+UseG1GC",
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(),
        "-XX:ErrorFile=" + Paths.get("path_to_logs/es_hs_err_pid%p.log").toAbsolutePath(),
        "-Des.networkaddress.cache.ttl=60",
        "-Des.networkaddress.cache.negative.ttl=10",
        "-XX:+AlwaysPreTouch",
        "-Xss1m",
        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8",
        "-Djna.nosys=true",
        "-Djna.tmpdir=" + tmpDir.getAbsolutePath(),
        "-XX:-OmitStackTraceInFastThrow",
        "-Dio.netty.noUnsafe=true",
        "-Dio.netty.noKeySetOptimization=true",
        "-Dio.netty.recycler.maxCapacityPerThread=0",
        "-Dio.netty.allocator.numDirectArenas=0",
        "-Dlog4j.shutdownHookEnabled=false",
        "-Dlog4j2.disable.jmx=true",
        "-Dlog4j2.formatMsgNoLookups=true",
        "-Djava.locale.providers=COMPAT",
        "-Des.enforce.bootstrap.checks=true",
        "-Xlog:disable");
  }

  @Test
  public void constructor_does_not_force_boostrap_checks_if_sonarqube_property_is_true() throws IOException {
    properties.put("sonar.es.bootstrap.checks.disable", "true");
    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .doesNotContain("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void constructor_forces_boostrap_checks_if_jdbc_url_property_does_not_exist() throws IOException {
    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .contains("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void constructor_forces_boostrap_checks_if_jdbc_url_property_is_not_h2() throws IOException {
    properties.put("sonar.jdbc.url", secure().nextAlphanumeric(53));
    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .contains("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void constructor_does_not_force_boostrap_checks_if_jdbc_url_property_contains_h2() throws IOException {
    properties.put("sonar.jdbc.url", "jdbc:h2:tcp://ffoo:bar/sonar");
    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .doesNotContain("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void boostrap_checks_can_be_set_true_if_h2() throws IOException {
    properties.put("sonar.jdbc.url", "jdbc:h2:tcp://ffoo:bar/sonar");
    properties.put("sonar.es.bootstrap.checks.disable", "true");

    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .doesNotContain("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void boostrap_checks_can_be_set_false_if_h2() throws IOException {
    properties.put("sonar.jdbc.url", "jdbc:h2:tcp://ffoo:bar/sonar");
    properties.put("sonar.es.bootstrap.checks.disable", "false");

    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .contains("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void boostrap_checks_can_be_set_true_if_jdbc_other_than_h2() throws IOException {
    properties.put("sonar.jdbc.url", secure().nextAlphanumeric(53));
    properties.put("sonar.es.bootstrap.checks.disable", "true");

    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .doesNotContain("-Des.enforce.bootstrap.checks=true");
  }

  @Test
  public void boostrap_checks_can_be_set_false_if_jdbc_other_than_h2() throws IOException {
    properties.put("sonar.jdbc.url", secure().nextAlphanumeric(53));
    properties.put("sonar.es.bootstrap.checks.disable", "false");

    File tmpDir = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir);

    assertThat(underTest.getAll())
      .isNotEmpty()
      .contains("-Des.enforce.bootstrap.checks=true");
  }

  /**
   * This test may fail if SQ's test are not executed with target Java version 8.
   */
  @Test
  public void writeToJvmOptionFile_writes_all_JVM_options_to_file_with_warning_header() throws IOException {
    File tmpDir = temporaryFolder.newFolder("with space");
    File file = temporaryFolder.newFile();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), tmpDir)
      .add("-foo")
      .add("-bar");

    underTest.writeToJvmOptionFile(file);

    assertThat(file).hasContent(
      "# This file has been automatically generated by SonarQube during startup.\n" +
        "# Please use sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to specify jvm options for Elasticsearch\n" +
        "\n" +
        "# DO NOT EDIT THIS FILE\n" +
        "\n" +
        "-XX:+UseG1GC\n" +
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath() + "\n" +
        "-XX:ErrorFile=" + Paths.get("path_to_logs/es_hs_err_pid%p.log").toAbsolutePath() + "\n" +
        "-Xlog:disable\n" +
        "-Des.networkaddress.cache.ttl=60\n" +
        "-Des.networkaddress.cache.negative.ttl=10\n" +
        "-XX:+AlwaysPreTouch\n" +
        "-Xss1m\n" +
        "-Djava.awt.headless=true\n" +
        "-Dfile.encoding=UTF-8\n" +
        "-Djna.nosys=true\n" +
        "-Djna.tmpdir=" + tmpDir.getAbsolutePath() + "\n" +
        "-XX:-OmitStackTraceInFastThrow\n" +
        "-Dio.netty.noUnsafe=true\n" +
        "-Dio.netty.noKeySetOptimization=true\n" +
        "-Dio.netty.recycler.maxCapacityPerThread=0\n" +
        "-Dio.netty.allocator.numDirectArenas=0\n" +
        "-Dlog4j.shutdownHookEnabled=false\n" +
        "-Dlog4j2.disable.jmx=true\n" +
        "-Dlog4j2.formatMsgNoLookups=true\n" +
        "-Djava.locale.providers=COMPAT\n" +
        "-Des.enforce.bootstrap.checks=true\n" +
        "-foo\n" +
        "-bar");

  }

  @Test
  public void writeToJvmOptionFile_throws_ISE_in_case_of_IOException() throws IOException {
    File notAFile = temporaryFolder.newFolder();
    EsJvmOptions underTest = new EsJvmOptions(new Props(properties), temporaryFolder.newFolder());

    assertThatThrownBy(() -> underTest.writeToJvmOptionFile(notAFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Cannot write Elasticsearch jvm options file")
      .hasRootCauseInstanceOf(IOException.class);
  }
}
