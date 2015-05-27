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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceDb.Test.TestStatus;

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
  private final DbComponentsRefCache dbComponentsRefCache;

  public PersistTestsStep(DbClient dbClient, System2 system, DbComponentsRefCache dbComponentsRefCache) {
    this.dbClient = dbClient;
    this.system = system;
    this.dbComponentsRefCache = dbComponentsRefCache;
  }

  @Override
  public void execute(final ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      TestDepthTraversalTypeAwareVisitor visitor = new TestDepthTraversalTypeAwareVisitor(context, session, dbComponentsRefCache);
      visitor.visit(context.getRoot());
      session.commit();
      if (visitor.hasUnprocessedCoverageDetails) {
        String projectKey = dbComponentsRefCache.getByRef(context.getReportMetadata().getRootComponentRef()).getKey();
        LOG.warn("Some coverage tests are not taken into account during analysis of project '{}'", projectKey);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Persist tests";
  }

  private class TestDepthTraversalTypeAwareVisitor extends DepthTraversalTypeAwareVisitor {
    final DbSession session;
    final BatchReportReader reader;
    final DbComponentsRefCache dbComponentsRefCache;
    final Map<String, FileSourceDto> existingFileSourcesByUuid;
    final String projectUuid;
    boolean hasUnprocessedCoverageDetails = false;

    public TestDepthTraversalTypeAwareVisitor(ComputationContext context, DbSession session, DbComponentsRefCache dbComponentsRefCache) {
      super(Component.Type.FILE, Order.PRE_ORDER);
      this.session = session;
      this.dbComponentsRefCache = dbComponentsRefCache;
      this.reader = context.getReportReader();
      this.existingFileSourcesByUuid = new HashMap<>();
      this.projectUuid = context.getRoot().getUuid();
      session.select("org.sonar.core.source.db.FileSourceMapper.selectHashesForProject",
        ImmutableMap.of("projectUuid", context.getRoot().getUuid(), "dataType", Type.TEST),
        new ResultHandler() {
          @Override
          public void handleResult(ResultContext context) {
            FileSourceDto dto = (FileSourceDto) context.getResultObject();
            existingFileSourcesByUuid.put(dto.getFileUuid(), dto);
          }
        });
    }

    @Override
    public void visitFile(Component file) {
      BatchReport.Component batchComponent = reader.readComponent(file.getRef());
      if (batchComponent.getIsTest()) {
        persistTestResults(batchComponent);
      }
    }

    private void persistTestResults(BatchReport.Component component) {
      Multimap<String, FileSourceDb.Test.Builder> testsByName = buildDbTests(component);
      Table<String, String, FileSourceDb.Test.CoveredFile.Builder> coveredFilesByName = loadCoverageDetails(component.getRef());
      List<FileSourceDb.Test> tests = addCoveredFilesToTests(testsByName, coveredFilesByName);
      if (checkIfThereAreUnprocessedCoverageDetails(testsByName, coveredFilesByName, component)) {
        hasUnprocessedCoverageDetails = true;
      }

      if (tests.isEmpty()) {
        return;
      }

      String componentUuid = getUuid(component.getRef());
      FileSourceDto existingDto = existingFileSourcesByUuid.get(componentUuid);
      long now = system.now();
      if (existingDto != null) {
        // update
        existingDto
          .setTestData(tests)
          .setUpdatedAt(now);
        dbClient.fileSourceDao().update(session, existingDto);
      } else {
        // insert
        FileSourceDto newDto = new FileSourceDto()
          .setTestData(tests)
          .setFileUuid(componentUuid)
          .setProjectUuid(projectUuid)
          .setDataType(Type.TEST)
          .setCreatedAt(now)
          .setUpdatedAt(now);
        dbClient.fileSourceDao().insert(session, newDto);
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

    private Multimap<String, FileSourceDb.Test.Builder> buildDbTests(BatchReport.Component component) {
      Multimap<String, FileSourceDb.Test.Builder> tests = ArrayListMultimap.create();
      File testsFile = reader.readTests(component.getRef());
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
            dbTest.setStatus(TestStatus.valueOf(batchTest.getStatus().name()));
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
    private Table<String, String, FileSourceDb.Test.CoveredFile.Builder> loadCoverageDetails(int testFileRef) {
      Table<String, String, FileSourceDb.Test.CoveredFile.Builder> nameToCoveredFiles = HashBasedTable.create();
      File coverageDetailsFile = reader.readCoverageDetails(testFileRef);
      if (coverageDetailsFile == null) {
        return nameToCoveredFiles;
      }

      ReportIterator<BatchReport.CoverageDetail> coverageIterator = new ReportIterator<>(coverageDetailsFile, BatchReport.CoverageDetail.PARSER);
      try {
        while (coverageIterator.hasNext()) {
          BatchReport.CoverageDetail batchCoverageDetail = coverageIterator.next();
          for (BatchReport.CoverageDetail.CoveredFile batchCoveredFile : batchCoverageDetail.getCoveredFileList()) {
            String testName = batchCoverageDetail.getTestName();
            String mainFileUuid = getUuid(batchCoveredFile.getFileRef());
            FileSourceDb.Test.CoveredFile.Builder existingDbCoveredFile = nameToCoveredFiles.get(testName, mainFileUuid);
            List<Integer> batchCoveredLines = batchCoveredFile.getCoveredLineList();
            if (existingDbCoveredFile == null) {
              FileSourceDb.Test.CoveredFile.Builder dbCoveredFile = FileSourceDb.Test.CoveredFile.newBuilder()
                .setFileUuid(getUuid(batchCoveredFile.getFileRef()))
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

    private String getUuid(int fileRef) {
      return dbComponentsRefCache.getByRef(fileRef).getUuid();
    }
  }

}
