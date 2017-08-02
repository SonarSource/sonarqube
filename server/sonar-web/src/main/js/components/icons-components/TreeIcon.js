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

export default function TreeIcon({ className, size = 14 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16"
      fillRule="evenodd"
      clipRule="evenodd"
      strokeLinejoin="round"
      strokeMiterlimit="1.414">
      <path
        fill="currentColor"
        d="M16 1.785c0-0.315-0.256-0.571-0.571-0.571h-14.857c-0.315 0-0.571 0.256-0.571 0.571v1.143c0 0.315 0.256 0.571 0.571 0.571h14.857c0.315 0 0.571-0.256 0.571-0.571v-1.143zM16 5.214c0-0.315-0.22-0.571-0.49-0.571h-12.735c-0.27 0-0.49 0.256-0.49 0.571v1.143c0 0.315 0.219 0.571 0.49 0.571h12.735c0.27 0 0.49-0.256 0.49-0.571v-1.143zM16 8.642c0-0.315-0.183-0.571-0.408-0.571h-10.612c-0.225 0-0.408 0.256-0.408 0.571v1.143c0 0.315 0.183 0.571 0.408 0.571h10.612c0.225 0 0.408-0.256 0.408-0.571v-1.143zM16 12.072c0-0.315-0.146-0.571-0.326-0.571h-8.49c-0.18 0-0.327 0.256-0.327 0.571v1.143c0 0.315 0.146 0.571 0.327 0.571h8.49c0.18 0 0.326-0.256 0.326-0.571v-1.143z"
      />
    </svg>
  );
}
