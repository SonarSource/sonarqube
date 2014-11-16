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
package org.sonar.server.es;

import org.elasticsearch.action.ActionRequest;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class BulkIndexRequestIterator<INPUT> implements Iterator<ActionRequest> {

  public static interface InputConverter<INPUT> {
    List<ActionRequest> convert(INPUT input);
  }

  private final Iterator<INPUT> input;
  private final InputConverter<INPUT> converter;
  private Iterator<ActionRequest> currents = null;

  public BulkIndexRequestIterator(Iterable<INPUT> input, InputConverter<INPUT> converter) {
    this.input = input.iterator();
    this.converter = converter;
    if (this.input.hasNext()) {
      this.currents = converter.convert(this.input.next()).iterator();
    }
  }

  @Override
  public boolean hasNext() {
    return currents != null && currents.hasNext();
  }

  @Override
  public ActionRequest next() {
    if (currents == null) {
      throw new NoSuchElementException();
    }
    ActionRequest request = currents.next();
    peekNext();
    return request;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private void peekNext() {
    if (!currents.hasNext()) {
      if (input.hasNext()) {
        currents = converter.convert(input.next()).iterator();
      } else {
        currents = null;
      }
    }
  }
}
