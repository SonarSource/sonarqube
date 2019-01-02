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

export default function BubblesIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size} style={{ fillRule: 'nonzero' }}>
      <path
        d="M4.1 10.2c1 0 1.9.8 1.9 1.9S5.1 14 4.1 14s-1.9-.8-1.9-1.9.8-1.9 1.9-1.9m0-2C2 8.2.2 9.9.2 12.1S1.9 16 4.1 16 8 14.3 8 12.1 6.2 8.2 4.1 8.2zM10.3 2c2 0 3.7 1.7 3.7 3.7s-1.7 3.7-3.7 3.7-3.8-1.6-3.8-3.7S8.2 2 10.3 2m0-2C7.1 0 4.5 2.6 4.5 5.7s2.6 5.7 5.7 5.7S16 8.9 16 5.7 13.4 0 10.3 0z"
        style={{ fill }}
      />
    </Icon>
  );
}
