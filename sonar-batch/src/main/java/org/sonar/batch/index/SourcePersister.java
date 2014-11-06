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
package org.sonar.batch.index;

import com.google.common.base.CharMatcher;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.CsvWriter;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.batch.highlighting.SyntaxHighlightingRule;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceMapper;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.core.source.db.SnapshotSourceDto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

public class SourcePersister implements ScanPersister {

  private static final String BOM = "\uFEFF";
  private final ResourcePersister resourcePersister;
  private final SnapshotSourceDao sourceDao;
  private final InputPathCache inputPathCache;
  private final MyBatis mybatis;
  private final MeasureCache measureCache;
  private final ComponentDataCache componentDataCache;
  private final System2 system2;
  private final ProjectTree projectTree;
  private final ResourceCache resourceCache;

  public SourcePersister(ResourcePersister resourcePersister, SnapshotSourceDao sourceDao, InputPathCache inputPathCache,
    MyBatis mybatis, MeasureCache measureCache, ComponentDataCache componentDataCache, ProjectTree projectTree, System2 system2, ResourceCache resourceCache) {
    this.resourcePersister = resourcePersister;
    this.sourceDao = sourceDao;
    this.inputPathCache = inputPathCache;
    this.mybatis = mybatis;
    this.measureCache = measureCache;
    this.componentDataCache = componentDataCache;
    this.projectTree = projectTree;
    this.system2 = system2;
    this.resourceCache = resourceCache;
  }

  public void saveSource(Resource resource, String source, Date updatedAt) {
    Snapshot snapshot = resourcePersister.getSnapshotOrFail(resource);
    SnapshotSourceDto dto = new SnapshotSourceDto();
    dto.setSnapshotId(snapshot.getId().longValue());
    dto.setData(source);
    dto.setUpdatedAt(updatedAt);
    sourceDao.insert(dto);
  }

