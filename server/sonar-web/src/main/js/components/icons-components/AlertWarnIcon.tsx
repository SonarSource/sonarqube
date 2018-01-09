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

export default function AlertWarnIcon({ className, fill = '#ed7d20', size = 16 }: IconProps) {
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
        d="M8 1.143q1.866 0 3.442.92t2.496 2.496.92 3.442-.92 3.442-2.496 2.496-3.442.92-3.442-.92-2.496-2.496-.92-3.442.92-3.442 2.496-2.496T8 1.143zm1.143 11.134v-1.696q0-.125-.08-.21t-.196-.085H7.153q-.116 0-.205.089t-.089.205v1.696q0 .116.089.205t.205.089h1.714q.116 0 .196-.085t.08-.21zm-.018-3.072l.161-5.545q0-.107-.089-.161-.089-.071-.214-.071H7.019q-.125 0-.214.071-.089.054-.089.161l.152 5.545q0 .089.089.156t.214.067h1.652q.125 0 .21-.067t.094-.156z"
      />
    </svg>
  );
}
