/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

export default function ChevronUpIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M8.28 4.404l4.602 4.604a.382.382 0 0 1 .118.279c0 .108-.04.2-.118.279l-1.03 1.03a.382.382 0 0 1-.278.117.382.382 0 0 1-.28-.117L8 7.3l-3.294 3.295a.382.382 0 0 1-.28.117.382.382 0 0 1-.279-.117l-1.03-1.03A.382.382 0 0 1 3 9.286c0-.107.04-.2.118-.278L7.72 4.404A.382.382 0 0 1 8 4.287c.108 0 .201.04.28.117z"
        style={{ fill }}
      />
    </Icon>
  );
}
