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
package org.sonar.scanner.scm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.scanner.bootstrap.RawScannerProperties;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.fs.InputModuleHierarchy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScmRevisionImplTest {

  @Test
  public void test_priority_of_revision_sources() {
    assertThat(testGet(null, null, null)).isEmpty();
    assertThat(testGet("1b34d77", null, null)).hasValue("1b34d77");
    assertThat(testGet(null, "1b34d77", null)).hasValue("1b34d77");
    assertThat(testGet(null, null, "1b34d77")).hasValue("1b34d77");
    assertThat(testGet("1b34d77", "f6e62c5", "f6e62c5")).hasValue("1b34d77");
    assertThat(testGet(null, "1b34d77", "f6e62c5")).hasValue("1b34d77");
  }

  @Test
  public void test_empty_values() {
    assertThat(testGet("", "", "")).isEmpty();
    assertThat(testGet("1b34d77", "", "")).hasValue("1b34d77");
    assertThat(testGet("", "1b34d77", "")).hasValue("1b34d77");
    assertThat(testGet("", "", "1b34d77")).hasValue("1b34d77");
    assertThat(testGet("1b34d77", "f6e62c5", "f6e62c5")).hasValue("1b34d77");
    assertThat(testGet("", "1b34d77", "f6e62c5")).hasValue("1b34d77");
  }

  @Test
  public void ignore_failure_if_scm_does_not_support_revisions() {
    CiConfiguration ciConfiguration = mock(CiConfiguration.class);
    when(ciConfiguration.getScmRevision()).thenReturn(Optional.empty());
    Map<String, String> scannerConfiguration = new HashMap<>();
    ScmConfiguration scmConfiguration = mock(ScmConfiguration.class, RETURNS_DEEP_STUBS);
    when(scmConfiguration.provider().revisionId(any())).thenThrow(new UnsupportedOperationException("BOOM"));
    InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class, RETURNS_DEEP_STUBS);

    ScmRevisionImpl underTest = new ScmRevisionImpl(ciConfiguration, new RawScannerProperties(scannerConfiguration), scmConfiguration, moduleHierarchy);

    assertThat(underTest.get()).isEmpty();
  }

  private Optional<String> testGet(@Nullable String cliValue, @Nullable String ciValue, @Nullable String scmValue) {
    CiConfiguration ciConfiguration = mock(CiConfiguration.class);
    when(ciConfiguration.getScmRevision()).thenReturn(Optional.ofNullable(ciValue));
    Map<String, String> scannerConfiguration = new HashMap<>();
    scannerConfiguration.put("sonar.scm.revision", cliValue);
    ScmConfiguration scmConfiguration = mock(ScmConfiguration.class, RETURNS_DEEP_STUBS);
    when(scmConfiguration.provider().revisionId(any())).thenReturn(scmValue);
    InputModuleHierarchy moduleHierarchy = mock(InputModuleHierarchy.class, RETURNS_DEEP_STUBS);

    ScmRevisionImpl underTest = new ScmRevisionImpl(ciConfiguration, new RawScannerProperties(scannerConfiguration), scmConfiguration, moduleHierarchy);
    return underTest.get();
  }
}
