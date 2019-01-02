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

export default function SearchIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M10.308 7.077c0-.89-.316-1.65-.949-2.283a3.111 3.111 0 0 0-2.282-.948c-.89 0-1.65.316-2.283.948a3.111 3.111 0 0 0-.948 2.283c0 .89.316 1.65.948 2.282a3.111 3.111 0 0 0 2.283.949c.89 0 1.65-.316 2.282-.949a3.111 3.111 0 0 0 .949-2.282zm3.692 6c0 .25-.091.466-.274.649a.887.887 0 0 1-.65.274.857.857 0 0 1-.648-.274L9.954 11.26c-.86.596-1.82.894-2.877.894a4.989 4.989 0 0 1-1.972-.4 5.076 5.076 0 0 1-1.623-1.082A5.076 5.076 0 0 1 2.4 9.049 4.989 4.989 0 0 1 2 7.077c0-.688.133-1.345.4-1.972a5.076 5.076 0 0 1 1.082-1.623A5.076 5.076 0 0 1 5.105 2.4 4.989 4.989 0 0 1 7.077 2c.687 0 1.345.133 1.972.4a5.076 5.076 0 0 1 1.623 1.082c.454.454.815.995 1.082 1.623.266.627.4 1.284.4 1.972a4.938 4.938 0 0 1-.894 2.877l2.473 2.474a.883.883 0 0 1 .267.649z"
        style={{ fill }}
      />
    </Icon>
  );
}
