/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ruby;

import org.jruby.rack.RackApplication;
import org.jruby.rack.RackApplicationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlatformRackBridgeTest {
  @Mock
  private ServletContext servletContext;
  @Mock
  private RackApplicationFactory rackApplicationFactory;
  @Mock
  private RackApplication rackApplication;

  @InjectMocks
  PlatformRackBridge underTest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = RuntimeException.class)
  public void getRubyRuntime_throws_RE_when_RackApplicationFactory_is_not_in_ServletContext() throws Exception {
    underTest.getRubyRuntime();
  }

  @Test
  public void getRubyRuntime_returns_Ruby_instance_from_rack_application() throws Exception {
    when(servletContext.getAttribute("rack.factory")).thenReturn(rackApplicationFactory);
    when(rackApplicationFactory.getApplication()).thenReturn(rackApplication);

    // since Ruby object can not be mocked and creating a Ruby instance is costly, we only make sure rackApplication#getRuntime method is
    // called and that we get null (as rackApplication#getRuntime() returns null)
    assertThat(underTest.getRubyRuntime()).isNull();
    verify(rackApplication).getRuntime();
  }
}
