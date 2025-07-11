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

import { flatten, groupBy, sortBy } from 'lodash';
import { HotspotRatingEnum } from '~design-system';
import {
  renderCASACategory,
  renderCWECategory,
  renderOwaspAsvs40Category,
  renderOwaspTop102021Category,
  renderOwaspTop10Category,
  renderPciDss32Category,
  renderPciDss40Category,
  renderSonarSourceSecurityCategory,
  renderStigCategory,
  renderCVSSCategory,
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
} from '../../types/security-hotspots';
import {
  Dict,
  FlowLocation,
  SourceViewerFile,
  StandardSecurityCategories,
} from '../../types/types';

const OTHERS_SECURITY_CATEGORY = 'others';

export const RISK_EXPOSURE_LEVELS = [
  HotspotRatingEnum.HIGH,
  HotspotRatingEnum.MEDIUM,
  HotspotRatingEnum.LOW,
];
export const SECURITY_STANDARDS = [
  SecurityStandard.SONARSOURCE,
  SecurityStandard.OWASP_TOP10,
  SecurityStandard.OWASP_TOP10_2021,
  SecurityStandard.CWE,
  SecurityStandard.PCI_DSS_3_2,
  SecurityStandard.PCI_DSS_4_0,
  SecurityStandard.OWASP_ASVS_4_0,
  SecurityStandard.CASA,
  SecurityStandard.STIG_ASD_V5R3,
  SecurityStandard.CVSS,
];

export const SECURITY_STANDARD_RENDERER = {
  [SecurityStandard.OWASP_TOP10]: renderOwaspTop10Category,
  [SecurityStandard.OWASP_TOP10_2021]: renderOwaspTop102021Category,
  [SecurityStandard.SONARSOURCE]: renderSonarSourceSecurityCategory,
  [SecurityStandard.CWE]: renderCWECategory,
  [SecurityStandard.PCI_DSS_3_2]: renderPciDss32Category,
  [SecurityStandard.PCI_DSS_4_0]: renderPciDss40Category,
  [SecurityStandard.OWASP_ASVS_4_0]: renderOwaspAsvs40Category,
  [SecurityStandard.CASA]: renderCASACategory,
  [SecurityStandard.STIG_ASD_V5R3]: renderStigCategory,
  [SecurityStandard.CVSS]: renderCVSSCategory,
};

export function mapRules(rules: Array<{ key: string; name: string }>): Dict<string> {
  return rules.reduce((ruleMap: Dict<string>, r) => {
    ruleMap[r.key] = r.name;
    return ruleMap;
  }, {});
}

export function groupByCategory(
  hotspots: RawHotspot[] = [],
  securityCategories: StandardSecurityCategories,
) {
  const groups = groupBy(hotspots, (h) => h.securityCategory);

  const groupList = Object.keys(groups).map((key) => ({
    key,
    title: getCategoryTitle(key, securityCategories),
    hotspots: groups[key],
  }));

  return [
    ...sortBy(
      groupList.filter((group) => group.key !== OTHERS_SECURITY_CATEGORY),
      (group) => group.title,
    ),
    ...groupList.filter(({ key }) => key === OTHERS_SECURITY_CATEGORY),
  ];
}

export function sortHotspots(hotspots: RawHotspot[], securityCategories: Dict<{ title: string }>) {
  return sortBy(hotspots, [
    (h) => RISK_EXPOSURE_LEVELS.indexOf(h.vulnerabilityProbability),
    (h) => getCategoryTitle(h.securityCategory, securityCategories),
    (h) => h.message,
  ]);
}

function getCategoryTitle(key: string, securityCategories: StandardSecurityCategories) {
  return securityCategories[key] ? securityCategories[key].title : key;
}

export function constructSourceViewerFile(
  { component, project }: Hotspot,
  lines?: number,
): SourceViewerFile {
  return {
    key: component.key,
    measures: { lines: lines ? lines.toString() : undefined },
    path: component.path || '',
    project: project.key,
    projectName: project.name,
    q: component.qualifier,
    uuid: '',
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
        name: hotspot.authorUser.name || hotspot.authorUser.login,
      },
    });
  }

  if (hotspot.changelog && hotspot.changelog.length > 0) {
    history.push(
      ...hotspot.changelog.map((log) => ({
        type: ReviewHistoryType.Diff,
        date: log.creationDate,
        user: {
          active: log.isUserActive,
          avatar: log.avatar,
          name: log.userName || log.user,
        },
        diffs: log.diffs,
      })),
    );
  }

  if (hotspot.comment && hotspot.comment.length > 0) {
    history.push(
      ...hotspot.comment.map((comment) => ({
        type: ReviewHistoryType.Comment,
        date: comment.createdAt,
        updatable: comment.updatable,
        user: {
          ...comment.user,
          name: comment.user.name || comment.user.login,
        },
        html: comment.htmlText,
        key: comment.key,
        markdown: comment.markdown,
      })),
    );
  }

  return sortBy(history, (elt) => elt.date).reverse();
}

