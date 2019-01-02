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

export default function FilterIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M13.957 2.333a.536.536 0 0 1-.12.596l-4.2 4.202v6.323a.552.552 0 0 1-.333.503.632.632 0 0 1-.213.043.51.51 0 0 1-.384-.162l-2.181-2.182a.542.542 0 0 1-.162-.383V7.13L2.162 2.929a.536.536 0 0 1-.12-.596A.552.552 0 0 1 2.547 2h10.908c.222 0 .418.137.503.333z"
        style={{ fill }}
      />
    </Icon>
  );
}
