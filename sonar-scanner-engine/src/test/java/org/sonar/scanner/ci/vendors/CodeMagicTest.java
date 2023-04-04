/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.ci.vendors;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CodeMagicTest {

  private final System2 system = mock(System2.class);
  private final CiVendor underTest = new CodeMagic(system);

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("CodeMagic");
  }

  @Test
  public void isDetected_givenBuildID_detectCodeMagic() {
    setEnvVariable("FCI_BUILD_ID", "1");

    assertThat(underTest.isDetected()).isTrue();
  }

  @Test
  public void isDetected_givenNoEnvVariable_dontDetectCodeMagic() {
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration_commitEnvVariableAvailable_addScmRevisionToConfig() {
    setEnvVariable("FCI_BUILD_ID", "1");
    setEnvVariable("FCI_COMMIT", "d9d25f70ec9023f7acc0c520c8b67204229a5c7e");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("d9d25f70ec9023f7acc0c520c8b67204229a5c7e");
  }

  @Test
  public void loadConfiguration_commitEnvVariableNotAvailable_addScmRevisionToConfig() {
    setEnvVariable("FCI_BUILD_ID", "1");

    CiConfiguration ciConfiguration = underTest.loadConfiguration();
    List<String> logs = logTester.logs(Level.WARN);

    assertThat(ciConfiguration.getScmRevision()).isEmpty();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).isEqualTo("Missing environment variable FCI_COMMIT");
  }


  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
