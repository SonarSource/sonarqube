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

export default function DownloadIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size} viewBox="0 0 48 48">
      <path
        d="M45.68 22.86a1.31 1.31 0 0 0-1.32 1.32v12a5.91 5.91 0 0 1-5.9 5.91H9.54a5.91 5.91 0 0 1-5.9-5.91V24A1.32 1.32 0 0 0 1 24v12.16a8.56 8.56 0 0 0 8.54 8.55h28.92A8.56 8.56 0 0 0 47 36.16v-12a1.32 1.32 0 0 0-1.32-1.3z"
        style={{ fill }}
      />
      <path
        d="M23.07 34.24a1.36 1.36 0 0 0 .93.39 1.32 1.32 0 0 0 .93-.39l8.37-8.38A1.32 1.32 0 0 0 31.44 24l-6.12 6.13V3.39a1.32 1.32 0 0 0-2.64 0v26.74L16.55 24a1.32 1.32 0 0 0-1.86 1.86z"
        style={{ fill }}
      />
    </Icon>
  );
}
