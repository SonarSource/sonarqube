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

export default function SortAscIcon({ className, fill = 'currentColor', size = 16 }: IconProps) {
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
        d="M6.571 12.857q0 0.107-0.089 0.214l-2.848 2.848q-0.089 0.080-0.205 0.080-0.107 0-0.205-0.080l-2.857-2.857q-0.134-0.143-0.063-0.313 0.071-0.179 0.268-0.179h1.714v-12.286q0-0.125 0.080-0.205t0.205-0.080h1.714q0.125 0 0.205 0.080t0.080 0.205v12.286h1.714q0.125 0 0.205 0.080t0.080 0.205zM16 14v1.714q0 0.125-0.080 0.205t-0.205 0.080h-7.429q-0.125 0-0.205-0.080t-0.080-0.205v-1.714q0-0.125 0.080-0.205t0.205-0.080h7.429q0.125 0 0.205 0.080t0.080 0.205zM14.286 9.429v1.714q0 0.125-0.080 0.205t-0.205 0.080h-5.714q-0.125 0-0.205-0.080t-0.080-0.205v-1.714q0-0.125 0.080-0.205t0.205-0.080h5.714q0.125 0 0.205 0.080t0.080 0.205zM12.571 4.857v1.714q0 0.125-0.080 0.205t-0.205 0.080h-4q-0.125 0-0.205-0.080t-0.080-0.205v-1.714q0-0.125 0.080-0.205t0.205-0.080h4q0.125 0 0.205 0.080t0.080 0.205zM10.857 0.286v1.714q0 0.125-0.080 0.205t-0.205 0.080h-2.286q-0.125 0-0.205-0.080t-0.080-0.205v-1.714q0-0.125 0.080-0.205t0.205-0.080h2.286q0.125 0 0.205 0.080t0.080 0.205z"
      />
    </svg>
  );
}
