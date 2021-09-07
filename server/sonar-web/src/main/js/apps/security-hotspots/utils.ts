/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import {
  renderCWECategory,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory
} from '../../helpers/security-standard';
import { SecurityStandard } from '../../types/security';
import {
  Hotspot,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
  HotspotStatusOption,
  RawHotspot,
  ReviewHistoryElement,
  ReviewHistoryType,
  RiskExposure
} from '../../types/security-hotspots';

export const RISK_EXPOSURE_LEVELS = [RiskExposure.HIGH, RiskExposure.MEDIUM, RiskExposure.LOW];
export const SECURITY_STANDARDS = [
  SecurityStandard.SONARSOURCE,
  SecurityStandard.OWASP_TOP10,
  SecurityStandard.SANS_TOP25,
  SecurityStandard.CWE
];

export const SECURITY_STANDARD_RENDERER = {
  [SecurityStandard.OWASP_TOP10]: renderOwaspTop10Category,
  [SecurityStandard.SANS_TOP25]: renderSansTop25Category,
  [SecurityStandard.SONARSOURCE]: renderSonarSourceSecurityCategory,
  [SecurityStandard.CWE]: renderCWECategory
};

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

  return Object.keys(groups).map(key => ({
    key,
    title: getCategoryTitle(key, securityCategories),
    hotspots: groups[key]
  }));
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
  { component, project }: Hotspot,
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

export function getHotspotReviewHistory(hotspot: Hotspot): ReviewHistoryElement[] {
  const history: ReviewHistoryElement[] = [];

  if (hotspot.creationDate) {
    history.push({
      type: ReviewHistoryType.Creation,
      date: hotspot.creationDate,
      user: {
        ...hotspot.authorUser,
        name: hotspot.authorUser.name || hotspot.authorUser.login
      }
    });
  }

  if (hotspot.changelog && hotspot.changelog.length > 0) {
    history.push(
      ...hotspot.changelog.map(log => ({
        type: ReviewHistoryType.Diff,
        date: log.creationDate,
        user: {
          active: log.isUserActive,
          avatar: log.avatar,
          name: log.userName || log.user
        },
        diffs: log.diffs
      }))
    );
  }

  if (hotspot.comment && hotspot.comment.length > 0) {
    history.push(
      ...hotspot.comment.map(comment => ({
        type: ReviewHistoryType.Comment,
        date: comment.createdAt,
        updatable: comment.updatable,
        user: {
          ...comment.user,
          name: comment.user.name || comment.user.login
        },
        html: comment.htmlText,
        key: comment.key,
        markdown: comment.markdown
      }))
    );
  }

  return sortBy(history, elt => elt.date);
}

const STATUS_AND_RESOLUTION_TO_STATUS_OPTION = {
  [HotspotStatus.TO_REVIEW]: HotspotStatusOption.TO_REVIEW,
  [HotspotStatus.REVIEWED]: HotspotStatusOption.FIXED,
  [HotspotResolution.FIXED]: HotspotStatusOption.FIXED,
  [HotspotResolution.SAFE]: HotspotStatusOption.SAFE
};

export function getStatusOptionFromStatusAndResolution(
  status: HotspotStatus,
  resolution?: HotspotResolution
) {
  // Resolution is the most determinist info here, so we use it first to get the matching status option
  // If not provided, we use the status (which will be TO_REVIEW)
  return STATUS_AND_RESOLUTION_TO_STATUS_OPTION[resolution ?? status];
}

const STATUS_OPTION_TO_STATUS_AND_RESOLUTION_MAP = {
  [HotspotStatusOption.TO_REVIEW]: { status: HotspotStatus.TO_REVIEW, resolution: undefined },
  [HotspotStatusOption.FIXED]: {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.FIXED
  },
  [HotspotStatusOption.SAFE]: {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.SAFE
  }
};

export function getStatusAndResolutionFromStatusOption(statusOption: HotspotStatusOption) {
  return STATUS_OPTION_TO_STATUS_AND_RESOLUTION_MAP[statusOption];
}

const STATUS_OPTION_TO_STATUS_FILTER = {
  [HotspotStatusOption.TO_REVIEW]: HotspotStatusFilter.TO_REVIEW,
  [HotspotStatusOption.FIXED]: HotspotStatusFilter.FIXED,
  [HotspotStatusOption.SAFE]: HotspotStatusFilter.SAFE
};

export function getStatusFilterFromStatusOption(statusOption: HotspotStatusOption) {
  return STATUS_OPTION_TO_STATUS_FILTER[statusOption];
}
