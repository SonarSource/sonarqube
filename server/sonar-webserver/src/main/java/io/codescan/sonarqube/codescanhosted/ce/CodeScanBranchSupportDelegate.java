/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

package io.codescan.sonarqube.codescanhosted.ce;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.ce.queue.BranchSupport.ComponentKey;
import org.sonar.server.ce.queue.BranchSupportDelegate;

/**
 * CE calls this as a delegate in the BranchLoader. Set a sane branch name..
 */
public class CodeScanBranchSupportDelegate implements BranchSupportDelegate {

    private final UuidFactory uuidFactory;
    private final System2 system2;
    private final DbClient dbClient;

    public CodeScanBranchSupportDelegate(UuidFactory uuidFactory, System2 system2, DbClient dbClient) {
        this.uuidFactory = uuidFactory;
        this.system2 = system2;
        this.dbClient = dbClient;
    }

    public ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
        String branchTypeParam = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.BRANCH_TYPE_KEY));

        if (null == branchTypeParam) {
            String pullRequest = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.PULL_REQUEST));
            if (null == pullRequest) {
                throw new IllegalArgumentException(String.format("One of '%s' or '%s' parameters must be specified",
                        CeTaskCharacteristicDto.BRANCH_TYPE_KEY,
                        CeTaskCharacteristicDto.PULL_REQUEST));
            } else {
                return new BranchComponent(projectKey,
                        ComponentDto.generatePullRequestKey(projectKey, pullRequest), null,
                        pullRequest);
            }
        }

        String branch = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.BRANCH_KEY));

        try {
            BranchType.valueOf(branchTypeParam);
            return new BranchComponent(projectKey, ComponentDto.generateBranchKey(projectKey, branch), branch, null);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchTypeParam), ex);
        }
    }

    public ComponentDto createBranchComponent(DbSession dbSession, ComponentKey componentKey,
            OrganizationDto organization, ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
        if (!componentKey.getKey().equals(mainComponentDto.getKey())) {
            throw new IllegalStateException("Component Key and Main Component Key do not match");
        }

        Optional<String> branchOptional = componentKey.getBranchName();
        if (branchOptional.isPresent() && branchOptional.get().equals(mainComponentBranchDto.getKey())) {
            return mainComponentDto;
        }

        String branchUuid = uuidFactory.create();
        ComponentDto branchDto = mainComponentDto.copy()
                .setUuid(branchUuid)
                .setProjectUuid(branchUuid)
                .setRootUuid(branchUuid)
                .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
                .setModuleUuidPath(ComponentDto.UUID_PATH_SEPARATOR + branchUuid + ComponentDto.UUID_PATH_SEPARATOR)
                .setMainBranchProjectUuid(mainComponentDto.uuid())
                .setDbKey(componentKey.getDbKey())
                .setCreatedAt(new Date(system2.now()));
        dbClient.componentDao().insert(dbSession, branchDto);
        return branchDto;
    }

    private static final class BranchComponent extends ComponentKey {

        private final String projectKey;
        private final String dbKey;
        private final String branchName;
        private final String pullRequestKey;

        private BranchComponent(String projectKey, String dbKey, @CheckForNull String branchName, String pullRequestKey) {
            this.projectKey = projectKey;
            this.dbKey = dbKey;
            this.branchName = branchName;
            this.pullRequestKey = pullRequestKey;
        }

        public String getKey() {
            return this.projectKey;
        }

        public String getDbKey() {
            return this.dbKey;
        }

        public Optional<String> getDeprecatedBranchName() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getBranchName() {
            return Optional.ofNullable(branchName);
        }

        public Optional<String> getPullRequestKey() {
            return Optional.ofNullable(pullRequestKey);
        }

        public ComponentKey getMainBranchComponentKey() {
            return this.projectKey.equals(this.dbKey) ? this : new BranchComponent(this.projectKey, this.projectKey, null, null);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other != null && this.getClass() == other.getClass()) {
                BranchComponent that = (BranchComponent) other;
                return Objects.equals(this.projectKey, that.projectKey)
                        && Objects.equals(this.dbKey, that.dbKey)
                        && Objects.equals(this.branchName, that.branchName)
                        && Objects.equals(this.pullRequestKey, that.pullRequestKey);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(this.projectKey, this.dbKey, this.branchName, this.pullRequestKey);
        }

        public String toString() {
            return "BranchComponent{key='" + this.projectKey
                    + '\'' + ", dbKey='" + this.dbKey
                    + '\'' + ", branchName=" + this.branchName
                    + '\'' + ", pullRequestKey=" + this.pullRequestKey
                    + '}';
        }
    }
}
