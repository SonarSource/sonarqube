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
package org.sonar.plugins.checkstyle;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import org.junit.Test;

public class CheckstyleAuditListenerTest {
  @Test
  public void testUtilityMethods() {
    AuditEvent event;

    event = new AuditEvent(this, "", new LocalizedMessage(0, "", "", null, "", CheckstyleAuditListenerTest.class, "msg"));
    assertThat(CheckstyleAuditListener.getLineId(event), nullValue());
    assertThat(CheckstyleAuditListener.getMessage(event), is("msg"));
    assertThat(CheckstyleAuditListener.getRuleKey(event), is(CheckstyleAuditListenerTest.class.getName()));

    event = new AuditEvent(this, "", new LocalizedMessage(1, "", "", null, "", CheckstyleAuditListenerTest.class, "msg"));
    assertThat(CheckstyleAuditListener.getLineId(event), is(1));
    assertThat(CheckstyleAuditListener.getMessage(event), is("msg"));
    assertThat(CheckstyleAuditListener.getRuleKey(event), is(CheckstyleAuditListenerTest.class.getName()));

    event = new AuditEvent(this);
    assertThat(CheckstyleAuditListener.getLineId(event), nullValue());
    assertThat(CheckstyleAuditListener.getMessage(event), nullValue());
    assertThat(CheckstyleAuditListener.getRuleKey(event), nullValue());
  }
}
