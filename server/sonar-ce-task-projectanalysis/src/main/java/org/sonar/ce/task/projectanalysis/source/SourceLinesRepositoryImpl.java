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

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.util.CloseableIterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

public class SourceLinesRepositoryImpl implements SourceLinesRepository {

  private final BatchReportReader reportReader;

  public SourceLinesRepositoryImpl(BatchReportReader reportReader) {
    this.reportReader = reportReader;
  }

  @Override
  public CloseableIterator<String> readLines(Component file) {
    requireNonNull(file, "Component should not be null");
    checkArgument(file.getType() == FILE, "Component '%s' is not a file", file);

    Optional<CloseableIterator<String>> linesIteratorOptional = reportReader.readFileSource(file.getReportAttributes().getRef());

    checkState(linesIteratorOptional.isPresent(), "File '%s' has no source code", file);
    CloseableIterator<String> lineIterator = linesIteratorOptional.get();

    return new ComponentLinesCloseableIterator(file, lineIterator, file.getFileAttributes().getLines());
  }

  private static class ComponentLinesCloseableIterator extends CloseableIterator<String> {
    private static final String EXTRA_END_LINE = "";

    private final Component file;
    private final CloseableIterator<String> delegate;
    private final int numberOfLines;
    private int currentLine = 0;

    private ComponentLinesCloseableIterator(Component file, CloseableIterator<String> lineIterator, int numberOfLines) {
      this.file = file;
      this.delegate = lineIterator;
      this.numberOfLines = numberOfLines;
    }

    @Override
    public boolean hasNext() {
      if (delegate.hasNext()) {
        checkState(currentLine < numberOfLines, "Source of file '%s' has at least one more line than the expected number (%s)", file, numberOfLines);
        return true;
      }
      checkState((currentLine + 1) >= numberOfLines, "Source of file '%s' has less lines (%s) than the expected number (%s)", file, currentLine, numberOfLines);
      return currentLine < numberOfLines;
    }

    @Override
    public String next() {
      if (!hasNext()) {
        // will throw NoSuchElementException
        return delegate.next();
      }

      currentLine++;
      if (delegate.hasNext()) {
        return delegate.next();
      }
      return EXTRA_END_LINE;
    }

    @Override
    protected String doNext() {
      throw new UnsupportedOperationException("Not implemented because hasNext() and next() are overriden");
    }

    @Override
    protected void doClose() {
      delegate.close();
    }
  }
}
