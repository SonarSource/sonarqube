/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.steps;

import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.ce.task.projectexport.steps.DumpElement.METADATA;

public class FakeDumpWriter implements DumpWriter {

  private boolean published = false;
  private final Map<DumpElement<?>, ListStreamWriter<?>> writersByDumpElement = new HashMap<>();
  private final Map<DumpElement<?>, Integer> failureThresholds = new HashMap<>();

  @Override
  public void write(ProjectDump.Metadata metadata) {
    checkNotPublished();
    newStreamWriter(METADATA).write(metadata);
  }

  /**
   * @throws IllegalStateException if metadata have not been written yet.
   * @see #write(ProjectDump.Metadata)
   */
  public ProjectDump.Metadata getMetadata() {
    return getWrittenMessagesOf(METADATA).get(0);
  }

  /**
   * @throws IllegalStateException if messages have not been written yet for the specified {@code DumpElement}
   * @see #newStreamWriter(DumpElement)
   */
  @SuppressWarnings("unchecked")
  public <MSG extends Message> List<MSG> getWrittenMessagesOf(DumpElement<MSG> elt) {
    ListStreamWriter<MSG> writer = (ListStreamWriter<MSG>) writersByDumpElement.get(elt);
    checkState(writer != null);
    return writer.messages;
  }

  @Override
  public <MSG extends Message> StreamWriter<MSG> newStreamWriter(DumpElement<MSG> elt) {
    checkNotPublished();
    checkState(!writersByDumpElement.containsKey(elt));

    int failureThreshold = MoreObjects.firstNonNull(failureThresholds.get(elt), Integer.MAX_VALUE);
    ListStreamWriter<MSG> writer = new ListStreamWriter<>(failureThreshold);
    writersByDumpElement.put(elt, writer);
    return writer;
  }

  /**
   * The stream returned by {@link #newStreamWriter(DumpElement)} will throw an
   * {@link IllegalStateException} if more than {@code count} messages are written.
   * By default no exception is thrown.
   */
  public void failIfMoreThan(int count, DumpElement element) {
    failureThresholds.put(element, count);
  }

  @Override
  public void publish() {
    checkNotPublished();
    published = true;
  }

  private void checkNotPublished() {
    checkState(!published, "Dump is already published");
  }

  private static class ListStreamWriter<MSG extends Message> implements StreamWriter<MSG> {
    private final List<MSG> messages = new ArrayList<>();
    private final int failureThreshold;

    private ListStreamWriter(int failureThreshold) {
      checkArgument(failureThreshold >= 0, "Threshold (%d) must be positive", failureThreshold);
      this.failureThreshold = failureThreshold;
    }

    @Override
    public void write(MSG msg) {
      checkState(messages.size() < failureThreshold, "Maximum of %d written messages has been reached", failureThreshold);
      messages.add(msg);
    }

    @Override
    public void close() {
      // nothing to do
    }
  }
}
