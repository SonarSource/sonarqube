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
  size?: number;
}

export default function ProjectEventIcon({ className, size = 14 }: Props) {
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
        style={{ fill: '#fff', stroke: 'currentColor', strokeWidth: '2px' }}
        d="M8 2 L14 8 L8 14 L2 8 L8 2 L14 8"
      />
    </svg>
  );
}
