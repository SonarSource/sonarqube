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
/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Not intended to be instantiated by clients.</p>
 *
 * @since 2.2
 * @deprecated since 5.5
 */
@Deprecated
public class SourceCode {

  public static final String EOL = System.getProperty("line.separator", "\n");

  public abstract static class CodeLoader {
    private SoftReference<List<String>> code;

    public List<String> getCode() {
      List<String> c = null;
      if (code != null) {
        c = code.get();
      }
      if (c != null) {
        return c;
      }
      this.code = new SoftReference<>(load());
      return code.get();
    }

    public abstract String getFileName();

    protected abstract Reader getReader() throws Exception;

    protected List<String> load() {
      try (LineNumberReader lnr = new LineNumberReader(getReader())) {
        List<String> lines = new ArrayList<>();
        String currentLine;
        while ((currentLine = lnr.readLine()) != null) {
          lines.add(currentLine);
        }
        return lines;
      } catch (Exception e) {
        throw new IllegalStateException("Problem while reading " + getFileName() + ":" + e.getMessage(), e);
      }
    }
  }

  public static class FileCodeLoader extends CodeLoader {
    private File file;
    private String encoding;

    public FileCodeLoader(File file, String encoding) {
      this.file = file;
      this.encoding = encoding;
    }

    @Override
    public Reader getReader() throws Exception {
      return new InputStreamReader(new FileInputStream(file), encoding);
    }

    @Override
    public String getFileName() {
      return this.file.getAbsolutePath();
    }
  }

  public static class StringCodeLoader extends CodeLoader {
    public static final String DEFAULT_NAME = "CODE_LOADED_FROM_STRING";

    private String sourceCode;

    private String name;

    public StringCodeLoader(String code) {
      this(code, DEFAULT_NAME);
    }

    public StringCodeLoader(String code, String name) {
      this.sourceCode = code;
      this.name = name;
    }

    @Override
    public Reader getReader() {
      return new StringReader(sourceCode);
    }

    @Override
    public String getFileName() {
      return name;
    }
  }

  private CodeLoader cl;

  public SourceCode(CodeLoader cl) {
    this.cl = cl;
  }

  public List<String> getCode() {
    return cl.getCode();
  }

  public StringBuffer getCodeBuffer() {
    StringBuffer sb = new StringBuffer();
    List<String> lines = cl.getCode();
    for (String line : lines) {
      sb.append(line);
      sb.append(EOL);
    }
    return sb;
  }

  public String getSlice(int startLine, int endLine) {
    StringBuffer sb = new StringBuffer();
    List lines = cl.getCode();
    for (int i = (startLine == 0 ? startLine : (startLine - 1)); i < endLine && i < lines.size(); i++) {
      if (sb.length() != 0) {
        sb.append(EOL);
      }
      sb.append((String) lines.get(i));
    }
    return sb.toString();
  }

  /**
   * Within Sonar Ecosystem - absolute path to file containing code,
   * whereas in fact existence of such file not guaranteed - see {@link StringCodeLoader}.
   */
  public String getFileName() {
    return cl.getFileName();
  }
}
