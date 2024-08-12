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
import { isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { CCT_SOFTWARE_QUALITY_METRICS, OLD_TAXONOMY_METRICS } from '../../helpers/constants';
import { BranchLike } from '../../types/branch-like';

const METRICS = [
  MetricKey.ncloc,
  ...CCT_SOFTWARE_QUALITY_METRICS,
  ...OLD_TAXONOMY_METRICS,
  MetricKey.security_hotspots,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
];

const APPLICATION_METRICS = [MetricKey.alert_status, ...METRICS];

const PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.software_quality_releasability_rating,
  MetricKey.security_rating,
  MetricKey.software_quality_security_rating,
  MetricKey.reliability_rating,
  MetricKey.software_quality_reliability_rating,
  MetricKey.sqale_rating,
  MetricKey.software_quality_maintainability_rating,
  MetricKey.security_review_rating,
  MetricKey.software_quality_security_review_rating,
  MetricKey.ncloc,
];

const NEW_PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.software_quality_releasability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_software_quality_security_rating,
  MetricKey.new_reliability_rating,
  MetricKey.new_software_quality_reliability_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.new_software_quality_maintainability_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_software_quality_security_review_rating,
  MetricKey.new_lines,
];

const LEAK_METRICS = [
  MetricKey.new_lines,
  ...CCT_SOFTWARE_QUALITY_METRICS,
  ...OLD_TAXONOMY_METRICS,
  MetricKey.security_hotspots,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
];

export function getCodeMetrics(
  qualifier: string,
  branchLike?: BranchLike,
  options: { includeQGStatus?: boolean; newCode?: boolean } = {},
) {
  if (isPortfolioLike(qualifier)) {
    let metrics: MetricKey[] = [];
    if (options?.newCode === undefined) {
      metrics = [...NEW_PORTFOLIO_METRICS, ...PORTFOLIO_METRICS];
    } else if (options?.newCode) {
      metrics = [...NEW_PORTFOLIO_METRICS];
    } else {
      metrics = [...PORTFOLIO_METRICS];
    }
    return options.includeQGStatus ? metrics.concat(MetricKey.alert_status) : metrics;
  }
  if (qualifier === ComponentQualifier.Application) {
    return [...APPLICATION_METRICS];
  }
  if (isPullRequest(branchLike)) {
    return [...LEAK_METRICS];
  }
  return [...METRICS];
}

export function mostCommonPrefix(strings: string[]) {
  const sortedStrings = strings.slice(0).sort((a, b) => a.localeCompare(b));
  const firstString = sortedStrings[0];
  const firstStringLength = firstString.length;
  const lastString = sortedStrings[sortedStrings.length - 1];
  let i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  const prefix = firstString.slice(0, i);
  const prefixTokens = prefix.split(/[\s\\/]/);
  const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
  return prefix.slice(0, prefix.length - lastPrefixPart.length);
}
