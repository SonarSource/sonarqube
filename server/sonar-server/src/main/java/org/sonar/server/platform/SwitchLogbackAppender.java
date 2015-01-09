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
package org.sonar.server.platform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import org.sonar.server.computation.ComputationWorkerLauncher;

import java.util.Iterator;

public class SwitchLogbackAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

  private transient AppenderAttachableImpl<ILoggingEvent> attachedAppenders = new AppenderAttachableImpl<ILoggingEvent>();
  private transient Appender<ILoggingEvent> console = null;
  private transient Appender<ILoggingEvent> analysisReports = null;

  @Override
  protected void append(ILoggingEvent event) {
    if (Thread.currentThread().getName().startsWith(ComputationWorkerLauncher.THREAD_NAME_PREFIX)) {
      analysisReports.doAppend(event);
      if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
        console.doAppend(event);
      }
    } else {
      console.doAppend(event);
    }
  }

  @Override
  public void addAppender(Appender<ILoggingEvent> newAppender) {
    attachedAppenders.addAppender(newAppender);
    if ("CONSOLE".equals(newAppender.getName())) {
      console = newAppender;
    } else if ("ANALYSIS_REPORTS".equals(newAppender.getName())) {
      analysisReports = newAppender;
    } else {
      throw new IllegalArgumentException("Invalid appender name: " + newAppender.getName());
    }
  }

  @Override
  public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
    return attachedAppenders.iteratorForAppenders();
  }

  @Override
  public Appender<ILoggingEvent> getAppender(String name) {
    return attachedAppenders.getAppender(name);
  }

  @Override
  public boolean isAttached(Appender<ILoggingEvent> appender) {
    return attachedAppenders.isAttached(appender);
  }

  @Override
  public void detachAndStopAllAppenders() {
    attachedAppenders.detachAndStopAllAppenders();

  }

  @Override
  public boolean detachAppender(Appender<ILoggingEvent> appender) {
    return attachedAppenders.detachAppender(appender);
  }

  @Override
  public boolean detachAppender(String name) {
    return attachedAppenders.detachAppender(name);
  }

}
