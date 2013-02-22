/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.application;

import org.mortbay.log.Logger;
import org.mortbay.log.StdErrLog;

public class FilteredLogger implements Logger {
  private final String name;
  private final Logger delegate;

  public FilteredLogger() {
    this(null);
  }

  private FilteredLogger(String name) {
    this.name = name;
    this.delegate = new StdErrLog(name);
  }

  public boolean isDebugEnabled() {
    return delegate.isDebugEnabled();
  }

  public void setDebugEnabled(boolean enabled) {
    delegate.setDebugEnabled(enabled);
  }

  public void info(String msg, Object arg0, Object arg1) {
    if (msg.contains("JVM BUG(s)")) {
      // Ignore, see SONAR-3866
      return;
    }
    delegate.info(msg, arg0, arg1);
  }

  public void debug(String msg, Throwable th) {
    delegate.debug(msg, th);
  }

  public void debug(String msg, Object arg0, Object arg1) {
    delegate.debug(msg, arg0, arg1);
  }

  public void warn(String msg, Object arg0, Object arg1) {
    delegate.warn(msg, arg0, arg1);
  }

  public void warn(String msg, Throwable th) {
    delegate.warn(msg, th);
  }

  public Logger getLogger(String name) {
    if ((name == null && this.name == null) || ((name != null) && name.equals(this.name))) {
      return this;
    }
    return new FilteredLogger(name);
  }
}
