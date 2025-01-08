/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.web;

import java.io.IOException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.platformlevel.PlatformLevel;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiV2ServletTest {

  @Test
  public void getServletConfig_shouldReturnServletConfig() throws ServletException {
    ApiV2Servlet underTest = new ApiV2Servlet();
    ServletConfig mockServletConfig = mock(ServletConfig.class);
    underTest.init(mockServletConfig);

    assertThat(underTest.getServletConfig()).isEqualTo(mockServletConfig);
  }

  @Test
  public void getServletInfo_shouldReturnOwnDefinedServletName() {
    ApiV2Servlet underTest = new ApiV2Servlet();

    assertThat(underTest.getServletInfo())
      .isEqualTo(ApiV2Servlet.SERVLET_NAME);
  }

  @Test
  public void init_shouldInitDispatcherSafeModeConfig_whenDispatcherHadBeenInitWithoutConfig() throws ServletException {
    ApiV2Servlet underTest = new ApiV2Servlet();
    DispatcherServlet mockDispatcherServletSafeMode = mock(DispatcherServlet.class);
    underTest.setServletProvider(context -> mockDispatcherServletSafeMode);
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    underTest.initDispatcherSafeMode(mockPlatformLevel);

    ServletConfig mockServletConfig = mock(ServletConfig.class);
    underTest.init(mockServletConfig);

    verify(mockDispatcherServletSafeMode, times(1)).init(mockServletConfig);
  }

  @Test
  public void init_shouldInitDispatcherLevel4Config_whenDispatcherHadBeenInitWithoutConfig() throws ServletException {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();

    DispatcherServlet mockDispatcherServletLevel4 = mock(DispatcherServlet.class);
    underTest.setServletProvider(context -> mockDispatcherServletLevel4);
    underTest.initDispatcherLevel4(mockPlatformLevel);

    ServletConfig mockServletConfig = mock(ServletConfig.class);

    underTest.init(mockServletConfig);

    verify(mockDispatcherServletLevel4, times(1)).init(mockServletConfig);
  }

  @Test
  public void service_shouldDispatchOnRightDispatcher() throws ServletException, IOException {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();
    DispatcherServlet mockDispatcherServletSafeMode = mock(DispatcherServlet.class);
    DispatcherServlet mockDispatcherServletLevel4 = mock(DispatcherServlet.class);

    underTest.setServletProvider(context -> mockDispatcherServletSafeMode);
    underTest.initDispatcherSafeMode(mockPlatformLevel);
    underTest.init(mock(ServletConfig.class));
    ServletRequest mockRequest1 = mock(ServletRequest.class);
    ServletResponse mockResponse1 = mock(ServletResponse.class);
    underTest.service(mockRequest1, mockResponse1);
    verify(mockDispatcherServletSafeMode, times(1)).service(mockRequest1, mockResponse1);

    underTest.setServletProvider(context -> mockDispatcherServletLevel4);
    underTest.initDispatcherLevel4(mockPlatformLevel);
    ServletRequest mockRequest2 = mock(ServletRequest.class);
    ServletResponse mockResponse2 = mock(ServletResponse.class);
    underTest.service(mockRequest2, mockResponse2);
    verify(mockDispatcherServletLevel4, times(1)).service(mockRequest2, mockResponse2);
  }

  @Test
  public void service_shouldReturnNotFound_whenDispatchersAreNotAvailable() throws ServletException, IOException {
    ApiV2Servlet underTest = new ApiV2Servlet();
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    underTest.service(mock(ServletRequest.class), mockResponse);

    verify(mockResponse, times(1)).sendError(SC_NOT_FOUND);
  }

  @Test
  public void initDispatcherServlet_shouldThrowRuntimeException_whenDispatcherInitFails() throws ServletException {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();
    ServletConfig mockServletConfig = mock(ServletConfig.class);
    String exceptionMessage = "Exception message";

    DispatcherServlet mockDispatcherServletSafeMode = mock(DispatcherServlet.class);
    doThrow(new ServletException(exceptionMessage)).when(mockDispatcherServletSafeMode).init(mockServletConfig);

    underTest.setServletProvider(context -> mockDispatcherServletSafeMode);
    underTest.init(mockServletConfig);

    assertThatThrownBy(() -> underTest.initDispatcherSafeMode(mockPlatformLevel))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMessage);
  }

  @Test
  public void initDispatcherServlet_initLevel4ShouldDestroySafeMode() throws ServletException {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();

    DispatcherServlet mockDispatcherServletSafeMode = mock(DispatcherServlet.class);
    underTest.setServletProvider(context -> mockDispatcherServletSafeMode);
    underTest.initDispatcherSafeMode(mockPlatformLevel);
    underTest.init(mock(ServletConfig.class));

    underTest.setServletProvider(context -> mock(DispatcherServlet.class));

    underTest.initDispatcherLevel4(mockPlatformLevel);

    verify(mockDispatcherServletSafeMode, times(1)).destroy();
  }

  @Test
  public void destroy_shouldDestroyDispatcherLevel4() {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();
    DispatcherServlet mockDispatcherServletLevel4 = mock(DispatcherServlet.class);

    underTest.setServletProvider(context -> mockDispatcherServletLevel4);
    underTest.initDispatcherLevel4(mockPlatformLevel);

    underTest.destroy();

    verify(mockDispatcherServletLevel4, times(1)).destroy();
  }

  @Test
  public void destroy_shouldDestroyDispatcherSafeMode() throws ServletException {
    PlatformLevel mockPlatformLevel = getMockPlatformLevel();
    ApiV2Servlet underTest = new ApiV2Servlet();
    DispatcherServlet mockDispatcherServletSafeMode = mock(DispatcherServlet.class);

    underTest.setServletProvider(context -> mockDispatcherServletSafeMode);
    underTest.initDispatcherSafeMode(mockPlatformLevel);
    underTest.init(mock(ServletConfig.class));

    underTest.destroy();

    verify(mockDispatcherServletSafeMode, times(1)).destroy();
  }

  private static PlatformLevel getMockPlatformLevel() {
    SpringComponentContainer mockSpringComponentContainer = mock(SpringComponentContainer.class);
    when(mockSpringComponentContainer.context()).thenReturn(new AnnotationConfigApplicationContext());
    PlatformLevel mockPlatformLevel = mock(PlatformLevel.class);
    when(mockPlatformLevel.getContainer()).thenReturn(mockSpringComponentContainer);
    return mockPlatformLevel;
  }
}
