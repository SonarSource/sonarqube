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

package org.sonar.server.computation.step;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceDto.Type;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersistTestsStep implements ComputationStep {

  private static final Logger LOG = Loggers.get(PersistTestsStep.class);

  private final DbClient dbClient;
  private final System2 system;

  public PersistTestsStep(DbClient dbClient, System2 system) {
    this.dbClient = dbClient;
    this.system = system;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext computationContext) {
    DbSession session = dbClient.openSession(true);
    try {
      int rootComponentRef = computationContext.getReportMetadata().getRootComponentRef();
      TestContext context = new TestContext(computationContext, session);

      recursivelyProcessComponent(context, rootComponentRef);
      session.commit();
      if (context.hasUnprocessedCoverageDetails) {
        LOG.warn("Some coverage tests are not taken into account during analysis of project '{}'", computationContext.getProject().getKey());
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(TestContext context, int componentRef) {
    BatchReportReader reportReader = context.reader;
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getIsTest()) {
      persistTestResults(component, context);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void persistTestResults(BatchReport.Component component, TestContext context) {
    Multimap<String, FileSourceDb.Test.Builder> testsByName = buildDbTests(context, component);
    Table<String, String, FileSourceDb.Test.CoveredFile.Builder> coveredFilesByName = loadCoverageDetails(component.getRef(), context);
    List<FileSourceDb.Test> tests = addCoveredFilesToTests(testsByName, coveredFilesByName);
    if (checkIfThereAreUnprocessedCoverageDetails(testsByName, coveredFilesByName, component)) {
      context.hasUnprocessedCoverageDetails = true;
    }

    if (tests.isEmpty()) {
      return;
    }

    FileSourceDto existingDto = context.existingFileSourcesByUuid.get(component.getUuid());
    long now = system.now();
    if (existingDto != null) {
      // update
      existingDto
        .setTestData(tests)
        .setUpdatedAt(now);
      dbClient.fileSourceDao().update(context.session, existingDto);
    } else {
      // insert
      FileSourceDto newDto = new FileSourceDto()
        .setTestData(tests)
        .setFileUuid(component.getUuid())
        .setProjectUuid(context.context.getProject().uuid())
        .setDataType(Type.TEST)
        .setCreatedAt(now)
        .setUpdatedAt(now);
      dbClient.fileSourceDao().insert(context.session, newDto);
    }
  }

  private boolean checkIfThereAreUnprocessedCoverageDetails(Multimap<String, FileSourceDb.Test.Builder> testsByName,
    Table<String, String, FileSourceDb.Test.CoveredFile.Builder> coveredFilesByName,
    BatchReport.Component component) {
    Set<String> unprocessedCoverageDetailNames = new HashSet<>(coveredFilesByName.rowKeySet());
    unprocessedCoverageDetailNames.removeAll(testsByName.keySet());
    boolean hasUnprocessedCoverageDetails = !unprocessedCoverageDetailNames.isEmpty();
    if (hasUnprocessedCoverageDetails) {
      LOG.trace("The following test coverages for file '{}' have not been taken into account: {}", component.getPath(), Joiner.on(", ").join(unprocessedCoverageDetailNames));
    }
    return hasUnprocessedCoverageDetails;
  }

  private List<FileSourceDb.Test> addCoveredFilesToTests(Multimap<String, FileSourceDb.Test.Builder> testsByName,
    Table<String, String, FileSourceDb.Test.CoveredFile.Builder> coveredFilesByName) {
    List<FileSourceDb.Test> tests = new ArrayList<>();
    for (FileSourceDb.Test.Builder test : testsByName.values()) {
      Collection<FileSourceDb.Test.CoveredFile.Builder> coveredFiles = coveredFilesByName.row(test.getName()).values();
      if (!coveredFiles.isEmpty()) {
        for (FileSourceDb.Test.CoveredFile.Builder coveredFile : coveredFiles) {
          test.addCoveredFile(coveredFile);
        }
      }
      tests.add(test.build());
    }

    return tests;
  }

  private Multimap<String, FileSourceDb.Test.Builder> buildDbTests(TestContext context, BatchReport.Component component) {
    Multimap<String, FileSourceDb.Test.Builder> tests = ArrayListMultimap.create();
    File testsFile = context.reader.readTests(component.getRef());
    if (testsFile == null) {
      return tests;
    }
    ReportIterator<BatchReport.Test> testIterator = new ReportIterator<>(testsFile, BatchReport.Test.PARSER);
    try {
      while (testIterator.hasNext()) {
        BatchReport.Test batchTest = testIterator.next();
        FileSourceDb.Test.Builder dbTest = FileSourceDb.Test.newBuilder();
        dbTest.setUuid(Uuids.create());
        dbTest.setName(batchTest.getName());
        if (batchTest.hasStacktrace()) {
          dbTest.setStacktrace(batchTest.getStacktrace());
        }
        if (batchTest.hasStatus()) {
          dbTest.setStatus(batchTest.getStatus());
        }
        if (batchTest.hasMsg()) {
          dbTest.setMsg(batchTest.getMsg());
        }
        if (batchTest.hasDurationInMs()) {
          dbTest.setExecutionTimeMs(batchTest.getDurationInMs());
        }

        tests.put(dbTest.getName(), dbTest);
      }
    } finally {
      testIterator.close();
    }

    return tests;
  }

  /**
   * returns a Table of (test name, main file uuid, covered file)
   */
  private Table<String, String, FileSourceDb.Test.CoveredFile.Builder> loadCoverageDetails(int testFileRef, TestContext context) {
    Table<String, String, FileSourceDb.Test.CoveredFile.Builder> nameToCoveredFiles = HashBasedTable.create();
    File coverageDetailsFile = context.reader.readCoverageDetails(testFileRef);
    if (coverageDetailsFile == null) {
      return nameToCoveredFiles;
    }

    ReportIterator<BatchReport.CoverageDetail> coverageIterator = new ReportIterator<>(coverageDetailsFile, BatchReport.CoverageDetail.PARSER);
    try {
      while (coverageIterator.hasNext()) {
        BatchReport.CoverageDetail batchCoverageDetail = coverageIterator.next();
        for (BatchReport.CoverageDetail.CoveredFile batchCoveredFile : batchCoverageDetail.getCoveredFileList()) {
          String testName = batchCoverageDetail.getTestName();
          String mainFileUuid = context.getUuid(batchCoveredFile.getFileRef());
          FileSourceDb.Test.CoveredFile.Builder existingDbCoveredFile = nameToCoveredFiles.get(testName, mainFileUuid);
          List<Integer> batchCoveredLines = batchCoveredFile.getCoveredLineList();
          if (existingDbCoveredFile == null) {
            FileSourceDb.Test.CoveredFile.Builder dbCoveredFile = FileSourceDb.Test.CoveredFile.newBuilder()
              .setFileUuid(context.getUuid(batchCoveredFile.getFileRef()))
              .addAllCoveredLine(batchCoveredLines);
            nameToCoveredFiles.put(testName, mainFileUuid, dbCoveredFile);
          } else {
            List<Integer> remainingBatchCoveredLines = new ArrayList<>(batchCoveredLines);
            remainingBatchCoveredLines.removeAll(existingDbCoveredFile.getCoveredLineList());
            existingDbCoveredFile.addAllCoveredLine(batchCoveredLines);
          }
        }
      }
    } finally {
      coverageIterator.close();
    }
    return nameToCoveredFiles;
  }

  @Override
  public String getDescription() {
    return "Persist tests";
  }

  private static class TestContext {
    final DbSession session;
    final ComputationContext context;
    final BatchReportReader reader;
    final ComponentUuidsCache componentRefToUuidCache;
    final Map<String, FileSourceDto> existingFileSourcesByUuid;
    boolean hasUnprocessedCoverageDetails = false;

    TestContext(ComputationContext context, DbSession session) {
      this.session = session;
      this.context = context;
      this.reader = context.getReportReader();
      this.componentRefToUuidCache = new ComponentUuidsCache(context.getReportReader());
      this.existingFileSourcesByUuid = new HashMap<>();
      session.select("org.sonar.core.source.db.FileSourceMapper.selectHashesForProject",
        ImmutableMap.of("projectUuid", context.getProject().uuid(), "dataType", Type.TEST),
        new ResultHandler() {
          @Override
          public void handleResult(ResultContext context) {
            FileSourceDto dto = (FileSourceDto) context.getResultObject();
            existingFileSourcesByUuid.put(dto.getFileUuid(), dto);
          }
        });
    }

    public String getUuid(int fileRef) {
      return componentRefToUuidCache.getUuidFromRef(fileRef);
    }
  }
}
