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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import java.util.List;
import java.util.Map;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

public class TestILoggingEvent implements ILoggingEvent {
  private String formattedMessage;

  public TestILoggingEvent(String formattedMessage) {
    this.formattedMessage = formattedMessage;
  }

  @Override
  public String getThreadName() {
    return null;
  }

  @Override
  public Level getLevel() {
    return null;
  }

  @Override
  public String getMessage() {
    return null;
  }

  @Override
  public Object[] getArgumentArray() {
    return null;
  }

  @Override
  public String getFormattedMessage() {
    return this.formattedMessage;
  }

  @Override
  public String getLoggerName() {
    return null;
  }

  @Override
  public LoggerContextVO getLoggerContextVO() {
    return null;
  }

  @Override
  public IThrowableProxy getThrowableProxy() {
    return null;
  }

  @Override
  public StackTraceElement[] getCallerData() {
    return new StackTraceElement[0];
  }

  @Override
  public boolean hasCallerData() {
    return false;
  }

  @Override
  public Marker getMarker() {
    return null;
  }

  @Override public List<Marker> getMarkerList() {
    return null;
  }

  @Override
  public Map<String, String> getMDCPropertyMap() {
    return null;
  }

  @Override
  public Map<String, String> getMdc() {
    return null;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override public int getNanoseconds() {
    return 0;
  }

  @Override public long getSequenceNumber() {
    return 0;
  }

  @Override public List<KeyValuePair> getKeyValuePairs() {
    return null;
  }

  @Override
  public void prepareForDeferredProcessing() {

  }
}
