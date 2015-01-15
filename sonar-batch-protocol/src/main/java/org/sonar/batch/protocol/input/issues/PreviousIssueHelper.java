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
package org.sonar.batch.protocol.input.issues;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.sonar.batch.protocol.GsonHelper;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PreviousIssueHelper {

  private final Gson gson = GsonHelper.create();

  private PreviousIssueHelper() {
  }

  public static PreviousIssueHelper create() {
    return new PreviousIssueHelper();
  }

  public static interface Function<F, T> {
    T apply(@Nullable F from);
  }

  public <G> void streamIssues(Writer out, Iterable<G> issues, Function<G, PreviousIssue> converter) {
    try {
      JsonWriter writer = new JsonWriter(out);
      writer.setIndent("  ");
      writer.beginArray();
      for (G issue : issues) {
        gson.toJson(converter.apply(issue), PreviousIssue.class, writer);
      }
      writer.endArray();
      writer.close();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to stream issues", e);
    }
  }

  public Iterable<PreviousIssue> getIssues(final Reader reader) {

    return new Iterable<PreviousIssue>() {
      @Override
      public Iterator<PreviousIssue> iterator() {
        return new PreviousIssueIterator(reader);
      }
    };
  }

  private final class PreviousIssueIterator implements Iterator<PreviousIssue> {

    private JsonReader jsonreader;

    public PreviousIssueIterator(Reader reader) {
      try {
        jsonreader = new JsonReader(reader);
        jsonreader.beginArray();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read issues", e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        if (jsonreader.hasNext()) {
          return true;
        }
        jsonreader.endArray();
        jsonreader.close();
        return false;
      } catch (IOException e) {
        throw new IllegalStateException("Unable to iterate over JSON file ", e);
      }
    }

    @Override
    public PreviousIssue next() {
      try {
        if (!jsonreader.hasNext()) {
          throw new NoSuchElementException();
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to iterate over JSON file ", e);
      }
      return gson.fromJson(jsonreader, PreviousIssue.class);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }

}
