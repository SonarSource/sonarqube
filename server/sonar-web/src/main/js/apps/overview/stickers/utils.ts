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

export type StickerColors = 'white' | 'black' | 'orange';

export interface StickerOptions {
  branch?: string;
  color: StickerColors;
  component?: string;
  metric: string;
}

export enum StickerType {
  marketing = 'marketing',
  measure = 'measure',
  qualityGate = 'quality_gate'
}

export function getStickerUrl(
  type: StickerType,
  { branch, color, component, metric }: StickerOptions
) {
  switch (type) {
    case StickerType.marketing:
      return `${getHostUrl()}/images/stickers/sonarcloud-${color}.svg`;
    case StickerType.measure:
      return `${getHostUrl()}/api/stickers/measure?${stringify(
        omitNil({
          branch,
          component,
          metric
        })
      )}`;
  }
  return '';
}
