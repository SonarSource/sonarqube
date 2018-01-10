/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { stringify } from 'querystring';
import { omitNil } from '../../../helpers/request';
import { getHostUrl } from '../../../helpers/urls';

export type BadgeColors = 'white' | 'black' | 'orange';

export interface BadgeOptions {
  branch?: string;
  color?: BadgeColors;
  project?: string;
  metric?: string;
}

export enum BadgeType {
  measure = 'measure',
  qualityGate = 'quality_gate',
  marketing = 'marketing'
}

export function getBadgeUrl(
  type: BadgeType,
  { branch, project, color = 'white', metric = 'alert_status' }: BadgeOptions
) {
  switch (type) {
    case BadgeType.marketing:
      return `${getHostUrl()}/images/project_badges/sonarcloud-${color}.svg`;
    case BadgeType.qualityGate:
      return `${getHostUrl()}/api/project_badges/quality_gate?${stringify(
        omitNil({ branch, project })
      )}`;
    case BadgeType.measure:
    default:
      return `${getHostUrl()}/api/project_badges/measure?${stringify(
        omitNil({ branch, project, metric })
      )}`;
  }
}
