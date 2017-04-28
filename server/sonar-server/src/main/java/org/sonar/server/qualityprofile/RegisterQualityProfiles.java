/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.Pagination.forPage;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(RegisterQualityProfiles.class);
  private static final Pagination PROCESSED_ORGANIZATIONS_BATCH_SIZE = forPage(1).andSize(2000);

  private final DefinedQProfileRepository definedQProfileRepository;
  private final DbClient dbClient;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final DefinedQProfileInsert definedQProfileInsert;

  public RegisterQualityProfiles(DefinedQProfileRepository definedQProfileRepository,
    DbClient dbClient, DefinedQProfileInsert definedQProfileInsert, ActiveRuleIndexer activeRuleIndexer) {
    this.definedQProfileRepository = definedQProfileRepository;
    this.dbClient = dbClient;
    this.activeRuleIndexer = activeRuleIndexer;
    this.definedQProfileInsert = definedQProfileInsert;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register quality profiles");
    if (definedQProfileRepository.getQProfilesByLanguage().isEmpty()) {
      return;
    }

    try (DbSession session = dbClient.openSession(false)) {
      List<ActiveRuleChange> changes = new ArrayList<>();
      definedQProfileRepository.getQProfilesByLanguage()
        .forEach((key, value) -> registerPerLanguage(session, value, changes));
      activeRuleIndexer.index(changes);
      profiler.stopDebug();
    }
  }

  private void registerPerLanguage(DbSession session, List<DefinedQProfile> qualityProfiles, List<ActiveRuleChange> changes) {
    qualityProfiles.stream()
      .sorted(new SortByParentName(qualityProfiles))
      .forEach(qp -> registerPerQualityProfile(session, qp, changes));
    session.commit();
  }

  private void registerPerQualityProfile(DbSession session, DefinedQProfile qualityProfile, List<ActiveRuleChange> changes) {
    LOGGER.info("Register profile {}", qualityProfile.getQProfileName());

    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    List<OrganizationDto> organizationDtos;
    while (!(organizationDtos = getOrganizationsWithoutQP(session, qualityProfile)).isEmpty()) {
      organizationDtos.forEach(organization -> registerPerQualityProfileAndOrganization(session, qualityProfile, organization, changes, profiler));
    }
  }

  private List<OrganizationDto> getOrganizationsWithoutQP(DbSession session, DefinedQProfile qualityProfile) {
    return dbClient.organizationDao().selectOrganizationsWithoutLoadedTemplate(session,
      qualityProfile.getLoadedTemplateType(), PROCESSED_ORGANIZATIONS_BATCH_SIZE);
  }

  private void registerPerQualityProfileAndOrganization(DbSession session,
    DefinedQProfile definedQProfile, OrganizationDto organization, List<ActiveRuleChange> changes, Profiler profiler) {
    profiler.start();

    definedQProfileInsert.create(session, definedQProfile, organization, changes);

    session.commit();

    profiler.stopDebug(format("Register profile %s for organization %s", definedQProfile.getQProfileName(), organization.getKey()));
  }

  @VisibleForTesting
  static class SortByParentName implements Comparator<DefinedQProfile> {
    private final Map<String, DefinedQProfile> buildersByName;
    @VisibleForTesting
    final Map<String, Integer> depthByBuilder;

    @VisibleForTesting
    SortByParentName(Collection<DefinedQProfile> builders) {
      buildersByName = builders.stream()
        .collect(MoreCollectors.uniqueIndex(DefinedQProfile::getName, Function.identity(), builders.size()));
      Map<String, Integer> depthByBuilder = new HashMap<>();
      builders.forEach(builder -> depthByBuilder.put(builder.getName(), 0));
      builders.forEach(builder -> increaseDepth(buildersByName, depthByBuilder, builder));
      this.depthByBuilder = ImmutableMap.copyOf(depthByBuilder);
    }

    private void increaseDepth(Map<String, DefinedQProfile> buildersByName, Map<String, Integer> maps, DefinedQProfile builder) {
      Optional.ofNullable(builder.getParentQProfileName())
        .ifPresent(parentQProfileName -> {
          DefinedQProfile parent = buildersByName.get(parentQProfileName.getName());
          if (parent.getParentQProfileName() != null) {
            increaseDepth(buildersByName, maps, parent);
          }
          maps.put(builder.getName(), maps.get(parent.getName()) + 1);
        });
    }

    @Override
    public int compare(DefinedQProfile o1, DefinedQProfile o2) {
      return depthByBuilder.getOrDefault(o1.getName(), 0) - depthByBuilder.getOrDefault(o2.getName(), 0);
    }
  }

  public static class RuleRepository {
    private final Map<RuleKey, RuleDefinitionDto> ruleDefinitions;
    private final Map<RuleKey, Set<RuleParamDto>> ruleParams;

    public RuleRepository(DbClient dbClient, DbSession session) {
      this.ruleDefinitions = dbClient.ruleDao().selectAllDefinitions(session)
        .stream()
        .collect(Collectors.toMap(RuleDefinitionDto::getKey, Function.identity()));
      Map<Integer, RuleKey> ruleIdsByKey = ruleDefinitions.values()
        .stream()
        .collect(MoreCollectors.uniqueIndex(RuleDefinitionDto::getId, RuleDefinitionDto::getKey));
      this.ruleParams = new HashMap<>(ruleIdsByKey.size());
      dbClient.ruleDao().selectRuleParamsByRuleKeys(session, ruleDefinitions.keySet())
        .forEach(ruleParam -> ruleParams.compute(
          ruleIdsByKey.get(ruleParam.getRuleId()),
          (key, value) -> {
            if (value == null) {
              return ImmutableSet.of(ruleParam);
            }
            return ImmutableSet.copyOf(Sets.union(value, Collections.singleton(ruleParam)));
          }));
    }

    public Optional<RuleDefinitionDto> getDefinition(RuleKey ruleKey) {
      return Optional.ofNullable(ruleDefinitions.get(requireNonNull(ruleKey, "RuleKey can't be null")));
    }

    public Set<RuleParamDto> getRuleParams(RuleKey ruleKey) {
      Set<RuleParamDto> res = ruleParams.get(requireNonNull(ruleKey, "RuleKey can't be null"));
      return res == null ? Collections.emptySet() : res;
    }
  }
}
