/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';

type Props = { className?: string, size?: number };

export default function ListIcon({ className, size = 16 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16">
      <path
        fill="currentColor"
        d="M15.045 11.526v1.007q0 0.204-0.149 0.354t-0.354 0.149h-13.084q-0.204 0-0.354-0.149t-0.149-0.354v-1.006q0-0.204 0.149-0.354t0.354-0.149h13.084q0.204 0 0.354 0.149t0.149 0.354zM15.045 8.506v1.006q0 0.204-0.149 0.354t-0.354 0.149h-13.084q-0.204 0-0.354-0.149t-0.149-0.354v-1.006q0-0.204 0.149-0.354t0.354-0.149h13.084q0.204 0 0.354 0.149t0.149 0.354zM15.045 5.487v1.006q0 0.204-0.149 0.354t-0.354 0.149h-13.084q-0.204 0-0.354-0.149t-0.149-0.354v-1.006q0-0.204 0.149-0.354t0.354-0.149h13.084q0.204 0 0.354 0.149t0.149 0.354zM15.045 2.468v1.006q0 0.204-0.149 0.354t-0.354 0.149h-13.084q-0.204 0-0.354-0.149t-0.149-0.354v-1.006q0-0.204 0.149-0.354t0.354-0.149h13.084q0.204 0 0.354 0.149t0.149 0.354z"
      />
    </svg>
  );
}
