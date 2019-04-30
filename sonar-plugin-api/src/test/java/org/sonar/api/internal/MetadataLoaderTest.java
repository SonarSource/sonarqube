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
package org.sonar.api.internal;

import java.io.File;
import java.net.MalformedURLException;
import org.sonar.api.SonarEdition;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataLoaderTest {
  private System2 system = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void load_version_from_file_in_classpath() {
    Version version = MetadataLoader.loadVersion(System2.INSTANCE);
    assertThat(version).isNotNull();
    assertThat(version.major()).isGreaterThanOrEqualTo(5);
  }

  @Test
  public void load_edition_from_file_in_classpath() {
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);
    assertThat(edition).isNotNull();
  }

  @Test
  public void load_edition_defaults_to_community_if_file_not_found() throws MalformedURLException {
    when(system.getResource(anyString())).thenReturn(new File("target/unknown").toURI().toURL());
    SonarEdition edition = MetadataLoader.loadEdition(System2.INSTANCE);
    assertThat(edition).isEqualTo(SonarEdition.COMMUNITY);
  }

  @Test
  public void throw_ISE_if_edition_is_invalid() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Invalid edition found in '/sonar-edition.txt': 'TRASH'");

    MetadataLoader.parseEdition("trash");
  }

  @Test
  public void throw_ISE_if_fail_to_load_version() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not load /sonar-api-version.txt from classpath");

    when(system.getResource(anyString())).thenReturn(new File("target/unknown").toURI().toURL());
    MetadataLoader.loadVersion(system);
  }

}
