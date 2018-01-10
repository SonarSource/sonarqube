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
import { getHostUrl } from '../../../helpers/urls';

export enum StickerType {
  badge = 'badge',
  card = 'card',
  marketing = 'marketing'
}

export interface StickerOptions {
  color?: 'white' | 'black' | 'orange';
}

export function getStickerUrl(type: StickerType, options: StickerOptions) {
  switch (type) {
    case StickerType.marketing:
      return `${getHostUrl()}/images/stickers/sonarcloud-${options.color || 'white'}.svg`;
    case StickerType.card:
      return '';
    case StickerType.badge:
      return '';
  }
}
