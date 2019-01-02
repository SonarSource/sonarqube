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

export default function LockIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M5.455 7.364h5.09v-1.91A2.55 2.55 0 0 0 8 2.91a2.55 2.55 0 0 0-2.545 2.546v1.909zm8.272.954v5.727a.955.955 0 0 1-.954.955H3.227a.955.955 0 0 1-.954-.955V8.318c0-.527.427-.954.954-.954h.318v-1.91C3.545 3.01 5.554 1 8 1s4.455 2.009 4.455 4.455v1.909h.318c.527 0 .954.427.954.954z"
        style={{ fill }}
      />
    </Icon>
  );
}
