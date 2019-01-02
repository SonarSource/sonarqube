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

export default function PinIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M7.25 7.25v-3.5a.243.243 0 0 0-.07-.18A.243.243 0 0 0 7 3.5a.243.243 0 0 0-.18.07.243.243 0 0 0-.07.18v3.5c0 .073.023.133.07.18.047.047.107.07.18.07a.243.243 0 0 0 .18-.07.243.243 0 0 0 .07-.18zM12.5 10a.482.482 0 0 1-.148.352.482.482 0 0 1-.352.148H8.648l-.398 3.773a.29.29 0 0 1-.082.161.219.219 0 0 1-.16.066H8c-.141 0-.224-.07-.25-.211L7.156 10.5H4a.482.482 0 0 1-.352-.148A.482.482 0 0 1 3.5 10c0-.641.204-1.217.613-1.73.409-.513.871-.77 1.387-.77v-4a.96.96 0 0 1-.703-.297A.96.96 0 0 1 4.5 2.5a.96.96 0 0 1 .297-.703A.96.96 0 0 1 5.5 1.5h5a.96.96 0 0 1 .703.297.96.96 0 0 1 .297.703.96.96 0 0 1-.297.703.96.96 0 0 1-.703.297v4c.516 0 .978.257 1.387.77.409.513.613 1.089.613 1.73z"
        style={{ fill }}
      />
    </Icon>
  );
}
