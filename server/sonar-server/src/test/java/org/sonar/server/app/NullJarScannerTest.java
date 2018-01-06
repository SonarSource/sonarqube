/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.app;

import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.junit.Test;

import javax.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class NullJarScannerTest {
  @Test
  public void does_nothing() {
    ServletContext context = mock(ServletContext.class);
    JarScannerCallback callback = mock(JarScannerCallback.class);

    NullJarScanner scanner = new NullJarScanner();

    scanner.scan(JarScanType.PLUGGABILITY, context, callback);
    verifyZeroInteractions(context, callback);
    scanner.setJarScanFilter(mock(JarScanFilter.class));
    assertThat(scanner.getJarScanFilter()).isNull();
  }
}
