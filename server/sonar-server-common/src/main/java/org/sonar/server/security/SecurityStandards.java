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
package org.sonar.server.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.HIGH;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.LOW;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.MEDIUM;

@Immutable
public final class SecurityStandards {

  public static final String UNKNOWN_STANDARD = "unknown";
  public static final String SANS_TOP_25_INSECURE_INTERACTION = "insecure-interaction";
  public static final String SANS_TOP_25_RISKY_RESOURCE = "risky-resource";
  public static final String SANS_TOP_25_POROUS_DEFENSES = "porous-defenses";

  private static final String OWASP_TOP10_PREFIX = "owaspTop10:";
  private static final String CWE_PREFIX = "cwe:";
  // See https://www.sans.org/top25-software-errors
  private static final Set<String> INSECURE_CWE = new HashSet<>(asList("89", "78", "79", "434", "352", "601"));
  private static final Set<String> RISKY_CWE = new HashSet<>(asList("120", "22", "494", "829", "676", "131", "134", "190"));
  private static final Set<String> POROUS_CWE = new HashSet<>(asList("306", "862", "798", "311", "807", "250", "863", "732", "327", "307", "759"));
  public static final Map<String, Set<String>> CWES_BY_SANS_TOP_25 = ImmutableMap.of(
    SANS_TOP_25_INSECURE_INTERACTION, INSECURE_CWE,
    SANS_TOP_25_RISKY_RESOURCE, RISKY_CWE,
    SANS_TOP_25_POROUS_DEFENSES, POROUS_CWE);

  public enum VulnerabilityProbability {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int score;

    VulnerabilityProbability(int index) {
      this.score = index;
    }

    public int getScore() {
      return score;
    }

    public static Optional<VulnerabilityProbability> byScore(@Nullable Integer score) {
      if (score == null) {
        return Optional.empty();
      }
      return Arrays.stream(values())
        .filter(t -> t.score == score)
        .findFirst();
    }
  }

  public enum SQCategory {
    SQL_INJECTION("sql-injection", HIGH),
    COMMAND_INJECTION("command-injection", HIGH),
    PATH_TRAVERSAL_INJECTION("path-traversal-injection", HIGH),
    LDAP_INJECTION("ldap-injection", LOW),
    XPATH_INJECTION("xpath-injection", LOW),
    RCE("rce", MEDIUM),
    DOS("dos", MEDIUM),
    SSRF("ssrf", MEDIUM),
    CSRF("csrf", HIGH),
    XSS("xss", HIGH),
    LOG_INJECTION("log-injection", LOW),
    HTTP_RESPONSE_SPLITTING("http-response-splitting", LOW),
    OPEN_REDIRECT("open-redirect", MEDIUM),
    XXE("xxe", MEDIUM),
    OBJECT_INJECTION("object-injection", LOW),
    WEAK_CRYPTOGRAPHY("weak-cryptography", MEDIUM),
    AUTH("auth", HIGH),
    INSECURE_CONF("insecure-conf", LOW),
    FILE_MANIPULATION("file-manipulation", LOW),
    OTHERS("others", LOW);

    private static final Map<String, SQCategory> SQ_CATEGORY_BY_KEY = stream(values()).collect(uniqueIndex(SQCategory::getKey));
    private final String key;
    private final VulnerabilityProbability vulnerability;

    SQCategory(String key, VulnerabilityProbability vulnerability) {
      this.key = key;
      this.vulnerability = vulnerability;
    }

    public String getKey() {
      return key;
    }

    public VulnerabilityProbability getVulnerability() {
      return vulnerability;
    }

    public static Optional<SQCategory> fromKey(@Nullable String key) {
      return Optional.ofNullable(key).map(SQ_CATEGORY_BY_KEY::get);
    }
  }

