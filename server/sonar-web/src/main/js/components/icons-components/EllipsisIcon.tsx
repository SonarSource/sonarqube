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

export default function EllipsisIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M5.273 7.182v1.636a.818.818 0 0 1-.818.818H2.818A.818.818 0 0 1 2 8.818V7.182c0-.452.366-.818.818-.818h1.637c.451 0 .818.366.818.818zm4.363 0v1.636a.818.818 0 0 1-.818.818H7.182a.818.818 0 0 1-.818-.818V7.182c0-.452.366-.818.818-.818h1.636c.452 0 .818.366.818.818zm4.364 0v1.636a.818.818 0 0 1-.818.818h-1.637a.818.818 0 0 1-.818-.818V7.182c0-.452.367-.818.818-.818h1.637c.452 0 .818.366.818.818z"
        style={{ fill }}
      />
    </Icon>
  );
}
