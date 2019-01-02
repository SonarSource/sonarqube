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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.base.Throwables;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.core.util.CloseableIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ReportIterator<E> extends CloseableIterator<E> {

  private final Parser<E> parser;
  private InputStream stream;

  public ReportIterator(File file, Parser<E> parser) {
    try {
      this.parser = parser;
      this.stream = FileUtils.openInputStream(file);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected E doNext() {
    try {
      return parser.parseDelimitedFrom(stream);
    } catch (InvalidProtocolBufferException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected void doClose() {
    IOUtils.closeQuietly(stream);
  }
}