const STATUS_AND_RESOLUTION_TO_STATUS_OPTION = {
  [HotspotStatus.TO_REVIEW]: HotspotStatusOption.TO_REVIEW,
  [HotspotStatus.REVIEWED]: HotspotStatusOption.FIXED,
  [HotspotResolution.ACKNOWLEDGED]: HotspotStatusOption.ACKNOWLEDGED,
  [HotspotResolution.EXCEPTION]: HotspotStatusOption.EXCEPTION,
  [HotspotResolution.FIXED]: HotspotStatusOption.FIXED,
  [HotspotResolution.SAFE]: HotspotStatusOption.SAFE,
};

export function getStatusOptionFromStatusAndResolution(
  status: HotspotStatus,
  resolution?: HotspotResolution,
) {
  // Resolution is the most determinist info here, so we use it first to get the matching status option
  // If not provided, we use the status (which will be TO_REVIEW)
  return STATUS_AND_RESOLUTION_TO_STATUS_OPTION[resolution ?? status];
}

const STATUS_OPTION_TO_STATUS_AND_RESOLUTION_MAP = {
  [HotspotStatusOption.TO_REVIEW]: { status: HotspotStatus.TO_REVIEW, resolution: undefined },
  [HotspotStatusOption.ACKNOWLEDGED]: {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.ACKNOWLEDGED,
  },
  [HotspotStatusOption.EXCEPTION]: {
      status: HotspotStatus.REVIEWED,
      resolution: HotspotResolution.EXCEPTION,
    },
  [HotspotStatusOption.FIXED]: {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.FIXED,
  },
  [HotspotStatusOption.SAFE]: {
    status: HotspotStatus.REVIEWED,
    resolution: HotspotResolution.SAFE,
  },
};

export function getStatusAndResolutionFromStatusOption(statusOption: HotspotStatusOption) {
  return STATUS_OPTION_TO_STATUS_AND_RESOLUTION_MAP[statusOption];
}

const STATUS_OPTION_TO_STATUS_FILTER = {
  [HotspotStatusOption.TO_REVIEW]: HotspotStatusFilter.TO_REVIEW,
  [HotspotStatusOption.ACKNOWLEDGED]: HotspotStatusFilter.ACKNOWLEDGED,
  [HotspotStatusOption.FIXED]: HotspotStatusFilter.FIXED,
  [HotspotStatusOption.SAFE]: HotspotStatusFilter.SAFE,
  [HotspotStatusOption.EXCEPTION]: HotspotStatusFilter.EXCEPTION,
};

export function getStatusFilterFromStatusOption(statusOption: HotspotStatusOption) {
  return STATUS_OPTION_TO_STATUS_FILTER[statusOption];
}

function getSecondaryLocations(flows: RawHotspot['flows']) {
  const parsedFlows: FlowLocation[][] = (flows || [])
    .filter((flow) => flow.locations !== undefined)
    .map((flow) => flow.locations!.filter((location) => location.textRange != null))
    .map((flow) =>
      flow.map((location) => {
        return { ...location };
      }),
    );

  const onlySecondaryLocations = parsedFlows.every((flow) => flow.length === 1);

  return onlySecondaryLocations
    ? { secondaryLocations: orderLocations(flatten(parsedFlows)), flows: [] }
    : { secondaryLocations: [], flows: parsedFlows.map(reverseLocations) };
}

export function getLocations(rawFlows: RawHotspot['flows'], selectedFlowIndex: number | undefined) {
  const { flows, secondaryLocations } = getSecondaryLocations(rawFlows);
  if (selectedFlowIndex !== undefined) {
    return flows[selectedFlowIndex] || [];
  }
  return flows.length > 0 ? flows[0] : secondaryLocations;
}

function orderLocations(locations: FlowLocation[]) {
  return sortBy(
    locations,
    (location) => location.textRange?.startLine,
    (location) => location.textRange?.startOffset,
  );
}

function reverseLocations(locations: FlowLocation[]): FlowLocation[] {
  const x = [...locations];
  x.reverse();
  return x;
}

export function getFilePath(component: string, project: string) {
  return component.replace(project, '').replace(':', '');
}
