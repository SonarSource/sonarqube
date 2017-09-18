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
  open: boolean;
  size?: number;
}

export default function OpenCloseIcon({ className, open, size = 14 }: Props) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 16 16"
      width={size}
      height={size}
      style={{ fill: 'currentColor' }}>
      {open ? (
        <path d="M13.506 9.289l-5.191 5.184q-0.133 0.133-0.315 0.133t-0.315-0.133l-5.191-5.184q-0.133-0.133-0.133-0.318t0.133-0.318l1.161-1.154q0.133-0.133 0.315-0.133t0.315 0.133l3.715 3.715 3.715-3.715q0.133-0.133 0.315-0.133t0.315 0.133l1.161 1.154q0.133 0.133 0.133 0.318t-0.133 0.318z" />
      ) : (
        <path d="M13.527 9.318l-5.244 5.244q-0.134 0.134-0.318 0.134t-0.318-0.134l-1.173-1.173q-0.134-0.134-0.134-0.318t0.134-0.318l3.753-3.753-3.753-3.753q-0.134-0.134-0.134-0.318t0.134-0.318l1.173-1.173q0.134-0.134 0.318-0.134t0.318 0.134l5.244 5.244q0.134 0.134 0.134 0.318t-0.134 0.318z" />
      )}
    </svg>
  );
}
