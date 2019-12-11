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
import { groupBy, sortBy } from 'lodash';
import { DetailedHotspot, RawHotspot, RiskExposure } from '../../types/security-hotspots';

export const RISK_EXPOSURE_LEVELS = [RiskExposure.HIGH, RiskExposure.MEDIUM, RiskExposure.LOW];

export function mapRules(rules: Array<{ key: string; name: string }>): T.Dict<string> {
  return rules.reduce((ruleMap: T.Dict<string>, r) => {
    ruleMap[r.key] = r.name;
    return ruleMap;
  }, {});
}

export function groupByCategory(
  hotspots: RawHotspot[] = [],
  securityCategories: T.StandardSecurityCategories
) {
  const groups = groupBy(hotspots, h => h.securityCategory);

  return sortBy(
    Object.keys(groups).map(key => ({
      key,
      title: getCategoryTitle(key, securityCategories),
      hotspots: groups[key]
    })),
    cat => cat.title
  );
}

export function sortHotspots(
  hotspots: RawHotspot[],
  securityCategories: T.Dict<{ title: string }>
) {
  return sortBy(hotspots, [
    h => RISK_EXPOSURE_LEVELS.indexOf(h.vulnerabilityProbability),
    h => getCategoryTitle(h.securityCategory, securityCategories),
    h => h.message
  ]);
}

function getCategoryTitle(key: string, securityCategories: T.StandardSecurityCategories) {
  return securityCategories[key] ? securityCategories[key].title : key;
}

export function constructSourceViewerFile(
  { component, project }: DetailedHotspot,
  lines?: number
): T.SourceViewerFile {
  return {
    key: component.key,
    measures: { lines: lines ? lines.toString() : undefined },
    path: component.path || '',
    project: project.key,
    projectName: project.name,
    q: component.qualifier,
    uuid: ''
  };
}
