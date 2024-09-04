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
package org.sonar.server.platform.web;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.sonar.server.platform.platformlevel.PlatformLevel;
import org.sonar.server.v2.config.PlatformLevel4WebConfig;
import org.sonar.server.v2.config.SafeModeWebConfig;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class ApiV2Servlet implements Servlet {
  public static final String SERVLET_NAME = "WebAPI V2 Servlet";
  private Function<WebApplicationContext, DispatcherServlet> servletProvider;
  private ServletConfig config = null;
  private DispatcherServlet dispatcherLevel4 = null;
  private DispatcherServlet dispatcherSafeMode = null;

  private final CountDownLatch safeModeInitializationCompletedLatch = new CountDownLatch(2);

  public ApiV2Servlet() {
    this.servletProvider = DispatcherServlet::new;
  }

  @VisibleForTesting
  void setServletProvider(Function<WebApplicationContext, DispatcherServlet> servletProvider) {
    this.servletProvider = servletProvider;
  }

  @Override
  public String getServletInfo() {
    return SERVLET_NAME;
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    if (dispatcherLevel4 != null) {
      dispatcherLevel4.init(config);
    }
    if (dispatcherSafeMode != null) {
      dispatcherSafeMode.init(config);
      safeModeInitializationCompletedLatch.countDown();
    }
  }

  public void initDispatcherSafeMode(PlatformLevel platformLevel) {
    dispatcherSafeMode = initDispatcherServlet(platformLevel, SafeModeWebConfig.class);
    safeModeInitializationCompletedLatch.countDown();
  }

  public void initDispatcherLevel4(PlatformLevel platformLevel) {
    dispatcherLevel4 = initDispatcherServlet(platformLevel, PlatformLevel4WebConfig.class);
    destroyDispatcherSafeMode();
  }

  private DispatcherServlet initDispatcherServlet(PlatformLevel platformLevel, Class<?> configClass) {
    AnnotationConfigWebApplicationContext springMvcContext = new AnnotationConfigWebApplicationContext();
    springMvcContext.setBeanNameGenerator(FullyQualifiedAnnotationBeanNameGenerator.INSTANCE);
    springMvcContext.setAllowBeanDefinitionOverriding(false);
    springMvcContext.setParent(platformLevel.getContainer().context());
    springMvcContext.register(configClass);
    if (PlatformLevel4WebConfig.class.equals(configClass)) {
      platformLevel.getContainer().getWebApiV2ConfigurationClasses().forEach(springMvcContext::register);
    }

    DispatcherServlet dispatcher = servletProvider.apply(springMvcContext);
    try {
      if (config != null) {
        dispatcher.init(config);
      }
    } catch (ServletException e) {
      throw new RuntimeException(e);
    }
    return dispatcher;
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    if (dispatcherSafeMode != null) {
      dispatcherSafeMode.service(req, res);
    } else if (dispatcherLevel4 != null) {
      dispatcherLevel4.service(req, res);
    } else {
      HttpServletResponse httpResponse = (HttpServletResponse) res;
      httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  public void destroy() {
    destroyDispatcherSafeMode();
    destroyLevel4();
  }

  private void destroyDispatcherSafeMode() {
    if (dispatcherSafeMode != null) {
      try {
        safeModeInitializationCompletedLatch.await();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(ie);
      }
      DispatcherServlet dispatcherToDestroy = dispatcherSafeMode;
      dispatcherSafeMode = null;
      dispatcherToDestroy.destroy();
    }
  }

  private void destroyLevel4() {
    if (dispatcherLevel4 != null) {
      dispatcherLevel4.destroy();
    }
  }
}
