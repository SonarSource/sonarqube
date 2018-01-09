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
import { IconProps } from './types';

export default function IconHistory({ className, fill = 'currentColor', size = 16 }: IconProps) {
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
        style={{ fill }}
        d="M14.7 3.4v3.3c0 .1 0 .2-.1.2s-.2 0-.3-.1l-.9-.9-4.8 4.8c-.1.1-.1.1-.2.1s-.1 0-.2-.1L6.4 9l-3.2 3.2-1.5-1.5 4.5-4.5c.1-.1.1-.1.2-.1s.1 0 .2.1L8.4 8l3.5-3.5-.9-1c-.1-.1-.1-.2-.1-.3s.1-.1.2-.1h3.3c.1 0 .1 0 .2.1.1 0 .1.1.1.2z"
      />
    </svg>
  );
}
