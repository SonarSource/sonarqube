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

export default function BackIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <Icon {...iconProps}>
      <path
        d="M3.6 8.69l4.07 4.13.04.04a.7.7 0 01.12.7.69.69 0 01-.86.4.73.73 0 01-.26-.16L1 8l5.71-5.8.04-.03A.73.73 0 017.13 2l.1-.01c.1.01.2.04.3.09a.7.7 0 01.3.82c-.03.1-.09.19-.16.27L3.61 7.3c3.59-.03 7.18-.14 10.77.01.05 0 .06 0 .1.02a.68.68 0 01.52.61.7.7 0 01-.57.74h-.1z"
        style={{ fill }}
      />
    </Icon>
  );
}