  public static final Map<SQCategory, Set<String>> CWES_BY_SQ_CATEGORY = ImmutableMap.<SQCategory, Set<String>>builder()
    .put(SQCategory.SQL_INJECTION, ImmutableSet.of("89", "564"))
    .put(SQCategory.COMMAND_INJECTION, ImmutableSet.of("77", "78", "88", "214"))
    .put(SQCategory.PATH_TRAVERSAL_INJECTION, ImmutableSet.of("22"))
    .put(SQCategory.LDAP_INJECTION, ImmutableSet.of("90"))
    .put(SQCategory.XPATH_INJECTION, ImmutableSet.of("643"))
    .put(SQCategory.RCE, ImmutableSet.of("94", "95"))
    .put(SQCategory.DOS, ImmutableSet.of("400", "624"))
    .put(SQCategory.SSRF, ImmutableSet.of("918"))
    .put(SQCategory.CSRF, ImmutableSet.of("352"))
    .put(SQCategory.XSS, ImmutableSet.of("79", "80", "81", "82", "83", "84", "85", "86", "87"))
    .put(SQCategory.LOG_INJECTION, ImmutableSet.of("117"))
    .put(SQCategory.HTTP_RESPONSE_SPLITTING, ImmutableSet.of("113"))
    .put(SQCategory.OPEN_REDIRECT, ImmutableSet.of("601"))
    .put(SQCategory.XXE, ImmutableSet.of("611", "827"))
    .put(SQCategory.OBJECT_INJECTION, ImmutableSet.of("134", "470", "502"))
    .put(SQCategory.WEAK_CRYPTOGRAPHY, ImmutableSet.of("295", "297", "321", "322", "323", "324", "325", "326", "327", "328", "330", "780"))
    .put(SQCategory.AUTH, ImmutableSet.of("798", "640", "620", "549", "522", "521", "263", "262", "261", "259", "284"))
    .put(SQCategory.INSECURE_CONF, ImmutableSet.of("102", "215", "311", "315", "346", "614", "489", "942"))
    .put(SQCategory.FILE_MANIPULATION, ImmutableSet.of("97", "73"))
    .build();
  private static final Ordering<SQCategory> SQ_CATEGORY_ORDERING = Ordering.explicit(stream(SQCategory.values()).collect(Collectors.toList()));
  public static final Ordering<String> SQ_CATEGORY_KEYS_ORDERING = Ordering.explicit(stream(SQCategory.values()).map(SQCategory::getKey).collect(Collectors.toList()));

  private final Set<String> standards;
  private final Set<String> cwe;
  private final SQCategory sqCategory;
  private final Set<SQCategory> ignoredSQCategories;

  private SecurityStandards(Set<String> standards, Set<String> cwe, SQCategory sqCategory, Set<SQCategory> ignoredSQCategories) {
    this.standards = standards;
    this.cwe = cwe;
    this.sqCategory = sqCategory;
    this.ignoredSQCategories = ignoredSQCategories;
  }

  public Set<String> getStandards() {
    return standards;
  }

  public Set<String> getCwe() {
    return cwe;
  }

  public Set<String> getOwaspTop10() {
    return toOwaspTop10(standards);
  }

  public Set<String> getSansTop25() {
    return toSansTop25(cwe);
  }

  public SQCategory getSqCategory() {
    return sqCategory;
  }

  /**
   * If CWEs mapped to multiple {@link SQCategory}, those which are not taken into account are listed here.
   */
  public Set<SQCategory> getIgnoredSQCategories() {
    return ignoredSQCategories;
  }

  /**
   * @throws IllegalStateException if {@code securityStandards} maps to multiple {@link SQCategory SQCategories}
   */
  public static SecurityStandards fromSecurityStandards(Set<String> securityStandards) {
    Set<String> standards = securityStandards.stream().filter(Objects::nonNull).collect(toSet());
    Set<String> cwe = toCwes(standards);
    List<SQCategory> sq = toSortedSQCategories(cwe);
    SQCategory sqCategory = sq.iterator().next();
    Set<SQCategory> ignoredSQCategories = sq.stream().skip(1).collect(toSet());
    return new SecurityStandards(standards, cwe, sqCategory, ignoredSQCategories);
  }

  private static Set<String> toOwaspTop10(Set<String> securityStandards) {
    return securityStandards.stream()
      .filter(s -> s.startsWith(OWASP_TOP10_PREFIX))
      .map(s -> s.substring(OWASP_TOP10_PREFIX.length()))
      .collect(toSet());
  }

  private static Set<String> toCwes(Collection<String> securityStandards) {
    Set<String> result = securityStandards.stream()
      .filter(s -> s.startsWith(CWE_PREFIX))
      .map(s -> s.substring(CWE_PREFIX.length()))
      .collect(toSet());
    return result.isEmpty() ? singleton(UNKNOWN_STANDARD) : result;
  }

  private static Set<String> toSansTop25(Collection<String> cwe) {
    return CWES_BY_SANS_TOP_25
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SANS_TOP_25.get(k)::contains))
      .collect(toSet());
  }

  private static List<SQCategory> toSortedSQCategories(Collection<String> cwe) {
    List<SQCategory> result = CWES_BY_SQ_CATEGORY
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SQ_CATEGORY.get(k)::contains))
      .sorted(SQ_CATEGORY_ORDERING)
      .collect(toList());
    return result.isEmpty() ? singletonList(SQCategory.OTHERS) : result;
  }
}
