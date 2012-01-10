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
package org.sonar.java.ast;

import org.sonar.squid.api.AnalysisException;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

class CheckstyleAuditListener implements AuditListener {

  /**
   * {@inheritDoc}
   */
  public void addError(AuditEvent evt) {
    // some projects can have file parsing errors (tapestry for example)
    // currently do not throw an error.
    // see
    // http://sourceforge.net/tracker/?func=detail&atid=397078&aid=1667137&group_id=29721
    if (evt.getMessage().contains("expecting EOF, found")) {
      return;
    }
    throw new AnalysisException(evt.getMessage() + ", file : " + evt.getFileName() + ", line : " + evt.getLine());
  }

  /**
   * {@inheritDoc}
   */
  public void addException(AuditEvent evt, Throwable throwable) {
  }

  /**
   * {@inheritDoc}
   */
  public void auditStarted(AuditEvent evt) {
  }

  /**
   * {@inheritDoc}
   */
  public void auditFinished(AuditEvent evt) {
  }

  /**
   * {@inheritDoc}
   */
  public void fileStarted(AuditEvent evt) {
  }

  /**
   * {@inheritDoc}
   */
  public void fileFinished(AuditEvent evt) {
  }

}
