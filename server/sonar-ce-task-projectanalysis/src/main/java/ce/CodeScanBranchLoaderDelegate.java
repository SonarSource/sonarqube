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
package ce;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchLoaderDelegate;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata;
import org.sonar.server.project.Project;

/**
 * CE calls this as a delegate in the BranchLoader. Set a sane branch name..
 */
public class CodeScanBranchLoaderDelegate implements BranchLoaderDelegate {

    private final DbClient dbClient;
    private final MutableAnalysisMetadataHolder metadataHolder;

    public CodeScanBranchLoaderDelegate(DbClient dbClient, MutableAnalysisMetadataHolder metadataHolder) {
        this.dbClient = dbClient;
        this.metadataHolder = metadataHolder;
    }

    @Override
    public void load(@Nonnull Metadata metadata) {
        Branch branch = load(metadata, metadataHolder.getProject());

        metadataHolder.setBranch(branch);
        metadataHolder.setPullRequestKey(metadata.getPullRequestKey());
    }

    private Branch load(final Metadata metadata, Project project) {
        String targetBranchName = StringUtils.trimToNull(metadata.getTargetBranchName());
        String branchName = StringUtils.trimToNull(metadata.getBranchName());
        String projectUuid = StringUtils.trimToNull(project.getUuid());

        // Get the branch name or default to master.
        if (branchName == null) {
            Optional<BranchDto> branchDto = findBranchByProjectUuid(projectUuid);
            if (branchDto.isPresent()) {
                BranchDto dto = branchDto.get();
                return new CodeScanBranch(dto.getKey(), dto.getBranchType(), dto.isMain(), null, null, targetBranchName);
            } else {
                throw new IllegalStateException("Could not find main branch");
            }
        } else {
            String targetBranch = StringUtils.trimToNull(metadata.getReferenceBranchName());
            Metadata.BranchType branchType = metadata.getBranchType();

            if (Metadata.BranchType.PULL_REQUEST == branchType) {
                if (targetBranchName == null) {
                    targetBranchName = targetBranch;
                }
                return createPullRequest(metadata, branchName, projectUuid, targetBranch, targetBranchName);
            } else if (Metadata.BranchType.BRANCH == branchType) {
                return createBranch(metadata, branchName, projectUuid, targetBranch, targetBranchName);
            } else {
                throw new IllegalStateException(String.format("Invalid branch type '%s'", branchType.name()));
            }
        }
    }

    private Branch createPullRequest(Metadata metadata, String branchName,
            String projectUuid, String targetBranch, String targetBranchName) {
        Optional<BranchDto> branchDto = findBranchByKey(projectUuid, targetBranch);
        if (branchDto.isPresent()) {
            String pullRequestKey = metadata.getPullRequestKey();

            BranchDto dto = branchDto.get();
            return new CodeScanBranch(branchName, BranchType.PULL_REQUEST, false, dto.getUuid(), pullRequestKey,
                    targetBranchName);
        } else {
            throw new IllegalStateException(
                    String.format("Could not find target branch '%s' in project", targetBranch));
        }
    }

    private Branch createBranch(Metadata metadata, String branchName, String projectUuid,
            String referenceBranch, String targetBranchName) {
        // Find an existing target branch.
        boolean isMain = false;
        BranchType branchType = this.convertBranchType(metadata.getBranchType());
        final Optional<BranchDto> branchDto = this.findBranchByKey(branchName);
        if (branchDto.isPresent()) {
            //check that the branch type hasn't changed....
            if (branchDto.get().getBranchType() != branchType) {
                throw new IllegalStateException(
                        String.format("Branch '%s' already exists with type '%s', type '%s' not possible.", branchName,
                                branchDto.get().getBranchType(), branchType));
            }
            //set the isMain based on existing...
            isMain = branchDto.get().isMain();
        }

        //find the target branch...
        String referenceBranchUuid = null;
        if (referenceBranch != null) {
            referenceBranchUuid = findReferenceBranchUuid(projectUuid, referenceBranch);
        } else if (branchType == BranchType.PULL_REQUEST) {
            throw new IllegalArgumentException(
                    String.format("Branch target for '%s' not set, so branch type should be BRANCH but is '%s'",
                            branchName, branchType));
        }

        return new CodeScanBranch(branchName, branchType, isMain, referenceBranchUuid, null, targetBranchName);
    }

    private BranchType convertBranchType(final Metadata.BranchType branchType) {
        switch (branchType.ordinal()) {
            case Metadata.BranchType.BRANCH_VALUE:
            case Metadata.BranchType.UNSET_VALUE:
                return BranchType.BRANCH;
            case Metadata.BranchType.PULL_REQUEST_VALUE:
                return BranchType.PULL_REQUEST;
            default:
                throw new IllegalStateException("Invalid branch type: " + branchType);
        }
    }

    private String findReferenceBranchUuid(String projectUuid, String branchName) {
        final Optional<BranchDto> branch = this.findBranchByKey(projectUuid, branchName);
        if (branch.isPresent()) {
            final BranchDto branchDto = branch.get();
            if (branchDto.getBranchType() == BranchType.BRANCH) {
                return branchDto.getUuid();
            }

            throw new IllegalArgumentException("Target branch '" + branchName + "' should be BRANCH but is '" + branchDto.getBranchType() + "'");
        } else {
            throw new IllegalArgumentException("Target Branch '" + branchName + "' does not exist");
        }
    }

    private Optional<BranchDto> findBranchByKey(final String branchName) {
        final Project project = this.metadataHolder.getProject();
        return this.findBranchByKey(project.getUuid(), branchName);
    }


    private Optional<BranchDto> findBranchByKey(final String projectUuid, final String key) {
        try (DbSession openSession = this.dbClient.openSession(false)) {
            return this.dbClient.branchDao().selectByBranchKey(openSession, projectUuid, key);
        }
    }

    private Optional<BranchDto> findBranchByProjectUuid(final String projectUuid) {
        try (DbSession openSession = this.dbClient.openSession(false)) {
            return this.dbClient.branchDao().selectMainBranchByProjectUuid(openSession, projectUuid);
        }
    }
}
