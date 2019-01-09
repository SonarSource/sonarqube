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

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.hash.LineRange;
import org.sonar.core.hash.SourceLineHashesComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.source.LineHashVersion;

public class SourceLinesHashRepositoryImpl implements SourceLinesHashRepository {
  private final SourceLinesRepository sourceLinesRepository;
  private final SignificantCodeRepository significantCodeRepository;
  private final SourceLinesHashCache cache;
  private final DbLineHashVersion dbLineHashesVersion;

  public SourceLinesHashRepositoryImpl(SourceLinesRepository sourceLinesRepository, SignificantCodeRepository significantCodeRepository,
    SourceLinesHashCache cache, DbLineHashVersion dbLineHashVersion) {
    this.sourceLinesRepository = sourceLinesRepository;
    this.significantCodeRepository = significantCodeRepository;
    this.cache = cache;
    this.dbLineHashesVersion = dbLineHashVersion;
  }

  @Override
  public List<String> getLineHashesMatchingDBVersion(Component component) {
    return cache.computeIfAbsent(component, this::createLineHashesMatchingDBVersion);
  }

  @Override
  public int getLineHashesVersion(Component component) {
    if (significantCodeRepository.getRangesPerLine(component).isPresent()) {
      return LineHashVersion.WITH_SIGNIFICANT_CODE.getDbValue();
    } else {
      return LineHashVersion.WITHOUT_SIGNIFICANT_CODE.getDbValue();
    }
  }

  @Override
  public LineHashesComputer getLineHashesComputerToPersist(Component component) {
    boolean cacheHit = cache.contains(component);

    // check if line hashes are cached and if we can use it
    if (cacheHit && dbLineHashesVersion.hasLineHashesWithSignificantCode(component)) {
      return new CachedLineHashesComputer(cache.get(component));
    }

    Optional<LineRange[]> significantCodePerLine = significantCodeRepository.getRangesPerLine(component);
    if (cacheHit && !significantCodePerLine.isPresent()) {
      return new CachedLineHashesComputer(cache.get(component));
    }

    // Generate the line hashes taking into account significant code ranges
    return createLineHashesProcessor(component.getFileAttributes().getLines(), significantCodePerLine);
  }

  private List<String> createLineHashesMatchingDBVersion(Component component) {
    if (!dbLineHashesVersion.hasLineHashesWithSignificantCode(component)) {
      return createLineHashes(component, Optional.empty());
    }

    Optional<LineRange[]> significantCodePerLine = significantCodeRepository.getRangesPerLine(component);
    return createLineHashes(component, significantCodePerLine);
  }

  private List<String> createLineHashes(Component component, Optional<LineRange[]> significantCodePerLine) {
    LineHashesComputer processor = createLineHashesProcessor(component.getFileAttributes().getLines(), significantCodePerLine);
    try (CloseableIterator<String> lines = sourceLinesRepository.readLines(component)) {
      while (lines.hasNext()) {
        processor.addLine(lines.next());
      }
      return processor.getResult();
    }
  }

  public interface LineHashesComputer {
    void addLine(String line);

    List<String> getResult();
  }

  private static LineHashesComputer createLineHashesProcessor(int numLines, Optional<LineRange[]> significantCodePerLine) {
    if (significantCodePerLine.isPresent()) {
      return new SignificantCodeLineHashesComputer(new SourceLineHashesComputer(numLines), significantCodePerLine.get());
    } else {
      return new SimpleLineHashesComputer(numLines);
    }
  }

  static class CachedLineHashesComputer implements LineHashesComputer {
    private final List<String> lineHashes;

    public CachedLineHashesComputer(List<String> lineHashes) {
      this.lineHashes = lineHashes;
    }

    @Override
    public void addLine(String line) {
      // no op
    }

    @Override
    public List<String> getResult() {
      return lineHashes;
    }
  }

  static class SimpleLineHashesComputer implements LineHashesComputer {
    private final SourceLineHashesComputer delegate;

    public SimpleLineHashesComputer(int numLines) {
      this.delegate = new SourceLineHashesComputer(numLines);
    }

    @Override
    public void addLine(String line) {
      delegate.addLine(line);
    }

    @Override
    public List<String> getResult() {
      return delegate.getLineHashes();
    }
  }

  static class SignificantCodeLineHashesComputer implements LineHashesComputer {
    private final SourceLineHashesComputer delegate;
    private final LineRange[] rangesPerLine;

    private int i = 0;

    public SignificantCodeLineHashesComputer(SourceLineHashesComputer hashComputer, LineRange[] rangesPerLine) {
      this.rangesPerLine = rangesPerLine;
      this.delegate = hashComputer;
    }

    @Override
    public void addLine(String line) {
      LineRange range = null;
      if (i < rangesPerLine.length) {
        range = rangesPerLine[i];
      }

      if (range == null) {
        delegate.addLine("");
      } else {
        delegate.addLine(StringUtils.substring(line, range.startOffset(), range.endOffset()));
      }
      i++;
    }

    @Override
    public List<String> getResult() {
      return delegate.getLineHashes();
    }
  }

}
