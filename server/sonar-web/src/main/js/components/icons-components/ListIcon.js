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
        d="M16 12v1.143q0 0.232-0.17 0.402t-0.402 0.17h-14.857q-0.232 0-0.402-0.17t-0.17-0.402v-1.143q0-0.232 0.17-0.402t0.402-0.17h14.857q0.232 0 0.402 0.17t0.17 0.402zM16 8.571v1.143q0 0.232-0.17 0.402t-0.402 0.17h-14.857q-0.232 0-0.402-0.17t-0.17-0.402v-1.143q0-0.232 0.17-0.402t0.402-0.17h14.857q0.232 0 0.402 0.17t0.17 0.402zM16 5.143v1.143q0 0.232-0.17 0.402t-0.402 0.17h-14.857q-0.232 0-0.402-0.17t-0.17-0.402v-1.143q0-0.232 0.17-0.402t0.402-0.17h14.857q0.232 0 0.402 0.17t0.17 0.402zM16 1.714v1.143q0 0.232-0.17 0.402t-0.402 0.17h-14.857q-0.232 0-0.402-0.17t-0.17-0.402v-1.143q0-0.232 0.17-0.402t0.402-0.17h14.857q0.232 0 0.402 0.17t0.17 0.402z"
      />
    </svg>
  );
}
