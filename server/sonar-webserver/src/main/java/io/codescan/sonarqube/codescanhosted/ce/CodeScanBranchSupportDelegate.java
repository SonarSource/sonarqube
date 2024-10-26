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
package io.codescan.sonarqube.codescanhosted.ce;

import java.time.Clock;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.config.PurgeConstants;
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
import org.sonar.server.setting.ProjectConfigurationLoader;

/**
 * CE calls this as a delegate in the BranchLoader. Set a sane branch name..
 */
public class CodeScanBranchSupportDelegate implements BranchSupportDelegate {

    private final UuidFactory uuidFactory;
    private final DbClient dbClient;
    private final Clock clock;
    private final ProjectConfigurationLoader projectConfigurationLoader;

    public CodeScanBranchSupportDelegate(UuidFactory uuidFactory, DbClient dbClient, Clock clock,
            ProjectConfigurationLoader projectConfigurationLoader) {
        this.uuidFactory = uuidFactory;
        this.dbClient = dbClient;
        this.clock = clock;
        this.projectConfigurationLoader = projectConfigurationLoader;
    }

    @Override
    public ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
        String branchTypeParam = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristics.BRANCH_TYPE));

        if (null == branchTypeParam) {
            String pullRequest = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristics.PULL_REQUEST));
            if (null == pullRequest) {
                throw new IllegalArgumentException(String.format("One of '%s' or '%s' parameters must be specified",
                    CeTaskCharacteristics.BRANCH_TYPE,
                    CeTaskCharacteristics.PULL_REQUEST));
            } else {
                return new BranchComponent(projectKey, null, pullRequest);
            }
        }

        String branch = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristics.BRANCH));

        try {
            BranchType.valueOf(branchTypeParam);
            return new BranchComponent(projectKey, branch, null);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchTypeParam), ex);
        }
    }

    @Override
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
        ComponentDto componentDto = mainComponentDto.copy()
            .setUuid(branchUuid)
            .setBranchUuid(branchUuid)
            .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
            .setCreatedAt(new Date(clock.millis()));
        dbClient.componentDao().insert(dbSession, componentDto, false);

        BranchDto branchDto = new BranchDto()
                .setProjectUuid(mainComponentBranchDto.getProjectUuid())
                .setUuid(branchUuid);
        componentKey.getPullRequestKey().ifPresent(pullRequestKey -> branchDto.setBranchType(BranchType.PULL_REQUEST)
                .setExcludeFromPurge(false)
                .setIsMain(false)
                .setKey(pullRequestKey));
        componentKey.getBranchName().ifPresent(branchName -> branchDto.setBranchType(BranchType.BRANCH).setIsMain(false)
                .setExcludeFromPurge(isBranchExcludedFromPurge(
                        projectConfigurationLoader.loadProjectConfiguration(dbSession, mainComponentDto.uuid()), branchName))
                .setIsMain(false)
                .setKey(branchName));
        dbClient.branchDao().insert(dbSession, branchDto);

        return componentDto;
    }

    private static boolean isBranchExcludedFromPurge(Configuration projectConfiguration, String branchName) {
        return Arrays.stream(projectConfiguration.getStringArray(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE))
                .map(Pattern::compile)
                .map(Pattern::asMatchPredicate)
                .anyMatch(p -> p.test(branchName));
    }

    private static final class BranchComponent extends ComponentKey {

        private final String projectKey;
        private final String branchName;
        private final String pullRequestKey;

        private BranchComponent(String projectKey, @CheckForNull String branchName, String pullRequestKey) {
            this.projectKey = projectKey;
            this.branchName = branchName;
            this.pullRequestKey = pullRequestKey;
        }

        @Override
        public String getKey() {
            return this.projectKey;
        }

        @Override
        public Optional<String> getBranchName() {
            return Optional.ofNullable(branchName);
        }

        @Override
        public Optional<String> getPullRequestKey() {
            return Optional.ofNullable(pullRequestKey);
        }
    }
}
