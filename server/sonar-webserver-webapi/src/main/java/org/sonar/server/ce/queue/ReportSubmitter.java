/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.ce.queue;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.TempFolder;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.sonar.core.config.CorePropertyDefinitions.REPORT_PART_MAX_SIZE_MBYTES;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.project.CreationMethod.SCANNER_API;
import static org.sonar.db.project.CreationMethod.SCANNER_API_DEVOPS_AUTO_CONFIG;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportSubmitter.class);

  private final CeQueue queue;
  private final UserSession userSession;

  private final ProjectCreator projectCreator;
  private final ComponentUpdater componentUpdater;
  private final PermissionTemplateService permissionTemplateService;
  private final DbClient dbClient;
  private final BranchSupport branchSupport;
  private final DevOpsProjectCreatorFactory devOpsProjectCreatorFactory;
  private final ManagedInstanceService managedInstanceService;
  private final TempFolder tempFolder;
  private final Configuration configuration;

  public ReportSubmitter(CeQueue queue, UserSession userSession, ProjectCreator projectCreator, ComponentUpdater componentUpdater,
    PermissionTemplateService permissionTemplateService, DbClient dbClient, BranchSupport branchSupport,
    DevOpsProjectCreatorFactory devOpsProjectCreatorFactory, ManagedInstanceService managedInstanceService, TempFolder tempFolder, Configuration configuration) {
    this.queue = queue;
    this.userSession = userSession;
    this.projectCreator = projectCreator;
    this.componentUpdater = componentUpdater;
    this.permissionTemplateService = permissionTemplateService;
    this.dbClient = dbClient;
    this.branchSupport = branchSupport;
    this.devOpsProjectCreatorFactory = devOpsProjectCreatorFactory;
    this.managedInstanceService = managedInstanceService;
    this.tempFolder = tempFolder;
    this.configuration = configuration;
  }

  public CeTask submit(String projectKey, @Nullable String projectName, Map<String, String> characteristics, InputStream reportInput) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentCreationData componentCreationData = null;
      // Note: when the main branch is analyzed, the characteristics may or may not have the branch name, so componentKey#isMainBranch is not
      // reliable!
      BranchSupport.ComponentKey componentKey = branchSupport.createComponentKey(projectKey, characteristics);
      Optional<ComponentDto> mainBranchComponentOpt = dbClient.componentDao().selectByKey(dbSession, componentKey.getKey());
      ComponentDto mainBranchComponent;

      if (mainBranchComponentOpt.isPresent()) {
        mainBranchComponent = mainBranchComponentOpt.get();
        validateProject(dbSession, mainBranchComponent, projectKey);
      } else {
        componentCreationData = createProject(projectKey, projectName, characteristics, dbSession, componentKey);
        mainBranchComponent = componentCreationData.mainBranchComponent();
      }

      BranchDto mainBranch = dbClient.branchDao().selectByUuid(dbSession, mainBranchComponent.branchUuid())
        .orElseThrow(() -> new IllegalStateException("Couldn't find the main branch of the project"));
      ComponentDto branchComponent;
      if (isMainBranch(componentKey, mainBranch)) {
        branchComponent = mainBranchComponent;
      } else if (componentKey.getBranchName().isPresent()) {
        branchComponent = dbClient.componentDao().selectByKeyAndBranch(dbSession, componentKey.getKey(), componentKey.getBranchName().get())
          .orElseGet(() -> branchSupport.createBranchComponent(dbSession, componentKey, mainBranchComponent, mainBranch));
      } else {
        branchComponent = dbClient.componentDao().selectByKeyAndPullRequest(dbSession, componentKey.getKey(), componentKey.getPullRequestKey().get())
          .orElseGet(() -> branchSupport.createBranchComponent(dbSession, componentKey, mainBranchComponent, mainBranch));
      }

      if (componentCreationData != null) {
        componentUpdater.commitAndIndex(dbSession, componentCreationData);
      } else {
        dbSession.commit();
      }

      checkScanPermission(branchComponent);
      return submitReport(dbSession, reportInput, branchComponent, mainBranch, characteristics);
    }
  }

  private static boolean isMainBranch(BranchSupport.ComponentKey componentKey, BranchDto mainBranch) {
    if (componentKey.isMainBranch()) {
      return true;
    }

    return componentKey.getBranchName().isPresent() && componentKey.getBranchName().get().equals(mainBranch.getKey());
  }

  private void checkScanPermission(ComponentDto project) {
    // this is a specific and inconsistent behavior. For legacy reasons, "technical users"
    // defined with global scan permission should be able to analyze a project even if
    // they don't have the direct permission on the project.
    // That means that dropping the permission on the project does not have any effects
    // if user has still the global permission
    if (!userSession.hasComponentPermission(ProjectPermission.SCAN, project) && !userSession.hasPermission(GlobalPermission.SCAN)) {
      throw insufficientPrivilegesException();
    }
  }

  private void validateProject(DbSession dbSession, ComponentDto component, String rawProjectKey) {
    List<String> errors = new ArrayList<>();

    if (!ComponentQualifiers.PROJECT.equals(component.qualifier()) || !ComponentScopes.PROJECT.equals(component.scope())) {
      errors.add(format("Component '%s' is not a project", rawProjectKey));
    }
    if (!component.branchUuid().equals(component.uuid())) {
      // Project key is already used as a module of another project
      ComponentDto anotherBaseProject = dbClient.componentDao().selectOrFailByUuid(dbSession, component.branchUuid());
      errors.add(format("The project '%s' is already defined in SonarQube but as a module of project '%s'. "
          + "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
        rawProjectKey, anotherBaseProject.getKey(), anotherBaseProject.getKey(), rawProjectKey));
    }
    if (!errors.isEmpty()) {
      throw BadRequestException.create(errors);
    }
  }

  private ComponentCreationData createProject(String projectKey, @Nullable String projectName, Map<String, String> characteristics,
    DbSession dbSession, BranchSupport.ComponentKey componentKey) {
    userSession.checkPermission(GlobalPermission.PROVISION_PROJECTS);

    DevOpsProjectCreator devOpsProjectCreator = devOpsProjectCreatorFactory.getDevOpsProjectCreator(dbSession, characteristics).orElse(null);

    throwIfCurrentUserWouldNotHaveScanPermission(projectKey, dbSession, devOpsProjectCreator);

    if (devOpsProjectCreator != null) {
      return devOpsProjectCreator.createProjectAndBindToDevOpsPlatform(dbSession, SCANNER_API_DEVOPS_AUTO_CONFIG, false, projectKey, projectName, false);
    }
    return projectCreator.createProject(dbSession, componentKey.getKey(), defaultIfBlank(projectName, projectKey), null, SCANNER_API);
  }

  private void throwIfCurrentUserWouldNotHaveScanPermission(String projectKey, DbSession dbSession, @Nullable DevOpsProjectCreator devOpsProjectCreator) {
    if (!wouldCurrentUserHaveScanPermission(projectKey, dbSession, devOpsProjectCreator)) {
      throw insufficientPrivilegesException();
    }
  }

  private boolean wouldCurrentUserHaveScanPermission(String projectKey, DbSession dbSession, @Nullable DevOpsProjectCreator devOpsProjectCreator) {
    if (userSession.hasPermission(SCAN)) {
      return true;
    }
    if (managedInstanceService.isInstanceExternallyManaged() && devOpsProjectCreator != null) {
      return devOpsProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform();
    }
    return permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(dbSession, userSession.getUuid(), projectKey);
  }

  private CeTask submitReport(DbSession dbSession, InputStream reportInput, ComponentDto branch, BranchDto mainBranch, Map<String, String> characteristics) {
    CeTaskSubmit.Builder submit = queue.prepareSubmit();

    int nbParts = saveReport(dbSession, reportInput, submit);

    dbSession.commit();
    submit
      .setType(CeTaskTypes.REPORT)
      .setComponent(CeTaskSubmit.Component.fromDto(branch.uuid(), mainBranch.getProjectUuid()))
      .setSubmitterUuid(userSession.getUuid())
      .setCharacteristics(characteristics)
      .setReportPartCount(nbParts);
    return queue.submit(submit.build());
  }

  private int saveReport(DbSession dbSession, InputStream reportInput, CeTaskSubmit.Builder submit) {
    File tempFile = tempFolder.newFile();
    try {
      writeReportToTempFile(reportInput, tempFile);

      long sourceSize = getSourceSize(tempFile);
      long reportPartMaxSizeBytes = getReportMaxSizeBytes();

      if (sourceSize <= reportPartMaxSizeBytes) {
        saveFileOnDb(dbSession, submit, 0, tempFile);
        return 1;
      }

      return splitFile(tempFile, sourceSize, reportPartMaxSizeBytes, dbSession, submit);
    } finally {
      deleteTempFile(tempFile);
    }
  }

  @VisibleForTesting
  long getReportMaxSizeBytes() {
    return configuration.getInt(REPORT_PART_MAX_SIZE_MBYTES)
      .map(mBytes -> mBytes * 1024 * 1024)
      .orElse(450 * 1024 * 1024);
  }

  private static void writeReportToTempFile(InputStream reportInput, File tempFile) {
    try {
      Files.copy(reportInput, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save scanner report to temp file", e);
    }
  }

  private static long getSourceSize(File tempFile) {
    try {
      return Files.size(tempFile.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("Could not check the size of the report temp file", e);
    }
  }

  public int splitFile(File bigFile, long sourceSize, long reportPartMaxSizeBytes, DbSession dbSession, CeTaskSubmit.Builder submit) {
    int position = 0;
    LOGGER.debug("Splitting report file {} of size {}", bigFile.toPath(), sourceSize);
    try (RandomAccessFile sourceFile = new RandomAccessFile(bigFile, "r");
         FileChannel sourceChannel = sourceFile.getChannel()) {

      final long numSplits = sourceSize / reportPartMaxSizeBytes;
      final long remainingBytes = sourceSize % reportPartMaxSizeBytes;
      LOGGER.debug("Report file {} will be split in {} parts", bigFile.toPath(), numSplits + (remainingBytes > 0 ? 1 : 0));

      for (; position < numSplits; position++) {
        //write multipart files.
        writePartToDb(dbSession, submit, reportPartMaxSizeBytes, position, sourceChannel, reportPartMaxSizeBytes);
      }
      if (remainingBytes > 0) {
        writePartToDb(dbSession, submit, remainingBytes, position, sourceChannel, reportPartMaxSizeBytes);
        return position + 1;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to split report for task " + submit.getUuid(), e);
    }
    return position;
  }

  private void writePartToDb(DbSession dbSession, CeTaskSubmit.Builder submit, long byteSize, long partIndex, FileChannel sourceChannel, long reportPartMaxSizeBytes)
    throws IOException {
    File file = tempFolder.newFile();
    try {
      writePart(byteSize, partIndex, sourceChannel, reportPartMaxSizeBytes, file);

      saveFileOnDb(dbSession, submit, partIndex, file);
    } finally {
      deleteTempFile(file);
    }
  }

  private static void writePart(long byteSize, long partIndex, FileChannel sourceChannel, long reportPartMaxSizeBytes, File file) throws IOException {
    try (RandomAccessFile toFile = new RandomAccessFile(file, "rw");
         FileChannel toChannel = toFile.getChannel()
    ) {
      sourceChannel.position(partIndex * reportPartMaxSizeBytes);
      toChannel.transferFrom(sourceChannel, 0, byteSize);
    }
  }

  private static void deleteTempFile(File file) {
    try {
      Files.delete(file.toPath());
    } catch (IOException e) {
      LOGGER.warn("Failed to delete temp file {}", file.getAbsolutePath(), e);
    }
  }

  private void saveFileOnDb(DbSession dbSession, CeTaskSubmit.Builder submit, long partIndex, File file) {
    long partNumber = partIndex + 1;
    LOGGER.debug("Saving report {} part {} on DB from file {}", submit.getUuid(), partNumber, file.toPath());
    try (InputStream is = Files.newInputStream(file.toPath())) {
      dbClient.ceTaskInputDao().insert(dbSession, submit.getUuid(), partNumber, is);
    } catch (IOException e) {
      throw new IllegalStateException("Could not save report " + submit.getUuid() + " part " + partNumber + " file to database", e);
    }
  }

}
