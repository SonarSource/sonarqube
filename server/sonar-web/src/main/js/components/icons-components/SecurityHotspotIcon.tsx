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

export default function SecurityHotspotIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <g style={{ fill }}>
        <path
          d="M10.238 2.416c-0.432-0.895-1.259-1.504-2.202-1.504-1.386 0-2.521 1.318-2.521 2.927v5.481"
          fill="none"
          stroke={fill}
          strokeLinecap="round"
          strokeWidth="1.1429"
        />
        <path d="M8.537 10.372v1.199h-1.099v-1.199c-0.638-0.228-1.099-0.832-1.099-1.546 0-0.909 0.739-1.649 1.648-1.649s1.649 0.74 1.649 1.649c0 0.715-0.461 1.32-1.099 1.546zM10.734 4.979h-5.494c-1.21 0-2.199 0.989-2.199 2.197v4.395c0 1.21 0.989 2.199 2.199 2.199h5.494c1.209 0 2.197-0.989 2.197-2.199v-4.395c0-1.209-0.989-2.197-2.197-2.197z" />
        <path d="M4.030 6.352h6.923v6.923h-6.923z" />
        <path
          d="M7.504 10.283c0-0.423 0.048-0.757 0.144-1.002s0.251-0.457 0.465-0.637c0.215-0.18 0.377-0.344 0.489-0.493s0.167-0.313 0.167-0.493c0-0.438-0.189-0.656-0.565-0.656-0.174 0-0.314 0.064-0.421 0.191s-0.164 0.3-0.17 0.518h-1.469c0.006-0.58 0.189-1.031 0.548-1.354s0.864-0.485 1.513-0.485c0.646 0 1.147 0.149 1.501 0.447s0.532 0.723 0.532 1.274c0 0.241-0.048 0.459-0.144 0.656s-0.249 0.398-0.46 0.604l-0.5 0.465c-0.142 0.136-0.241 0.276-0.296 0.42s-0.086 0.325-0.091 0.545h-1.243zM7.326 11.604c0-0.215 0.078-0.39 0.233-0.528s0.349-0.207 0.58-0.207c0.232 0 0.425 0.068 0.58 0.207s0.233 0.313 0.233 0.528-0.078 0.39-0.233 0.528c-0.155 0.138-0.349 0.207-0.58 0.207s-0.425-0.068-0.58-0.207c-0.155-0.138-0.233-0.313-0.233-0.528z"
          fill="#fff"
        />
      </g>
    </Icon>
  );
}
