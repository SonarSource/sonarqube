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

export default function LinkIcon({ className, size = 14 }: Props) {
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
        d="M13.501 11.429q0-0.357-0.25-0.607l-1.857-1.857q-0.25-0.25-0.607-0.25-0.375 0-0.643 0.286 0.027 0.027 0.17 0.165t0.192 0.192 0.134 0.17 0.116 0.228 0.031 0.246q0 0.357-0.25 0.607t-0.607 0.25q-0.134 0-0.246-0.031t-0.228-0.116-0.17-0.134-0.192-0.192-0.165-0.17q-0.295 0.277-0.295 0.652 0 0.357 0.25 0.607l1.839 1.848q0.241 0.241 0.607 0.241 0.357 0 0.607-0.232l1.313-1.304q0.25-0.25 0.25-0.598zM7.224 5.134q0-0.357-0.25-0.607l-1.839-1.848q-0.25-0.25-0.607-0.25-0.348 0-0.607 0.241l-1.313 1.304q-0.25 0.25-0.25 0.598 0 0.357 0.25 0.607l1.857 1.857q0.241 0.241 0.607 0.241 0.375 0 0.643-0.277-0.027-0.027-0.17-0.165t-0.192-0.192-0.134-0.17-0.116-0.228-0.031-0.246q0-0.357 0.25-0.607t0.607-0.25q0.134 0 0.246 0.031t0.228 0.116 0.17 0.134 0.192 0.192 0.165 0.17q0.295-0.277 0.295-0.652zM15.215 11.429q0 1.071-0.759 1.813l-1.313 1.304q-0.741 0.741-1.813 0.741-1.080 0-1.821-0.759l-1.839-1.848q-0.741-0.741-0.741-1.813 0-1.098 0.786-1.866l-0.786-0.786q-0.768 0.786-1.857 0.786-1.071 0-1.821-0.75l-1.857-1.857q-0.75-0.75-0.75-1.821t0.759-1.813l1.313-1.304q0.741-0.741 1.813-0.741 1.080 0 1.821 0.759l1.839 1.848q0.741 0.741 0.741 1.813 0 1.098-0.786 1.866l0.786 0.786q0.768-0.786 1.857-0.786 1.071 0 1.821 0.75l1.857 1.857q0.75 0.75 0.75 1.821z"
      />
    </svg>
  );
}