  @CheckForNull
  public String getSource(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot != null && snapshot.getId() != null) {
      return sourceDao.selectSnapshotSource(snapshot.getId());
    }
    return null;
  }

  @Override
  public void persist() {
    DbSession session = mybatis.openSession(false);
    try {
      FileSourceMapper mapper = session.getMapper(FileSourceMapper.class);

      for (InputPath inputPath : inputPathCache.all()) {
        if (inputPath instanceof InputFile) {
          DefaultInputFile inputFile = (DefaultInputFile) inputPath;
          org.sonar.api.resources.File file = (org.sonar.api.resources.File) resourceCache.get(inputFile.key());
          String fileUuid = file.getUuid();
          FileSourceDto previous = mapper.select(fileUuid);
          String newData = getSourceData(inputFile);
          String dataHash = DigestUtils.md5Hex(newData);
          Date now = system2.newDate();
          if (previous == null) {
            FileSourceDto newFileSource = new FileSourceDto().setProjectUuid(projectTree.getRootProject().getUuid()).setFileUuid(fileUuid).setData(newData).setDataHash(dataHash)
              .setCreatedAt(now)
              .setUpdatedAt(now);
            mapper.insert(newFileSource);
          } else {
            if (dataHash.equals(previous.getDataHash())) {
              continue;
            } else {
              previous.setData(newData).setDataHash(dataHash).setUpdatedAt(now);
              mapper.update(previous);
            }
          }
        }
        session.commit();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save file sources", e);
    } finally {
      MyBatis.closeQuietly(session);
    }

  }

  String getSourceData(DefaultInputFile file) {
    if (file.lines() == 0) {
      return "";
    }
    List<String> lines;
    try {
      lines = FileUtils.readLines(file.file(), file.encoding());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
    // Missing empty last line
    if (lines.size() == file.lines() - 1) {
      lines.add("");
    }
    Map<Integer, String> authorsByLine = getLineMetric(file, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
    Map<Integer, String> revisionsByLine = getLineMetric(file, CoreMetrics.SCM_REVISIONS_BY_LINE_KEY);
    Map<Integer, String> datesByLine = getLineMetric(file, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
    SyntaxHighlightingData highlighting = componentDataCache.getData(file.key(), SnapshotDataTypes.SYNTAX_HIGHLIGHTING);
    String[] highlightingPerLine = computeHighlightingPerLine(file, highlighting);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    CsvWriter csv = CsvWriter.of(new OutputStreamWriter(output, UTF_8));
    for (int lineIdx = 1; lineIdx <= file.lines(); lineIdx++) {
      csv.values(revisionsByLine.get(lineIdx), authorsByLine.get(lineIdx), datesByLine.get(lineIdx), highlightingPerLine[lineIdx - 1],
        CharMatcher.anyOf(BOM).removeFrom(lines.get(lineIdx - 1)));
    }
    csv.close();
    return new String(output.toByteArray(), UTF_8);
  }

  String[] computeHighlightingPerLine(DefaultInputFile file, @Nullable SyntaxHighlightingData highlighting) {
    String[] highlightingPerLine = new String[file.lines()];
    if (highlighting == null) {
      return highlightingPerLine;
    }
    Iterable<SyntaxHighlightingRule> rules = highlighting.syntaxHighlightingRuleSet();
    int currentLineIdx = 1;
    StringBuilder currentLineSb = new StringBuilder();
    for (SyntaxHighlightingRule rule : rules) {
      long ruleStartOffset = rule.getStartPosition();
      long ruleEndOffset = rule.getEndPosition();
      while (currentLineIdx < file.lines() && ruleStartOffset >= file.originalLineOffsets()[currentLineIdx]) {
        // This rule starts on another line so advance
        saveLineHighlighting(highlightingPerLine, currentLineIdx, currentLineSb);
        currentLineIdx++;
      }
      // Now we know current rule starts on current line
      long ruleStartOffsetCurrentLine = ruleStartOffset;
      while (currentLineIdx < file.lines() && ruleEndOffset >= file.originalLineOffsets()[currentLineIdx]) {
        // rule continue on next line so write current line and continue on next line with same rule
        writeRule(currentLineSb, ruleStartOffsetCurrentLine - file.originalLineOffsets()[currentLineIdx - 1], file.originalLineOffsets()[currentLineIdx] - 1, rule.getTextType());
        saveLineHighlighting(highlightingPerLine, currentLineIdx, currentLineSb);
        currentLineIdx++;
        ruleStartOffsetCurrentLine = file.originalLineOffsets()[currentLineIdx];
      }
      // Rule ends on current line
      writeRule(currentLineSb, ruleStartOffsetCurrentLine - file.originalLineOffsets()[currentLineIdx - 1], ruleEndOffset - file.originalLineOffsets()[currentLineIdx - 1],
        rule.getTextType());
    }
    saveLineHighlighting(highlightingPerLine, currentLineIdx, currentLineSb);
    return highlightingPerLine;
  }

  private void writeRule(StringBuilder currentLineSb, long startLineOffset, long endLineOffset, TypeOfText textType) {
    if (currentLineSb.length() > 0) {
      currentLineSb.append(SyntaxHighlightingData.RULE_SEPARATOR);
    }
    currentLineSb.append(startLineOffset)
      .append(SyntaxHighlightingData.FIELD_SEPARATOR)
      .append(endLineOffset)
      .append(SyntaxHighlightingData.FIELD_SEPARATOR)
      .append(textType.cssClass());
  }

  private void saveLineHighlighting(String[] highlightingPerLine, int currentLineIdx, StringBuilder currentLineSb) {
    highlightingPerLine[currentLineIdx - 1] = currentLineSb.toString();
    currentLineSb.setLength(0);
  }

  private Map<Integer, String> getLineMetric(DefaultInputFile file, String metricKey) {
    Map<Integer, String> authorsByLine;
    Iterator<Measure> authorsIt = measureCache.byMetric(file.key(), metricKey).iterator();
    if (authorsIt.hasNext()) {
      authorsByLine = KeyValueFormat.parseIntString((String) authorsIt.next().value());
    } else {
      authorsByLine = Collections.emptyMap();
    }
    return authorsByLine;
  }
}
