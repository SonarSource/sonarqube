/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ui;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.servlet.WebappMetricsFilter;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.MetricRegistryLocator;
import org.sonar.server.platform.Platform;

import javax.management.MBeanServer;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class MonitoringFilter extends WebappMetricsFilter {

  public static final String REGISTRY_ATTRIBUTE = MonitoringFilter.class.getName() + ".registry";

  private static final String NAME_PREFIX = "responseCodes.";
  private static final int OK = 200;
  private static final int CREATED = 201;
  private static final int NO_CONTENT = 204;
  private static final int BAD_REQUEST = 400;
  private static final int NOT_FOUND = 404;
  private static final int SERVER_ERROR = 500;

  private MetricRegistry httpMetricsRegistry;

  public MonitoringFilter() {
    super(REGISTRY_ATTRIBUTE, createMeterNamesByStatusCode(), NAME_PREFIX + "other");
  }

  private static Map<Integer, String> createMeterNamesByStatusCode() {
      final Map<Integer, String> meterNamesByStatusCode = new HashMap<Integer, String>(6);
      meterNamesByStatusCode.put(OK, NAME_PREFIX + "ok");
      meterNamesByStatusCode.put(CREATED, NAME_PREFIX + "created");
      meterNamesByStatusCode.put(NO_CONTENT, NAME_PREFIX + "noContent");
      meterNamesByStatusCode.put(BAD_REQUEST, NAME_PREFIX + "badRequest");
      meterNamesByStatusCode.put(NOT_FOUND, NAME_PREFIX + "notFound");
      meterNamesByStatusCode.put(SERVER_ERROR, NAME_PREFIX + "serverError");
      return meterNamesByStatusCode;
  }

  // for test purposes
  boolean isJmxMonitoringActive() {
    Settings settings
      = Platform.getInstance().getContainer().getComponentByType(Settings.class);
    return settings.getBoolean("sonar.jmx.monitoring");
  }


  public void init(FilterConfig config) throws ServletException {
    if (isJmxMonitoringActive()) {
      MetricRegistry registry = MetricRegistryLocator.INSTANCE.getRegistry();
      config.getServletContext().setAttribute(REGISTRY_ATTRIBUTE, registry);
      config.getServletContext().setAttribute("com.codahale.metrics.servlets.MetricsServlet.registry", registry);
      super.init(config);
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      registry.register("jvm-bufferpool", new BufferPoolMetricSet(mBeanServer));
      registry.register("jvm-gc", new GarbageCollectorMetricSet());
      registry.register("jvm-mem", new MemoryUsageGaugeSet());
      registry.register("jvm-thread", new ThreadStatesGaugeSet());
      JmxReporter.forRegistry(registry).inDomain("sonar").build().start();
    }
  }

  public void destroy() {
    // not needed
  }

  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    if (httpMetricsRegistry == null) {
      chain.doFilter(request, response);
    } else {
      super.doFilter(request, response, chain);
    }

  }

}
