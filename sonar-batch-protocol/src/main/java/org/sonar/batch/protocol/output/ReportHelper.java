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
package org.sonar.batch.protocol.output;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.batch.protocol.GsonHelper;
import org.sonar.batch.protocol.output.component.ReportComponents;
import org.sonar.batch.protocol.output.issue.ReportIssue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ReportHelper {

  private static final String COMPONENTS_JSON = "components.json";
  private final File reportRootDir;
  private final Gson gson = GsonHelper.create();

  private ReportHelper(File reportRootDir) {
    this.reportRootDir = reportRootDir;
  }

  public static ReportHelper create(File workDirectory) {
    if (!workDirectory.exists() && !workDirectory.mkdirs()) {
      throw new IllegalStateException("Unable to create directory " + workDirectory);
    }
    return new ReportHelper(workDirectory);
  }

  public File reportRootDir() {
    return reportRootDir;
  }

  public void saveComponents(ReportComponents components) {
    File resourcesFile = new File(reportRootDir, COMPONENTS_JSON);
    try {
      FileUtils.write(resourcesFile, components.toJson());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write components", e);
    }
  }

  public void saveIssues(long componentBatchId, Iterable<ReportIssue> issues) {
    File issuesFile = getIssuesFile(componentBatchId);
    try (OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(issuesFile)), "UTF-8")) {

      JsonWriter writer = new JsonWriter(out);
      writer.setIndent("  ");
      writer.beginArray();
      for (ReportIssue reportIssue : issues) {
        gson.toJson(reportIssue, ReportIssue.class, writer);
      }
      writer.endArray();
      writer.close();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to save issues", e);
    }
  }

  private File getIssuesFile(long componentBatchId) {
    return new File(getComponentFolder(componentBatchId), "issues-" + componentBatchId + ".json");
  }

  private File getComponentFolder(long componentBatchId) {
    File folder = new File(reportRootDir, Long.toString(componentBatchId));
    if (!folder.exists() && !folder.mkdir()) {
      throw new IllegalStateException("Unable to create directory " + folder);
    }
    return folder;
  }

  public ReportComponents getComponents() {
    File file = new File(reportRootDir, COMPONENTS_JSON);

    try (InputStream resourcesStream = new FileInputStream(file)) {
      String json = IOUtils.toString(resourcesStream);
      return ReportComponents.fromJson(json);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read issues", e);
    }
  }

  public Iterable<ReportIssue> getIssues(final long componentBatchId) {

    return new Iterable<ReportIssue>() {
      @Override
      public Iterator<ReportIssue> iterator() {
        return new ReportIssueIterator(getIssuesFile(componentBatchId));
      }
    };
  }

  private final class ReportIssueIterator implements Iterator<ReportIssue> {

    private JsonReader reader;

    public ReportIssueIterator(File issuesFile) {
      try {
        reader = new JsonReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(issuesFile)), Charsets.UTF_8));
        reader.beginArray();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to read " + issuesFile, e);
      }
    }

    @Override
    public boolean hasNext() {
      try {
        if (reader.hasNext()) {
          return true;
        }
        reader.endArray();
        reader.close();
        return false;
      } catch (IOException e) {
        IOUtils.closeQuietly(reader);
        throw new IllegalStateException("Unable to iterate over JSON file ", e);
      }
    }

    @Override
    public ReportIssue next() {
      try {
        if (!reader.hasNext()) {
          throw new NoSuchElementException();
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to iterate over JSON file ", e);
      }
      return gson.fromJson(reader, ReportIssue.class);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }

}
