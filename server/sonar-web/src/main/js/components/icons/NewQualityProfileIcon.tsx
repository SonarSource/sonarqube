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
import * as React from 'react';
import Icon, { IconProps } from './Icon';

export default function NewQualityProfileIcon({
  ...iconProps
}: Omit<IconProps, 'viewBox' | 'fill'>) {
  return (
    <Icon {...iconProps} viewBox="0 0 82 64">
      <path
        d="M28 18.4201C28 15.9787 29.9787 14 32.4201 14H43.8504C45.0245 14 46.1433 14.4657 46.9721 15.295L53.2293 21.5488C54.0581 22.3776 54.5209 23.4964 54.5209 24.6705V44.941C54.5209 47.379 52.5387 49.3612 50.1007 49.3612H32.4201C29.9787 49.3612 28 47.379 28 44.941V18.4201ZM51.2058 44.941V25.0504H45.6806C44.4581 25.0504 43.4705 24.0627 43.4705 22.8403V17.3151H32.4201C31.8096 17.3151 31.3151 17.8096 31.3151 18.4201V44.941C31.3151 45.5488 31.8096 46.046 32.4201 46.046H50.1007C50.7085 46.046 51.2058 45.5488 51.2058 44.941Z"
        fill="#236A97"
      />
    </Icon>
  );
}
