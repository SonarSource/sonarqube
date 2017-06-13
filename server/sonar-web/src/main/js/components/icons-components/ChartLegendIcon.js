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

export default function ChartLegendIcon({ className, size = 16 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 16 16"
      width={size}
      height={size}>
      <path
        style={{ fill: 'currentColor' }}
        d="M14.325 7.143v1.714q0 0.357-0.25 0.607t-0.607 0.25h-10.857q-0.357 0-0.607-0.25t-0.25-0.607v-1.714q0-0.357 0.25-0.607t0.607-0.25h10.857q0.357 0 0.607 0.25t0.25 0.607z"
      />
    </svg>
  );
}
