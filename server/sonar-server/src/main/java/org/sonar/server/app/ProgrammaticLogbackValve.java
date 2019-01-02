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
package org.sonar.server.app;

import ch.qos.logback.access.tomcat.LogbackValve;
import ch.qos.logback.core.util.ExecutorServiceUtil;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.commons.lang.reflect.FieldUtils;

/**
 * Implementation of {@link ch.qos.logback.access.tomcat.LogbackValve} that does not
 * rely on the required file logback-access.xml. It allows to be configured
 * programmatically.
 */
public class ProgrammaticLogbackValve extends LogbackValve {

  @Override
  public synchronized void startInternal() throws LifecycleException {
    try {
      // direct coupling with LogbackValve implementation
      FieldUtils.writeField(this, "scheduledExecutorService", ExecutorServiceUtil.newScheduledExecutorService(), true);
      FieldUtils.writeField(this, "started", true, true);
      setState(LifecycleState.STARTING);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
