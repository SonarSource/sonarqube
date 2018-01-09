/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { IconProps } from './types';

export default function TreeIcon({ className, fill = 'currentColor', size = 16 }: IconProps) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 16 16"
      version="1.1"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve">
      <path
        style={{ fill }}
        d="M15.045 2.467c0-0.277-0.225-0.503-0.503-0.503h-13.084c-0.277 0-0.503 0.225-0.503 0.503v1.007c0 0.277 0.225 0.503 0.503 0.503h13.084c0.277 0 0.503-0.225 0.503-0.503v-1.007zM15.045 5.487c0-0.277-0.194-0.503-0.432-0.503h-11.216c-0.238 0-0.432 0.225-0.432 0.503v1.007c0 0.277 0.193 0.503 0.432 0.503h11.216c0.238 0 0.432-0.225 0.432-0.503v-1.007zM15.045 8.506c0-0.277-0.161-0.503-0.359-0.503h-9.346c-0.198 0-0.359 0.225-0.359 0.503v1.007c0 0.277 0.161 0.503 0.359 0.503h9.346c0.198 0 0.359-0.225 0.359-0.503v-1.007zM15.045 11.527c0-0.277-0.129-0.503-0.287-0.503h-7.477c-0.159 0-0.288 0.225-0.288 0.503v1.007c0 0.277 0.129 0.503 0.288 0.503h7.477c0.159 0 0.287-0.225 0.287-0.503v-1.007z"
      />
    </svg>
  );
}
