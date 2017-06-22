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

export default function ChangeIcon({ className, size = 12 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 14 14"
      width={size}
      height={size}>
      <path
        fill="#236a97"
        d="M3.35 12.82l.85-.84L2.02 9.8l-.84.85v.98h1.2v1.2h.97zM8.2 4.24c0-.13-.08-.2-.22-.2-.06 0-.1.02-.15.06l-5 5c-.05.05-.08.1-.08.17 0 .13.07.2.2.2.07 0 .12-.02.16-.06l5.02-5c.05-.04.07-.1.07-.16zm-.5-1.77l3.83 3.84-7.7 7.7H0v-3.84l7.7-7.7zm6.3.88c0 .33-.1.6-.34.84L12.12 5.7 8.28 1.88 9.8.35c.24-.23.5-.35.85-.35.32 0 .6.12.84.35l2.16 2.16c.23.25.34.53.34.85z"
      />
    </svg>
  );
}
