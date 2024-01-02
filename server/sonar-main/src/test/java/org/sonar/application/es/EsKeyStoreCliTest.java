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
package org.sonar.application.es;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.application.command.JavaCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EsKeyStoreCliTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  EsInstallation esInstallation = mock(EsInstallation.class);

  @Test
  public void execute_command_should_preserve_order_of_properties() throws Exception {
    File homeDir = temp.newFolder();
    File confDir = temp.newFolder();
    when(esInstallation.getHomeDirectory()).thenReturn(homeDir);
    when(esInstallation.getConfDirectory()).thenReturn(confDir);

    EsKeyStoreCli underTest = EsKeyStoreCli.getInstance(esInstallation);
    underTest
      .store("test.property1", "value1")
      .store("test.property2", "value2")
      .store("test.property3", "value3");

    MockProcess process = (MockProcess) underTest.executeWith(EsKeyStoreCliTest::mockLaunch);

    JavaCommand<?> executedCommand = process.getExecutedCommand();

    String expectedHomeLibPath = Paths.get(homeDir.toString(), "lib") + "/*";
    String expectedHomeKeystorePath = Paths.get(homeDir.toString(), "lib", "tools", "keystore-cli") + "/*";

    assertThat(executedCommand.getClassName()).isEqualTo("org.elasticsearch.common.settings.KeyStoreCli");
    assertThat(executedCommand.getClasspath())
      .containsExactly(expectedHomeLibPath, expectedHomeKeystorePath);
    assertThat(executedCommand.getParameters()).containsExactly("add", "-x", "-f", "test.property1", "test.property2", "test.property3");
    assertThat(executedCommand.getJvmOptions().getAll()).containsExactly(
      "-Xshare:auto",
      "-Xms4m",
      "-Xmx64m",
      "-Des.path.home=" + homeDir.toPath(),
      "-Des.path.conf=" + confDir.toPath(),
      "-Des.distribution=default",
      "-Des.distribution.type=tar");

    verify(process.getOutputStream()).write(argThat(new ArrayContainsMatcher("value1\nvalue2\nvalue3\n")), eq(0), eq(21));
    verify(process.getOutputStream(), atLeastOnce()).flush();
    verify(process.getMock()).waitFor(1L, TimeUnit.MINUTES);
  }

  @Test
  public void ISE_if_process_exited_abnormally() throws Exception {
    File homeDir = temp.newFolder();
    File confDir = temp.newFolder();
    when(esInstallation.getHomeDirectory()).thenReturn(homeDir);
    when(esInstallation.getConfDirectory()).thenReturn(confDir);

    EsKeyStoreCli underTest = EsKeyStoreCli.getInstance(esInstallation);
    underTest.store("test.property1", "value1");

    assertThatThrownBy(() -> underTest.executeWith(EsKeyStoreCliTest::mockFailureLaunch))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Elasticsearch KeyStore tool exited with code: 1");
  }

  @Test
  public void fail_if_tries_to_store_null_key() throws Exception {
    File homeDir = temp.newFolder();
    File confDir = temp.newFolder();
    when(esInstallation.getHomeDirectory()).thenReturn(homeDir);
    when(esInstallation.getConfDirectory()).thenReturn(confDir);

    EsKeyStoreCli underTest = EsKeyStoreCli.getInstance(esInstallation);
    assertThatThrownBy(() -> underTest.store(null, "value1"))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Property key cannot be null");
  }

  @Test
  public void fail_if_tries_to_store_null_value() throws Exception {
    File homeDir = temp.newFolder();
    File confDir = temp.newFolder();
    when(esInstallation.getHomeDirectory()).thenReturn(homeDir);
    when(esInstallation.getConfDirectory()).thenReturn(confDir);

    EsKeyStoreCli underTest = EsKeyStoreCli.getInstance(esInstallation);
    assertThatThrownBy(() -> underTest.store("key", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Property value cannot be null");
  }

  private static MockProcess mockLaunch(JavaCommand<?> javaCommand) {
    return new MockProcess(javaCommand);
  }

  private static MockProcess mockFailureLaunch(JavaCommand<?> javaCommand) {
    return new MockProcess(javaCommand, 1);
  }

  public static class ArrayContainsMatcher implements ArgumentMatcher<byte[]> {
    private final String left;

    public ArrayContainsMatcher(String left) {
      this.left = left;
    }

    @Override
    public boolean matches(byte[] right) {
      return new String(right).startsWith(left);
    }
  }

  private static class MockProcess extends Process {
    JavaCommand<?> executedCommand;
    Process process;
    OutputStream outputStream = mock(OutputStream.class);

    public MockProcess(JavaCommand<?> executedCommand) {
      this(executedCommand, 0);
    }

    public MockProcess(JavaCommand<?> executedCommand, int exitCode) {
      this.executedCommand = executedCommand;
      process = mock(Process.class);
      when(process.getOutputStream()).thenReturn(outputStream);
      when(process.exitValue()).thenReturn(exitCode);
    }

    public Process getMock() {
      return process;
    }

    public JavaCommand<?> getExecutedCommand() {
      return executedCommand;
    }

    @Override
    public OutputStream getOutputStream() {
      return outputStream;
    }

    @Override
    public InputStream getInputStream() {
      return null;
    }

    @Override
    public InputStream getErrorStream() {
      return null;
    }

    @Override
    public int waitFor() throws InterruptedException {
      process.waitFor();
      return 0;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
      process.waitFor(timeout, unit);
      return true;
    }

    @Override
    public int exitValue() {
      return process.exitValue();
    }

    @Override
    public void destroy() {

    }
  }

}
