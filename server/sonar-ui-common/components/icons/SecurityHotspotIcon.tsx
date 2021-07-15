/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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

export default function SecurityHotspotIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <path
        d="M14.08 3.23a1 1 0 00-.67-.77L8.16 1a1.06 1.06 0 00-.5 0L2.41 2.46a.94.94 0 00-.67.77c-.08.57-.74 5.63 1.14 8.31A9 9 0 007.68 15a.85.85 0 00.23 0 .78.78 0 00.22 0 8.93 8.93 0 004.81-3.46c1.85-2.68 1.21-7.74 1.14-8.31zM12.21 8a6.15 6.15 0 01-.86 2.42A7.92 7.92 0 018 13V8zM8 3v5H3.59a24.29 24.29 0 010-3.82z"
        style={{ fill }}
      />
    </Icon>
  );
}
