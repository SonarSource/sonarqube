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
package org.sonarqube.qa.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import static java.util.Objects.requireNonNull;

/**
 * Watch log files, usually server logs (see Orchestrator.getServer().get*Logs()).
 * This class allows to not load the full content in memory.
 */
public class LogsTailer implements AutoCloseable {

  private final List<Tailer> tailers;
  private final LogConsumer logConsumer;

  private LogsTailer(Builder builder) {
    logConsumer = new LogConsumer(builder.consumers);
    tailers = builder.files.stream()
      .map(file -> Tailer.create(file, logConsumer, 500))
      .collect(Collectors.toList());
  }

  public Watch watch(String text) {
    return new Watch(text);
  }

  @Override
  public void close() {
    for (Tailer tailer : tailers) {
      tailer.stop();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<File> files = new ArrayList<>();
    private final List<Consumer<String>> consumers = new ArrayList<>();

    public Builder addFile(File file) {
      this.files.add(file);
      return this;
    }

    public Builder addFiles(File file, File... otherFiles) {
      this.files.add(file);
      Collections.addAll(this.files, otherFiles);
      return this;
    }

    /**
     * Adds a consumer that is called on each new line appended
     * to the files.
     * Note that the consumer {@link Content} allows to keep
     * all past logs in memory.
     */
    public Builder addConsumer(Consumer<String> consumer) {
      this.consumers.add(consumer);
      return this;
    }

    public LogsTailer build() {
      return new LogsTailer(this);
    }
  }

  private static class LogConsumer extends TailerListenerAdapter {
    private final List<Consumer<String>> consumers = Collections.synchronizedList(new ArrayList<>());

    private LogConsumer(List<Consumer<String>> consumers) {
      this.consumers.addAll(consumers);
    }

    @Override
    public void handle(String line) {
      synchronized (consumers) {
        for (Consumer<String> consumer : consumers) {
          try {
            consumer.accept(line);
          } catch (Exception e) {
            // do not prevent other consumers to handle the log
            e.printStackTrace();
          }
        }
      }
    }

    public void add(Consumer<String> consumer) {
      this.consumers.add(consumer);
    }

    public void remove(Consumer<String> consumer) {
      this.consumers.remove(consumer);
    }
  }

  public class Watch implements AutoCloseable {
    private final String expectedText;
    private final CountDownLatch foundSignal = new CountDownLatch(1);
    private String log = null;
    private final Consumer<String> consumer;

    private Watch(String expectedText) {
      this.expectedText = requireNonNull(expectedText);
      this.consumer = l -> {
        if (l.contains(this.expectedText)) {
          this.log = l;
          foundSignal.countDown();
        }
      };
      logConsumer.add(consumer);
    }

    /**
     * Blocks until the expected log appears in watched files.
     */
    public void waitForLog() throws InterruptedException {
      foundSignal.await();
    }

    /**
     * Blocks until the expected log appears in watched files with timeout
     */
    public boolean waitForLog(long timeout, TimeUnit timeUnit) throws InterruptedException {
      return foundSignal.await(timeout, timeUnit);
    }

    public Optional<String> getLog() {
      return Optional.ofNullable(log);
    }

    @Override
    public void close() {
      logConsumer.remove(consumer);
    }
  }

  public static class Content implements Consumer<String> {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void accept(String s) {
      lines.add(s);
    }

    public boolean hasText(String text) {
      synchronized (lines) {
        for (String line : lines) {
          if (line.contains(text)) {
            return true;
          }
        }
      }
      return false;
    }

    public boolean hasLineMatching(Pattern pattern) {
      synchronized (lines) {
        for (String line : lines) {
          if (pattern.matcher(line).matches()) {
            return true;
          }
        }
      }
      return false;
    }
  }
}
