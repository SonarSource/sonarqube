/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

package org.sonar.api.batch;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.io.StringReader;

import javax.xml.stream.XMLStreamException;

import org.codehaus.staxmate.in.SMInputCursor;
import org.hibernate.lob.ReaderInputStream;
import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulesManager;

public class AbstractViolationsStaxParserTest {

  @Test
  public void testParseLineIndex() {

    assertThat(AbstractViolationsStaxParser.parseLineIndex("4"), is(4));
    assertNull(AbstractViolationsStaxParser.parseLineIndex("toto"));
    assertNull(AbstractViolationsStaxParser.parseLineIndex(""));
    assertNull(AbstractViolationsStaxParser.parseLineIndex(null));
    assertNull(AbstractViolationsStaxParser.parseLineIndex("-1"));
  }

  @Test
  public void testDoNotSaveViolationsOnUnexistedResource() throws XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    MyViolationParser violationParser = new MyViolationParser(context, null);
    violationParser.setDoSaveViolationsOnUnexistedResource(false);
    violationParser.parse(new ReaderInputStream(new StringReader("<root><file/></root>")));
  }

  @Test(expected = CursorForViolationsMethodHasBeenCalled.class)
  public void testDoSaveViolationsOnUnexistedResource() throws XMLStreamException {
    SensorContext context = mock(SensorContext.class);
    MyViolationParser violationParser = new MyViolationParser(context, null);
    violationParser.parse(new ReaderInputStream(new StringReader("<root><file/></root>")));
  }

  private class MyViolationParser extends AbstractViolationsStaxParser {

    protected MyViolationParser(SensorContext context, RulesManager rulesManager) {
      super(context, rulesManager);
    }

    protected SMInputCursor cursorForResources(SMInputCursor rootCursor) throws XMLStreamException {
      return rootCursor.descendantElementCursor("file");
    }

    protected SMInputCursor cursorForViolations(SMInputCursor resourcesCursor) throws XMLStreamException {
      throw new CursorForViolationsMethodHasBeenCalled();
    }

    protected Resource toResource(SMInputCursor resourceCursor) throws XMLStreamException {
      return new JavaFile("org.sonar.MyClass");
    }

    protected String messageFor(SMInputCursor violationCursor) throws XMLStreamException {
      return null;
    }

    protected String ruleKey(SMInputCursor violationCursor) throws XMLStreamException {
      return null;
    }

    protected String keyForPlugin() {
      return null;
    }

    protected String lineNumberForViolation(SMInputCursor violationCursor) throws XMLStreamException {
      return null;
    }
  }

  private class CursorForViolationsMethodHasBeenCalled extends RuntimeException {
  }
}
