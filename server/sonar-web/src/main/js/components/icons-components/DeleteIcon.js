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

export default function DeleteIcon({ className, size = 12 }: Props) {
  /* eslint-disable max-len */
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 14 14"
      width={size}
      height={size}>
      <path
        fill="#d4333f"
        d="M14 11.27c0 .3-.1.58-.33.8l-1.6 1.6c-.22.22-.5.33-.8.33-.32 0-.6-.1-.8-.33L7 10.2l-3.46 3.47c-.22.22-.5.33-.8.33-.32 0-.6-.1-.8-.33l-1.6-1.6c-.23-.22-.34-.5-.34-.8 0-.32.1-.6.33-.8L3.8 7 .32 3.54C.1 3.32 0 3.04 0 2.74c0-.32.1-.6.33-.8l1.6-1.6c.22-.23.5-.34.8-.34.32 0 .6.1.8.33L7 3.8 10.46.32c.22-.22.5-.33.8-.33.32 0 .6.1.8.33l1.6 1.6c.23.22.34.5.34.8 0 .32-.1.6-.33.8L10.2 7l3.47 3.46c.22.22.33.5.33.8z"
      />
    </svg>
  );
}
