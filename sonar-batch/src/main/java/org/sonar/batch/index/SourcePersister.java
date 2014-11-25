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
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
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
import org.sonar.batch.source.CodeColorizers;
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
import java.util.HashMap;
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
  private CodeColorizers codeColorizers;

  public SourcePersister(ResourcePersister resourcePersister, SnapshotSourceDao sourceDao, InputPathCache inputPathCache,
    MyBatis mybatis, MeasureCache measureCache, ComponentDataCache componentDataCache, ProjectTree projectTree, System2 system2, ResourceCache resourceCache,
    CodeColorizers codeColorizers) {
    this.resourcePersister = resourcePersister;
    this.sourceDao = sourceDao;
    this.inputPathCache = inputPathCache;
    this.mybatis = mybatis;
    this.measureCache = measureCache;
    this.componentDataCache = componentDataCache;
    this.projectTree = projectTree;
    this.system2 = system2;
    this.resourceCache = resourceCache;
    this.codeColorizers = codeColorizers;
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
    DbSession session = mybatis.openSession(true);
    try {

      final Map<String, FileSourceDto> fileSourceDtoByFileUuid = new HashMap<String, FileSourceDto>();

      session.select("org.sonar.core.source.db.FileSourceMapper.selectAllFileDataHashByProject", projectTree.getRootProject().getUuid(), new ResultHandler() {

        @Override
        public void handleResult(ResultContext context) {
          FileSourceDto dto = (FileSourceDto) context.getResultObject();
          fileSourceDtoByFileUuid.put(dto.getFileUuid(), dto);
        }
      });

      FileSourceMapper mapper = session.getMapper(FileSourceMapper.class);

      for (InputPath inputPath : inputPathCache.all()) {
        if (inputPath instanceof InputFile) {
          persist(session, mapper, inputPath, fileSourceDtoByFileUuid);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save file sources", e);
    } finally {
      MyBatis.closeQuietly(session);
    }

  }

  private void persist(DbSession session, FileSourceMapper mapper, InputPath inputPath, Map<String, FileSourceDto> fileSourceDtoByFileUuid) {
    DefaultInputFile inputFile = (DefaultInputFile) inputPath;
    org.sonar.api.resources.File file = (org.sonar.api.resources.File) resourceCache.get(inputFile.key());
    String fileUuid = file.getUuid();
    FileSourceDto previous = fileSourceDtoByFileUuid.get(fileUuid);
    String newData = getSourceData(inputFile);
    String newDataHash = newData != null ? DigestUtils.md5Hex(newData) : "0";
    Date now = system2.newDate();
    if (previous == null) {
      FileSourceDto newFileSource = new FileSourceDto()
        .setProjectUuid(projectTree.getRootProject().getUuid())
        .setFileUuid(fileUuid)
        .setData(newData)
        .setDataHash(newDataHash)
        .setLineHashes(lineHashesAsMd5Hex(inputFile))
        .setCreatedAt(now.getTime())
        .setUpdatedAt(now.getTime());
      mapper.insert(newFileSource);
      session.commit();
    } else {
      if (!newDataHash.equals(previous.getDataHash())) {
        previous
          .setData(newData)
          .setLineHashes(lineHashesAsMd5Hex(inputFile))
          .setDataHash(newDataHash)
          .setUpdatedAt(now.getTime());
        mapper.update(previous);
        session.commit();
      }
    }
  }

  @CheckForNull
  private String lineHashesAsMd5Hex(DefaultInputFile inputFile) {
    if (inputFile.lines() == 0) {
      return null;
    }
    // A md5 string is 32 char long + '\n' = 33
    StringBuilder result = new StringBuilder(inputFile.lines() * (32 + 1));
    for (byte[] lineHash : inputFile.lineHashes()) {
      if (result.length() > 0) {
        result.append("\n");
      }
      result.append(lineHash != null ? Hex.encodeHexString(lineHash) : "");
    }
    return result.toString();
  }

  @CheckForNull
  String getSourceData(DefaultInputFile file) {
    if (file.lines() == 0) {
      return null;
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
    Map<Integer, String> hitsByLine = getLineMetric(file, CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
    Map<Integer, String> condByLine = getLineMetric(file, CoreMetrics.CONDITIONS_BY_LINE_KEY);
    Map<Integer, String> coveredCondByLine = getLineMetric(file, CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
    SyntaxHighlightingData highlighting = loadHighlighting(file);
    String[] highlightingPerLine = computeHighlightingPerLine(file, highlighting);

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    CsvWriter csv = CsvWriter.of(new OutputStreamWriter(output, UTF_8));
    for (int lineIdx = 1; lineIdx <= file.lines(); lineIdx++) {
      csv.values(revisionsByLine.get(lineIdx), authorsByLine.get(lineIdx), datesByLine.get(lineIdx),
        hitsByLine.get(lineIdx), condByLine.get(lineIdx), coveredCondByLine.get(lineIdx),
        highlightingPerLine[lineIdx - 1],
        CharMatcher.anyOf(BOM).removeFrom(lines.get(lineIdx - 1)));
    }
    csv.close();
    return StringUtils.defaultIfEmpty(new String(output.toByteArray(), UTF_8), null);
  }

  @CheckForNull
  private SyntaxHighlightingData loadHighlighting(DefaultInputFile file) {
    SyntaxHighlightingData highlighting = componentDataCache.getData(file.key(), SnapshotDataTypes.SYNTAX_HIGHLIGHTING);
    if (highlighting == null) {
      highlighting = codeColorizers.toSyntaxHighlighting(file.file(), file.encoding(), file.language());
    }
    return highlighting;
  }

  String[] computeHighlightingPerLine(DefaultInputFile file, @Nullable SyntaxHighlightingData highlighting) {
    String[] result = new String[file.lines()];
    if (highlighting == null) {
      return result;
    }
    Iterable<SyntaxHighlightingRule> rules = highlighting.syntaxHighlightingRuleSet();
    int currentLineIdx = 1;
    StringBuilder[] highlightingPerLine = new StringBuilder[file.lines()];
    for (SyntaxHighlightingRule rule : rules) {
      while (currentLineIdx < file.lines() && rule.getStartPosition() >= file.originalLineOffsets()[currentLineIdx]) {
        // This rule starts on another line so advance
        currentLineIdx++;
      }
      // Now we know current rule starts on current line
      writeRule(file, rule, highlightingPerLine, currentLineIdx);
    }
    for (int i = 0; i < file.lines(); i++) {
      result[i] = highlightingPerLine[i] != null ? highlightingPerLine[i].toString() : null;
    }
    return result;
  }

  private void writeRule(DefaultInputFile file, SyntaxHighlightingRule rule, StringBuilder[] highlightingPerLine, int currentLine) {
    int currentLineIdx = currentLine;
    // We know current rule starts on current line
    long ruleStartOffsetCurrentLine = rule.getStartPosition();
    while (currentLineIdx < file.lines() && rule.getEndPosition() >= file.originalLineOffsets()[currentLineIdx]) {
      // rule continue on next line so write current line and continue on next line with same rule
      writeRule(highlightingPerLine, currentLineIdx, ruleStartOffsetCurrentLine - file.originalLineOffsets()[currentLineIdx - 1], file.originalLineOffsets()[currentLineIdx]
        - file.originalLineOffsets()[currentLineIdx - 1],
        rule.getTextType());
      currentLineIdx++;
      ruleStartOffsetCurrentLine = file.originalLineOffsets()[currentLineIdx - 1];
    }
    // Rule ends on current line
    writeRule(highlightingPerLine, currentLineIdx, ruleStartOffsetCurrentLine - file.originalLineOffsets()[currentLineIdx - 1], rule.getEndPosition()
      - file.originalLineOffsets()[currentLineIdx - 1],
      rule.getTextType());
  }

  private void writeRule(StringBuilder[] highlightingPerLine, int currentLineIdx, long startLineOffset, long endLineOffset, TypeOfText textType) {
    if (highlightingPerLine[currentLineIdx - 1] == null) {
      highlightingPerLine[currentLineIdx - 1] = new StringBuilder();
    }
    StringBuilder currentLineSb = highlightingPerLine[currentLineIdx - 1];
    writeRule(currentLineSb, startLineOffset, endLineOffset, textType);
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
