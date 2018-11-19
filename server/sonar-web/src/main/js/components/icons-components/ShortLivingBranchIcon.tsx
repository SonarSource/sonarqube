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

interface Props {
  className?: string;
  color?: string;
  size?: number;
}

export default function ShortLivingBranchIcon({ className, color = '#4b9fd5', size = 16 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16">
      <g transform="translate(3, 0)">
        <path
          style={{ fill: color }}
          d="M9.5 6.5c0-1.1-.9-2-2-2s-2 .9-2 2c0 .8.5 1.5 1.2 1.8-.3.6-.7 1.1-1.2 1.4-.9.5-1.9.5-2.5.4V4c.9-.2 1.5-1 1.5-1.9 0-1.1-.9-2-2-2s-2 .9-2 2C.5 3 1.1 3.8 2 4v8c-.9.2-1.5 1-1.5 1.9 0 1.1.9 2 2 2s2-.9 2-2c0-.9-.6-1.7-1.5-1.9v-1c.2 0 .5.1.7.1.7 0 1.5-.1 2.2-.6.8-.5 1.4-1.2 1.7-2.1 1.1 0 1.9-.9 1.9-1.9zm-8-4.4c0-.6.4-1 1-1s1 .4 1 1-.4 1-1 1-1-.5-1-1zm2 11.9c0 .6-.4 1-1 1s-1-.4-1-1 .4-1 1-1 1 .4 1 1zm4-6.5c-.6 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1z"
        />
      </g>
    </svg>
  );
}
