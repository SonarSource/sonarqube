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
package org.sonar.application.command;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.application.es.EsInstallation;
import org.sonar.process.System2;
import org.sonar.test.ExceptionCauseMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class EsJvmOptionsTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @UseDataProvider("java8or11")
  public void constructor_sets_mandatory_JVM_options_on_Java_8_and_11(System2 system2) throws IOException {
    File tmpDir = temporaryFolder.newFolder();
    File logDir = temporaryFolder.newFolder();
    EsInstallation esInstallation = mock(EsInstallation.class);
    when(esInstallation.getTmpDirectory()).thenReturn(tmpDir);
    when(esInstallation.getLogDirectory()).thenReturn(logDir);

    EsJvmOptions underTest = new EsJvmOptions(system2, esInstallation);

    assertThat(underTest.getAll())
      .containsExactly(
        "-XX:+UseConcMarkSweepGC",
        "-XX:CMSInitiatingOccupancyFraction=75",
        "-XX:+UseCMSInitiatingOccupancyOnly",
        "-Des.networkaddress.cache.ttl=60",
        "-Des.networkaddress.cache.negative.ttl=10",
        "-XX:+AlwaysPreTouch",
        "-Xss1m",
        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8",
        "-Djna.nosys=true",
        "-XX:-OmitStackTraceInFastThrow",
        "-Dio.netty.noUnsafe=true",
        "-Dio.netty.noKeySetOptimization=true",
        "-Dio.netty.recycler.maxCapacityPerThread=0",
        "-Dlog4j.shutdownHookEnabled=false",
        "-Dlog4j2.disable.jmx=true",
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(),
        "-XX:ErrorFile=" + new File(logDir, "es_hs_err_pid%p.log").getAbsolutePath());
  }

  @DataProvider
  public static Object[][] java8or11() {
    System2 java8 = mock(System2.class);
    when(java8.isJava9()).thenReturn(false);
    when(java8.isJava10()).thenReturn(false);
    System2 java10 = mock(System2.class);
    when(java10.isJava9()).thenReturn(false);
    when(java10.isJava10()).thenReturn(false);
    return new Object[][] {
      {java8},
      {java10}
    };
  }

  @Test
  public void constructor_sets_mandatory_JVM_options_on_Java_9() throws IOException {
    System2 java9 = mock(System2.class);
    when(java9.isJava9()).thenReturn(true);
    when(java9.isJava10()).thenReturn(false);

    File tmpDir = temporaryFolder.newFolder();
    File logDir = temporaryFolder.newFolder();
    EsInstallation esInstallation = mock(EsInstallation.class);
    when(esInstallation.getTmpDirectory()).thenReturn(tmpDir);
    when(esInstallation.getLogDirectory()).thenReturn(logDir);

    EsJvmOptions underTest = new EsJvmOptions(java9, esInstallation);

    assertThat(underTest.getAll())
      .containsExactly(
        "-XX:+UseConcMarkSweepGC",
        "-XX:CMSInitiatingOccupancyFraction=75",
        "-XX:+UseCMSInitiatingOccupancyOnly",
        "-Des.networkaddress.cache.ttl=60",
        "-Des.networkaddress.cache.negative.ttl=10",
        "-XX:+AlwaysPreTouch",
        "-Xss1m",
        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8",
        "-Djna.nosys=true",
        "-XX:-OmitStackTraceInFastThrow",
        "-Dio.netty.noUnsafe=true",
        "-Dio.netty.noKeySetOptimization=true",
        "-Dio.netty.recycler.maxCapacityPerThread=0",
        "-Dlog4j.shutdownHookEnabled=false",
        "-Dlog4j2.disable.jmx=true",
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(),
        "-XX:ErrorFile=" + new File(logDir, "es_hs_err_pid%p.log").getAbsolutePath(),
        "-Djava.locale.providers=COMPAT");
  }

  @Test
  public void constructor_sets_mandatory_JVM_options_on_Java_10() throws IOException {
    System2 java10 = mock(System2.class);
    when(java10.isJava9()).thenReturn(false);
    when(java10.isJava10()).thenReturn(true);

    File tmpDir = temporaryFolder.newFolder();
    File logDir = temporaryFolder.newFolder();
    EsInstallation esInstallation = mock(EsInstallation.class);
    when(esInstallation.getTmpDirectory()).thenReturn(tmpDir);
    when(esInstallation.getLogDirectory()).thenReturn(logDir);

    EsJvmOptions underTest = new EsJvmOptions(java10, esInstallation);

    assertThat(underTest.getAll())
      .containsExactly(
        "-XX:+UseConcMarkSweepGC",
        "-XX:CMSInitiatingOccupancyFraction=75",
        "-XX:+UseCMSInitiatingOccupancyOnly",
        "-Des.networkaddress.cache.ttl=60",
        "-Des.networkaddress.cache.negative.ttl=10",
        "-XX:+AlwaysPreTouch",
        "-Xss1m",
        "-Djava.awt.headless=true",
        "-Dfile.encoding=UTF-8",
        "-Djna.nosys=true",
        "-XX:-OmitStackTraceInFastThrow",
        "-Dio.netty.noUnsafe=true",
        "-Dio.netty.noKeySetOptimization=true",
        "-Dio.netty.recycler.maxCapacityPerThread=0",
        "-Dlog4j.shutdownHookEnabled=false",
        "-Dlog4j2.disable.jmx=true",
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath(),
        "-XX:ErrorFile=" + new File(logDir, "es_hs_err_pid%p.log").getAbsolutePath(),
        "-XX:UseAVX=2");
  }

  /**
   * This test may fail if SQ's test are not executed with target Java version 8.
   */
  @Test
  public void writeToJvmOptionFile_writes_all_JVM_options_to_file_with_warning_header() throws IOException {
    File tmpDir = temporaryFolder.newFolder("with space");
    File logDir = temporaryFolder.newFolder();
    EsInstallation esInstallation = mock(EsInstallation.class);
    when(esInstallation.getTmpDirectory()).thenReturn(tmpDir);
    when(esInstallation.getLogDirectory()).thenReturn(logDir);
    File file = temporaryFolder.newFile();

    EsJvmOptions underTest = new EsJvmOptions(esInstallation)
      .add("-foo")
      .add("-bar");

    underTest.writeToJvmOptionFile(file);

    assertThat(file).hasContent(
      "# This file has been automatically generated by SonarQube during startup.\n" +
        "# Please use sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to specify jvm options for Elasticsearch\n" +
        "\n" +
        "# DO NOT EDIT THIS FILE\n" +
        "\n" +
        "-XX:+UseConcMarkSweepGC\n" +
        "-XX:CMSInitiatingOccupancyFraction=75\n" +
        "-XX:+UseCMSInitiatingOccupancyOnly\n" +
        "-Des.networkaddress.cache.ttl=60\n" +
        "-Des.networkaddress.cache.negative.ttl=10\n" +
        "-XX:+AlwaysPreTouch\n" +
        "-Xss1m\n" +
        "-Djava.awt.headless=true\n" +
        "-Dfile.encoding=UTF-8\n" +
        "-Djna.nosys=true\n" +
        "-XX:-OmitStackTraceInFastThrow\n" +
        "-Dio.netty.noUnsafe=true\n" +
        "-Dio.netty.noKeySetOptimization=true\n" +
        "-Dio.netty.recycler.maxCapacityPerThread=0\n" +
        "-Dlog4j.shutdownHookEnabled=false\n" +
        "-Dlog4j2.disable.jmx=true\n" +
        "-Djava.io.tmpdir=" + tmpDir.getAbsolutePath() + "\n" +
        "-XX:ErrorFile=" + new File(logDir, "es_hs_err_pid%p.log").getAbsolutePath() + "\n" +
        "-foo\n" +
        "-bar");

  }

  @Test
  public void writeToJvmOptionFile_throws_ISE_in_case_of_IOException() throws IOException {
    File notAFile = temporaryFolder.newFolder();
    File tmpDir = temporaryFolder.newFolder();
    File logDir = temporaryFolder.newFolder();
    EsInstallation esInstallation = mock(EsInstallation.class);
    when(esInstallation.getTmpDirectory()).thenReturn(tmpDir);
    when(esInstallation.getLogDirectory()).thenReturn(logDir);

    EsJvmOptions underTest = new EsJvmOptions(esInstallation);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cannot write Elasticsearch jvm options file");
    expectedException.expectCause(ExceptionCauseMatcher.hasType(IOException.class));

    underTest.writeToJvmOptionFile(notAFile);
  }
}
