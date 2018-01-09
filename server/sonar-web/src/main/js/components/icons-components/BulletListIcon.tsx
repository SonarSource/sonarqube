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

export default function BulletListIcon({ className, fill = 'currentColor', size = 16 }: IconProps) {
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
        d="M2.968 11.274v1.51q0 0.102-0.075 0.177t-0.177 0.075h-1.51q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h1.51q0.102 0 0.177 0.075t0.075 0.177zM2.968 8.255v1.51q0 0.102-0.075 0.177t-0.177 0.075h-1.51q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h1.51q0.102 0 0.177 0.075t0.075 0.177zM2.968 5.235v1.51q0 0.102-0.075 0.177t-0.177 0.075h-1.51q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h1.51q0.102 0 0.177 0.075t0.075 0.177zM15.045 11.274v1.51q0 0.102-0.075 0.177t-0.177 0.075h-10.568q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h10.568q0.102 0 0.177 0.075t0.075 0.177zM2.968 2.216v1.51q0 0.102-0.075 0.177t-0.177 0.075h-1.51q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h1.51q0.102 0 0.177 0.075t0.075 0.177zM15.045 8.255v1.51q0 0.102-0.075 0.177t-0.177 0.075h-10.568q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h10.568q0.102 0 0.177 0.075t0.075 0.177zM15.045 5.235v1.51q0 0.102-0.075 0.177t-0.177 0.075h-10.568q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h10.568q0.102 0 0.177 0.075t0.075 0.177zM15.045 2.216v1.51q0 0.102-0.075 0.177t-0.177 0.075h-10.568q-0.102 0-0.177-0.075t-0.075-0.177v-1.51q0-0.102 0.075-0.177t0.177-0.075h10.568q0.102 0 0.177 0.075t0.075 0.177z"
      />
    </svg>
  );
}
