/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.qualityprofile.builtin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.qualityprofile.builtin.sonarwayvariants.SonarWayBalancedProfileDefinition;
import org.sonar.server.qualityprofile.builtin.sonarwayvariants.SonarWayEssentialsProfileDefinition;

/**
 * Derives the "Sonar way variants" (e.g. "Sonar way essentials", "Sonar way balanced") from a base profile (e.g.
 * "Sonar way"), keeping only rules that meet a per-variant impact-severity threshold, with optional per-rule
 * force-include/force-exclude overrides. Each variant's content lives in its own definition class under
 * {@code sonarwayvariants} — see {@link SonarWayEssentialsProfileDefinition}, {@link SonarWayBalancedProfileDefinition}
 * — so it can be reviewed/updated independently of this derivation mechanism.
 * <p>
 * Since installing or upgrading a plugin requires a server restart, {@link BuiltInQProfileRepositoryImpl#initialize()}
 * fully recomputes every built-in profile, including these variants, on every startup. A change to "Sonar way" or a
 * newly installed language is therefore already reflected here with no separate propagation mechanism needed.
 */
final class SonarWayVariants {
  private static final Logger LOGGER = Loggers.get(SonarWayVariants.class);
  private static final List<Spec> SPECS = List.of(
    new Spec(SonarWayEssentialsProfileDefinition.NAME, SonarWayEssentialsProfileDefinition.MIN_IMPACT_SEVERITY,
      SonarWayEssentialsProfileDefinition.FORCE_INCLUDED_RULE_KEYS, SonarWayEssentialsProfileDefinition.FORCE_EXCLUDED_RULE_KEYS),
    new Spec(SonarWayBalancedProfileDefinition.NAME, SonarWayBalancedProfileDefinition.MIN_IMPACT_SEVERITY,
      SonarWayBalancedProfileDefinition.FORCE_INCLUDED_RULE_KEYS, SonarWayBalancedProfileDefinition.FORCE_EXCLUDED_RULE_KEYS));

  /**
   * @param forceIncludedRuleKeys rules always kept, even if they don't meet {@code minImpactSeverity}
   * @param forceExcludedRuleKeys rules always dropped, even if they meet {@code minImpactSeverity}; takes precedence
   *                              over {@code forceIncludedRuleKeys} if a rule key is in both sets
   */
  record Spec(String name, Map<SoftwareQuality, Severity> minImpactSeverity,
    Set<RuleKey> forceIncludedRuleKeys, Set<RuleKey> forceExcludedRuleKeys) {
  }

  private record ProfilesForSpec(Spec spec, List<BuiltInQProfile> profiles) {
  }

  private final DbClient dbClient;

