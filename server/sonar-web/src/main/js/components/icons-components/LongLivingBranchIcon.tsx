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
import * as React from 'react';

interface Props {
  className?: string;
  color?: string;
  size?: number;
}

export default function LongLivingBranchIcon({ className, color = '#4b9fd5', size = 16 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16">
      <g transform="translate(5, 0)">
        <path
          style={{ fill: color }}
          d="M4.5 8c0-.9-.6-1.7-1.5-1.9V4c.9-.2 1.5-1 1.5-1.9 0-1.1-.9-2-2-2s-2 .9-2 2C.5 3 1.1 3.8 2 4v2.1C1.1 6.3.5 7.1.5 8s.6 1.7 1.5 2v2.1c-.9.2-1.5 1-1.5 1.9 0 1.1.9 2 2 2s2-.9 2-2c0-.9-.6-1.7-1.5-1.9V10c.9-.3 1.5-1 1.5-2zm-3-5.9c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.5-1-1zm0 5.9c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.4-1-1zm2 6c0 .6-.4 1-1 1s-1-.4-1-1 .4-1 1-1 1 .5 1 1z"
        />
      </g>
    </svg>
  );
}
