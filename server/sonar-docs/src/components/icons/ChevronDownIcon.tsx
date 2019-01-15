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

export default function ChevronDownIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M3.2,5.6c0-0.1,0-0.2,0.1-0.3c0.2-0.2,0.5-0.2,0.6,0l4.1,4.1l4.1-4.1c0.2-0.2,0.5-0.2,0.6,0 c0.2,0.2,0.2,0.5,0,0.6c0,0,0,0,0,0l0,0l-4.5,4.5c-0.2,0.2-0.5,0.2-0.6,0l0,0L3.3,5.9C3.2,5.9,3.2,5.7,3.2,5.6z"
        style={{ fill }}
      />
    </Icon>
  );
}
