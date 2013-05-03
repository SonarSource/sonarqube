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

import com.codahale.metrics.servlet.WebappMetricsFilter;

import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import com.codahale.metrics.jvm.GarbageCollectorMetricSet;

import com.codahale.metrics.jvm.BufferPoolMetricSet;

import org.sonar.api.config.Settings;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sonar.server.platform.Platform;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

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

  public void init(FilterConfig config) throws ServletException {
    Settings settings = Platform.getInstance().getContainer()
        .getComponentByType(Settings.class);
    boolean jmxActive = settings.getBoolean("sonar.jmx.monitoring");
    if (jmxActive) {
      httpMetricsRegistry = new MetricRegistry();
      config.getServletContext().setAttribute(REGISTRY_ATTRIBUTE, httpMetricsRegistry);
      super.init(config);
      JmxReporter.forRegistry(httpMetricsRegistry).inDomain("sonar-http").build().start();

      MetricRegistry jvmBufferPoolMetricsRegistry = new MetricRegistry();
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      jvmBufferPoolMetricsRegistry.registerAll(new BufferPoolMetricSet(mBeanServer));
	  JmxReporter.forRegistry(jvmBufferPoolMetricsRegistry).inDomain("sonar-jvm-buffer-pool").build().start();
	  
	  MetricRegistry jvmGcMetricsRegistry = new MetricRegistry();
      jvmGcMetricsRegistry.registerAll(new GarbageCollectorMetricSet());
	  JmxReporter.forRegistry(jvmGcMetricsRegistry).inDomain("sonar-jvm-gc").build().start();
	  
	  MetricRegistry jvmMemMetricsRegistry = new MetricRegistry();
      jvmMemMetricsRegistry.registerAll(new MemoryUsageGaugeSet());
	  JmxReporter.forRegistry(jvmMemMetricsRegistry).inDomain("sonar-jvm-mem").build().start();
	  
	  MetricRegistry jvmThreadMetricsRegistry = new MetricRegistry();
      jvmThreadMetricsRegistry.registerAll(new ThreadStatesGaugeSet());
      JmxReporter.forRegistry(jvmThreadMetricsRegistry).inDomain("sonar-jvm-thread").build().start();
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
