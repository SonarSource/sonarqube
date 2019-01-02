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
package org.sonar.scanner.genericcoverage;

import com.ctc.wstx.stax.WstxInputFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;

public class StaxParser {

  private SMInputFactory inf;
  private XmlStreamHandler streamHandler;
  private boolean isoControlCharsAwareParser;

  /**
   * Stax parser for a given stream handler and iso control chars set awarness to off
   *
   * @param streamHandler the xml stream handler
   */
  public StaxParser(XmlStreamHandler streamHandler) {
    this(streamHandler, false);
  }

  /**
   * Stax parser for a given stream handler and iso control chars set awarness to on.
   * The iso control chars in the xml file will be replaced by simple spaces, usefull for
   * potentially bogus XML files to parse, this has a small perfs overhead so use it only when necessary
   *
   * @param streamHandler              the xml stream handler
   * @param isoControlCharsAwareParser true or false
   */
  public StaxParser(XmlStreamHandler streamHandler, boolean isoControlCharsAwareParser) {
    this.streamHandler = streamHandler;
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    if (xmlFactory instanceof WstxInputFactory) {
      WstxInputFactory wstxInputfactory = (WstxInputFactory) xmlFactory;
      wstxInputfactory.configureForLowMemUsage();
      wstxInputfactory.getConfig().setUndeclaredEntityResolver(new UndeclaredEntitiesXMLResolver());
    }
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    this.isoControlCharsAwareParser = isoControlCharsAwareParser;
    inf = new SMInputFactory(xmlFactory);
  }

  public void parse(File xmlFile) throws XMLStreamException {
    FileInputStream input = null;
    try {
      input = new FileInputStream(xmlFile);
      parse(input);
    } catch (FileNotFoundException e) {
      throw new XMLStreamException(e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public void parse(InputStream xmlInput) throws XMLStreamException {
    xmlInput = isoControlCharsAwareParser ? new ISOControlCharAwareInputStream(xmlInput) : xmlInput;
    parse(inf.rootElementCursor(xmlInput));
  }

  public void parse(Reader xmlReader) throws XMLStreamException {
    if (isoControlCharsAwareParser) {
      throw new IllegalStateException("Method call not supported when isoControlCharsAwareParser=true");
    }
    parse(inf.rootElementCursor(xmlReader));
  }

  public void parse(URL xmlUrl) throws XMLStreamException {
    try {
      parse(xmlUrl.openStream());
    } catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  private void parse(SMHierarchicCursor rootCursor) throws XMLStreamException {
    try {
      streamHandler.stream(rootCursor);
    } finally {
      rootCursor.getStreamReader().closeCompletely();
    }
  }

  private static class UndeclaredEntitiesXMLResolver implements XMLResolver {
    @Override
    public Object resolveEntity(String arg0, String arg1, String fileName, String undeclaredEntity) throws XMLStreamException {
      // avoid problems with XML docs containing undeclared entities.. return the entity under its raw form if not an unicode expression
      if (StringUtils.startsWithIgnoreCase(undeclaredEntity, "u") && undeclaredEntity.length() == 5) {
        int unicodeCharHexValue = Integer.parseInt(undeclaredEntity.substring(1), 16);
        if (Character.isDefined(unicodeCharHexValue)) {
          undeclaredEntity = new String(new char[] {(char) unicodeCharHexValue});
        }
      }
      return undeclaredEntity;
    }
  }

  /**
   * Simple interface for handling XML stream to parse
   */
  public interface XmlStreamHandler {
    void stream(SMHierarchicCursor rootCursor) throws XMLStreamException;
  }

  private static class ISOControlCharAwareInputStream extends InputStream {

    private InputStream inputToCheck;

    public ISOControlCharAwareInputStream(InputStream inputToCheck) {
      super();
      this.inputToCheck = inputToCheck;
    }

    @Override
    public int read() throws IOException {
      return inputToCheck.read();
    }

    @Override
    public int available() throws IOException {
      return inputToCheck.available();
    }

    @Override
    public void close() throws IOException {
      inputToCheck.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      inputToCheck.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
      return inputToCheck.markSupported();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int readen = inputToCheck.read(b, off, len);
      checkBufferForISOControlChars(b, off, len);
      return readen;
    }

    @Override
    public int read(byte[] b) throws IOException {
      int readen = inputToCheck.read(b);
      checkBufferForISOControlChars(b, 0, readen);
      return readen;
    }

    @Override
    public synchronized void reset() throws IOException {
      inputToCheck.reset();
    }

    @Override
    public long skip(long n) throws IOException {
      return inputToCheck.skip(n);
    }

    private static void checkBufferForISOControlChars(byte[] buffer, int off, int len) {
      for (int i = off; i < len; i++) {
        char streamChar = (char) buffer[i];
        if (Character.isISOControl(streamChar) && streamChar != '\n') {
          // replace control chars by a simple space
          buffer[i] = ' ';
        }
      }
    }
  }
}
