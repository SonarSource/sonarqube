/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.web;

import java.io.IOException;
import javax.servlet.ServletContextEvent;
import org.jruby.rack.RackApplicationFactory;
import org.jruby.rack.servlet.ServletRackContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.server.platform.web.PlatformServletContextListener;
import org.sonar.server.platform.web.RubyRailsContextListener;

import static java.lang.Boolean.TRUE;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RubyRailsContextListenerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ServletContextEvent event = mock(ServletContextEvent.class, Mockito.RETURNS_DEEP_STUBS);
  RubyRailsContextListener underTest = new RubyRailsContextListener();

  @Test
  public void do_not_initialize_rails_if_error_during_startup() {
    when(event.getServletContext().getAttribute(PlatformServletContextListener.STARTED_ATTRIBUTE)).thenReturn(null);

    underTest.contextInitialized(event);

    verify(event.getServletContext(), never()).setAttribute(anyString(), anyObject());
  }

  @Test
  public void initialize_rails_if_no_errors_during_startup() {
    when(event.getServletContext().getAttribute(PlatformServletContextListener.STARTED_ATTRIBUTE)).thenReturn(TRUE);
    underTest.contextInitialized(event);
    // Ruby environment is started
    // See RailsServletContextListener -> RackServletContextListener
    verify(event.getServletContext()).setAttribute(eq("rack.factory"), anyObject());
  }

  @Test
  public void always_propagates_initialization_errors() {
    expectedException.expect(RuntimeException.class);

    underTest.handleInitializationException(new IOException(), mock(RackApplicationFactory.class), mock(ServletRackContext.class));
  }
}
