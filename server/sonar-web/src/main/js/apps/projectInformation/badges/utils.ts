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

import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';
import { getHostUrl, getPathUrlAsString, getProjectUrl } from '../../../helpers/urls';

export type BadgeColors = 'white' | 'black' | 'orange';
export type BadgeFormats = 'md' | 'url';

export interface BadgeOptions {
  branch?: string;
  format?: BadgeFormats;
  metric?: string;
  project?: string;
  pullRequest?: string;
}

export enum BadgeType {
  measure = 'measure',
  qualityGate = 'quality_gate',
  aiCodeAssurance = 'ai_code_assurance',
}

export function getBadgeSnippet(type: BadgeType, options: BadgeOptions, token: string) {
  const url = getBadgeUrl(type, options, token);
  const { branch, format = 'md', metric = 'alert_status', project } = options;

  if (format === 'url') {
    return url;
  }

  let label;
  let projectUrl;

  switch (type) {
    case BadgeType.measure:
      label = getLocalizedMetricName({ key: metric });
      break;
    case BadgeType.aiCodeAssurance:
      label = translate('overview.badges.ai_code_assurance');
      break;
    case BadgeType.qualityGate:
    default:
      label = 'Quality gate';
      break;
  }

  if (project) {
    projectUrl = getPathUrlAsString(getProjectUrl(project, branch), false);
  }

  const mdImage = `![${label}](${url})`;
  return projectUrl ? `[${mdImage}](${projectUrl})` : mdImage;
}

export function getBadgeUrl(
  type: BadgeType,
  { branch, project, metric = 'alert_status', pullRequest }: BadgeOptions,
  token: string,
  disableBrowserCache: boolean = false,
) {
  switch (type) {
    case BadgeType.qualityGate:
      return `${getHostUrl()}/api/project_badges/quality_gate?${new URLSearchParams(
        omitNil({ branch, project, pullRequest, token }),
      ).toString()}${disableBrowserCache ? `&${new Date().getTime()}` : ''}`;
    case BadgeType.aiCodeAssurance:
      return `${getHostUrl()}/api/project_badges/ai_code_assurance?${new URLSearchParams(
        omitNil({ branch, project, pullRequest, token }),
      ).toString()}${disableBrowserCache ? `&${new Date().getTime()}` : ''}`;

    case BadgeType.measure:
    default:
      return `${getHostUrl()}/api/project_badges/measure?${new URLSearchParams(
        omitNil({ branch, project, metric, pullRequest, token }),
      ).toString()}${disableBrowserCache ? `&${new Date().getTime()}` : ''}`;
  }
}