  SonarWayVariants(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Derives, from every profile named {@code baseProfileName}, one variant profile per {@link #SPECS} entry,
   * keeping only rules whose impact on at least one software quality meets that variant's minimum impact severity.
   */
  List<BuiltInQProfile> deriveVariantsFrom(List<BuiltInQProfile> profiles, String baseProfileName, Map<RuleKey, RuleDto> rulesByRuleKey) {
    List<BuiltInQProfile> baseProfiles = profiles.stream()
      .filter(profile -> baseProfileName.equals(profile.getName()))
      .toList();
    if (baseProfiles.isEmpty()) {
      return List.of();
    }

    logUnmatchedForceRuleKeys(baseProfiles);

    Set<QProfileName> declaredProfileNames = profiles.stream()
      .map(BuiltInQProfile::getQProfileName)
      .collect(Collectors.toSet());

    List<ProfilesForSpec> variantsBySpec = SPECS.stream()
      .map(spec -> new ProfilesForSpec(spec, baseProfiles.stream()
        .map(profile -> toVariantProfile(profile, rulesByRuleKey, spec))
        .filter(variant -> !collidesWithDeclaredProfile(variant, declaredProfileNames))
        .toList()))
      .toList();

    boolean anyEmptyVariant = variantsBySpec.stream()
      .flatMap(variantForSpec -> variantForSpec.profiles().stream())
      .anyMatch(variant -> variant.getActiveRules().isEmpty());
    // a language whose base-profile rules carry no impact data at all (e.g. test/legacy languages) would
    // otherwise get a meaningless, always-empty variant profile created for it; only keep an empty one
    // here if it was already persisted, so it still goes through the create/update dispatch below and
    // gets reconciled down to zero active rules instead of being silently stranded with stale ones
    Map<String, Set<String>> languagesByPersistedProfileName = anyEmptyVariant ? loadLanguagesByPersistedProfileName() : Map.of();

    return variantsBySpec.stream()
      .flatMap(variantForSpec -> variantForSpec.profiles().stream()
        .filter(variant -> !variant.getActiveRules().isEmpty()
          || languagesByPersistedProfileName.getOrDefault(variantForSpec.spec().name(), Set.of()).contains(variant.getLanguage())))
      .toList();
  }

  /**
   * Logs a warning for any force-included/force-excluded rule key configured on a {@link Spec} that doesn't match
   * any rule active in the base profile (typo, wrong repo key, or a rule renamed/removed upstream) — such a key
   * has no effect on the derived variant, so this is the only signal that it's stale.
   */
  private static void logUnmatchedForceRuleKeys(List<BuiltInQProfile> baseProfiles) {
    Set<RuleKey> activeRuleKeys = baseProfiles.stream()
      .flatMap(profile -> profile.getActiveRules().stream())
      .map(BuiltInQProfile.ActiveRule::getRuleKey)
      .collect(Collectors.toSet());
    for (Spec spec : SPECS) {
      warnIfUnmatched(spec.name(), "force-included", spec.forceIncludedRuleKeys(), activeRuleKeys);
      warnIfUnmatched(spec.name(), "force-excluded", spec.forceExcludedRuleKeys(), activeRuleKeys);
    }
  }

  private static void warnIfUnmatched(String specName, String kind, Set<RuleKey> configuredRuleKeys, Set<RuleKey> activeRuleKeys) {
    Set<RuleKey> unmatched = configuredRuleKeys.stream()
      .filter(ruleKey -> !activeRuleKeys.contains(ruleKey))
      .collect(Collectors.toSet());
    if (!unmatched.isEmpty()) {
      LOGGER.warn("{} {} rule key(s) do not match any rule active in the base profile and will have no effect: {}", specName, kind, unmatched);
    }
  }

  /**
   * A plugin may declare its own profile under the same name as one of our derived variants (e.g. a language
   * shipping a profile literally called "Sonar way balanced"). Persisting both would create two
   * {@link BuiltInQProfile} sharing the same {@link QProfileName}, which downstream code (unique-indexing by
   * name, {@code RegisterQualityProfiles}) assumes cannot happen. Drop the derived one and log instead of failing.
   */
  private static boolean collidesWithDeclaredProfile(BuiltInQProfile variant, Set<QProfileName> declaredProfileNames) {
    if (declaredProfileNames.contains(variant.getQProfileName())) {
      LOGGER.warn("Derived quality profile '{}' for language '{}' collides with a profile already declared by a plugin; " +
        "skipping derivation to avoid persisting a duplicate built-in profile", variant.getName(), variant.getLanguage());
      return true;
    }
    return false;
  }

  private Map<String, Set<String>> loadLanguagesByPersistedProfileName() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.qualityProfileDao().selectBuiltInRuleProfiles(dbSession).stream()
        .collect(Collectors.groupingBy(RulesProfileDto::getName, Collectors.mapping(RulesProfileDto::getLanguage, Collectors.toSet())));
    }
  }

  private static BuiltInQProfile toVariantProfile(BuiltInQProfile baseProfile, Map<RuleKey, RuleDto> rulesByRuleKey, Spec spec) {
    BuiltInQProfile.Builder builder = new BuiltInQProfile.Builder()
      .setLanguage(baseProfile.getLanguage())
      .setName(spec.name());
    baseProfile.getActiveRules().stream()
      .filter(rule -> keepInVariant(rule.getRuleKey(), rulesByRuleKey.get(rule.getRuleKey()), spec))
      .forEach(builder::addRule);
    return builder.build();
  }

  private static boolean keepInVariant(RuleKey ruleKey, @Nullable RuleDto ruleDto, Spec spec) {
    if (spec.forceExcludedRuleKeys().contains(ruleKey)) {
      return false;
    }
    if (spec.forceIncludedRuleKeys().contains(ruleKey)) {
      return true;
    }
    if (ruleDto == null) {
      return false;
    }
    // Security Hotspots are persisted with no default impact at all (see NewRuleCreator), so they would
    // otherwise always be dropped even when a variant is meant to keep every Security impact
    if (ruleDto.getEnumType() == RuleType.SECURITY_HOTSPOT) {
      return spec.minImpactSeverity().containsKey(SoftwareQuality.SECURITY);
    }
    Map<SoftwareQuality, Severity> impacts = ruleDto.getDefaultImpactsMap();
    if (impacts.isEmpty()) {
      return false;
    }
    return impacts.entrySet().stream().anyMatch(entry -> meetsThreshold(entry.getKey(), entry.getValue(), spec.minImpactSeverity()));
  }

  private static boolean meetsThreshold(SoftwareQuality quality, Severity severity, Map<SoftwareQuality, Severity> minImpactSeverity) {
    Severity minSeverity = minImpactSeverity.get(quality);
    return minSeverity != null && severity.compareTo(minSeverity) >= 0;
  }
}
