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

export default function ChevronRightIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M11.596 8.28l-4.604 4.602a.382.382 0 0 1-.279.118.382.382 0 0 1-.279-.118l-1.03-1.03a.382.382 0 0 1-.117-.278c0-.108.04-.201.117-.28L8.7 8 5.404 4.706a.382.382 0 0 1-.117-.28c0-.108.04-.2.117-.279l1.03-1.03A.382.382 0 0 1 6.714 3c.107 0 .2.04.278.118l4.604 4.603a.382.382 0 0 1 .117.279c0 .108-.04.201-.117.28z"
        style={{ fill }}
      />
    </Icon>
  );
}
