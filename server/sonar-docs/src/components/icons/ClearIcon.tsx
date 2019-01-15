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

export default function ClearIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size} viewBox="0 0 48 48">
      <path
        d="M28.24 24L47.07 5.16A3 3 0 1 0 42.93.83l-.09.1L24 19.76 5.16.93A3 3 0 0 0 .93 5.16L19.76 24 .93 42.84a3 3 0 1 0 4.14 4.33l.09-.1L24 28.24l18.84 18.83a3 3 0 1 0 4.33-4.14l-.1-.09z"
        style={{ fill }}
      />
    </Icon>
  );
}
