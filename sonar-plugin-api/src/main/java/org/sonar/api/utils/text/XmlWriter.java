/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.utils.text;

import javax.annotation.Nullable;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Writer;

/**
 * TODO document that output is UTF-8
 */
public class XmlWriter {

  private final XMLStreamWriter stream;

  private XmlWriter(Writer writer) {
    try {
      XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
      stream = xmlFactory.createXMLStreamWriter(writer);
    } catch (Exception e) {
      throw new WriterException("Fail to initialize XML Stream writer", e);
    }
  }

  public static XmlWriter of(Writer writer) {
    return new XmlWriter(writer);
  }

  public XmlWriter declaration() {
    try {
      stream.writeStartDocument();
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  public XmlWriter begin(String nodeName) {
    try {
      stream.writeStartElement(nodeName);
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }


  public XmlWriter end() {
    try {
      stream.writeEndElement();
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  /**
   * Same as {@link #end()}. The parameter is unused. It's declared only to improve
   * readability :
   * <pre>
   *   xml.write("rules");
   *   xml.write("rule");
   *   // many other writes
   *   xml.end("rule");
   *   xml.end("rules");
   * </pre>
   */
  public XmlWriter end(String unused) {
    return end();
  }

  public XmlWriter prop(String nodeName, @Nullable String value) {
    if (value != null) {
      begin(nodeName).value(value).end();
    }
    return this;
  }

  public XmlWriter prop(String nodeName, @Nullable Number value) {
    if (value != null) {
      begin(nodeName).value(value).end();
    }
    return this;
  }

  public XmlWriter prop(String nodeName, boolean value) {
    return begin(nodeName).value(value).end();
  }

  public XmlWriter prop(String nodeName, long value) {
    return begin(nodeName).value(value).end();
  }

  public XmlWriter prop(String nodeName, double value) {
    return begin(nodeName).value(value).end();
  }

  private XmlWriter value(boolean value) {
    try {
      stream.writeCharacters(String.valueOf(value));
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  private XmlWriter value(double value) {
    try {
      if (Double.isNaN(value) || Double.isInfinite(value)) {
        throw new WriterException("Fail to write XML. Double value is not valid: " + value);
      }
      stream.writeCharacters(String.valueOf(value));
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  private XmlWriter value(@Nullable String value) {
    try {
      if (value != null) {
        stream.writeCharacters(value);
      }
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  private XmlWriter value(long value) {
    try {
      stream.writeCharacters(String.valueOf(value));
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  private XmlWriter value(@Nullable Number value) {
    try {
      if (value != null) {
        stream.writeCharacters(String.valueOf(value));
      }
      return this;
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  public void close() {
    try {
      stream.writeEndDocument();
      stream.flush();
      stream.close();
    } catch (XMLStreamException e) {
      throw rethrow(e);
    }
  }

  private static IllegalStateException rethrow(XMLStreamException e) {
    // stacktrace is not helpful
    throw new IllegalStateException("Fail to write XML: " + e.getMessage());
  }
}
