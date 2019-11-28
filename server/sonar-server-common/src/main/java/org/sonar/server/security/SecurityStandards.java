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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

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

  public static final Map<String, Set<String>> CWES_BY_SQ_CATEGORY = ImmutableMap.<String, Set<String>>builder()
    .put("sql-injection", ImmutableSet.of("89", "564"))
    .put("command-injection", ImmutableSet.of("77", "78", "88", "214"))
    .put("path-traversal-injection", ImmutableSet.of("22"))
    .put("ldap-injection", ImmutableSet.of("90"))
    .put("xpath-injection", ImmutableSet.of("643"))
    .put("rce", ImmutableSet.of("94", "95"))
    .put("dos", ImmutableSet.of("400", "624"))
    .put("ssrf", ImmutableSet.of("918"))
    .put("csrf", ImmutableSet.of("352"))
    .put("xss", ImmutableSet.of("79", "80", "81", "82", "83", "84", "85", "86", "87"))
    .put("log-injection", ImmutableSet.of("117"))
    .put("http-response-splitting", ImmutableSet.of("113"))
    .put("open-redirect", ImmutableSet.of("601"))
    .put("xxe", ImmutableSet.of("611", "827"))
    .put("object-injection", ImmutableSet.of("134", "470", "502"))
    .put("weak-cryptography", ImmutableSet.of("295", "297", "321", "322", "323", "324", "325", "326", "327", "328", "330", "780"))
    .put("auth", ImmutableSet.of("798", "640", "620", "549", "522", "521", "263", "262", "261", "259", "284"))
    .put("insecure-conf", ImmutableSet.of("102", "215", "311", "315", "346", "614", "489", "942"))
    .put("file-manipulation", ImmutableSet.of("97", "73"))
    .build();
  public static final String SQ_OTHER_CATEGORY = "others";
  public static final Set<String> SQ_CATEGORIES = ImmutableSet.<String>builder().addAll(CWES_BY_SQ_CATEGORY.keySet()).add(SQ_OTHER_CATEGORY).build();
  public static final Ordering<String> SQ_CATEGORIES_ORDERING = Ordering.explicit(
    ImmutableList.<String>builder().addAll(CWES_BY_SQ_CATEGORY.keySet()).add(SQ_OTHER_CATEGORY).build());

  private final Set<String> standards;
  private final Set<String> cwe;
  private final Set<String> owaspTop10;
  private final Set<String> sansTop25;
  private final Set<String> sq;

  private SecurityStandards(Set<String> standards, Set<String> cwe, Set<String> owaspTop10, Set<String> sansTop25, Set<String> sq) {
    this.standards = standards;
    this.cwe = cwe;
    this.owaspTop10 = owaspTop10;
    this.sansTop25 = sansTop25;
    this.sq = sq;
  }

  public Set<String> getStandards() {
    return standards;
  }

  public Set<String> getCwe() {
    return cwe;
  }

  public Set<String> getOwaspTop10() {
    return owaspTop10;
  }

  public Set<String> getSansTop25() {
    return sansTop25;
  }

  public Set<String> getSq() {
    return sq;
  }

  public static SecurityStandards fromSecurityStandards(Set<String> securityStandards) {
    Set<String> standards = securityStandards.stream()
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toSet());
    Set<String> owaspTop10 = toOwaspTop10(standards);
    Set<String> cwe = toCwe(standards);
    Set<String> sansTop25 = toSansTop25(cwe);
    Set<String> sq = toSonarSourceSecurityCategories(cwe);
    return new SecurityStandards(standards, cwe, owaspTop10, sansTop25, sq);
  }

  private static Set<String> toOwaspTop10(Set<String> securityStandards) {
    return securityStandards.stream()
      .filter(s -> s.startsWith(OWASP_TOP10_PREFIX))
      .map(s -> s.substring(OWASP_TOP10_PREFIX.length()))
      .collect(MoreCollectors.toSet());
  }

  private static Set<String> toCwe(Collection<String> securityStandards) {
    Set<String> result = securityStandards.stream()
      .filter(s -> s.startsWith(CWE_PREFIX))
      .map(s -> s.substring(CWE_PREFIX.length()))
      .collect(MoreCollectors.toSet());
    return result.isEmpty() ? singleton(UNKNOWN_STANDARD) : result;
  }

  private static Set<String> toSansTop25(Collection<String> cwe) {
    return CWES_BY_SANS_TOP_25
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SANS_TOP_25.get(k)::contains))
      .collect(MoreCollectors.toSet());
  }

  private static Set<String> toSonarSourceSecurityCategories(Collection<String> cwe) {
    Set<String> result = CWES_BY_SQ_CATEGORY
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SQ_CATEGORY.get(k)::contains))
      .collect(MoreCollectors.toSet());
    return result.isEmpty() ? singleton(SQ_OTHER_CATEGORY) : result;
  }
}
