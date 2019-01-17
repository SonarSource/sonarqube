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
import Icon, { IconProps } from '../../../../components/icons-components/Icon';

export default function SCChevronDownIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size} viewBox="0 0 24 24">
      <path
        d="M4 9a1 1 0 0 1 .29-.71 1 1 0 0 1 1.42 0l6.29 6.3 6.29-6.3a1.0041 1.0041 0 0 1 1.42 1.42l-7 7a1 1 0 0 1-1.42 0l-7-7A1 1 0 0 1 4 9z"
        style={{ fill }}
      />
    </Icon>
  );
}
