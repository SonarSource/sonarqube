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

export default function EditIcon({ className, size = 14 }: Props) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 14 14"
      width={size}
      height={size}>
      <path
        fill="currentColor"
        d="M3.918 11.918l0.711-0.711-1.836-1.836-0.711 0.711v0.836h1v1h0.836zM8.004 4.668q0-0.172-0.172-0.172-0.078 0-0.133 0.055l-4.234 4.234q-0.055 0.055-0.055 0.133 0 0.172 0.172 0.172 0.078 0 0.133-0.055l4.234-4.234q0.055-0.055 0.055-0.133zM7.582 3.168l3.25 3.25-6.5 6.5h-3.25v-3.25zM12.918 3.918q0 0.414-0.289 0.703l-1.297 1.297-3.25-3.25 1.297-1.289q0.281-0.297 0.703-0.297 0.414 0 0.711 0.297l1.836 1.828q0.289 0.305 0.289 0.711z"
      />
    </svg>
  );
}
