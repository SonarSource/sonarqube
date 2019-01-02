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
import * as theme from '../../app/theme';

export default function WarningIcon({ className, fill = theme.warningIconColor, size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M9 12.242v-1.484c0-.14-.11-.258-.25-.258h-1.5c-.14 0-.25.117-.25.258v1.484c0 .14.11.258.25.258h1.5c.14 0 .25-.117.25-.258zM8.984 9.32l.141-3.586a.189.189 0 0 0-.078-.148C9 5.546 8.93 5.5 8.859 5.5H7.141c-.07 0-.141.047-.188.086-.055.039-.078.117-.078.164l.133 3.57c0 .102.117.18.265.18H8.72c.14 0 .258-.078.265-.18zm-.109-7.297l6 11A1 1 0 0 1 14 14.5H2a1 1 0 0 1-.875-1.477l6-11a.994.994 0 0 1 1.75 0z"
        style={{ fill }}
      />
    </Icon>
  );
}
