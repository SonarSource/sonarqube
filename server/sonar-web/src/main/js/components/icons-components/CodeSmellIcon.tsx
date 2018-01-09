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

export default function CodeSmellIcon({ className, fill = 'currentColor', size = 16 }: IconProps) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 16 16"
      xmlns="http://www.w3.org/2000/svg">
      <path
        style={{ fill }}
        d="M8 2C4.7 2 2 4.7 2 8s2.7 6 6 6 6-2.7 6-6-2.7-6-6-6zm-.5 5.5h.9v.9h-.9v-.9zm-3.8.2c-.1 0-.2-.1-.2-.2 0-.4.1-1.2.6-2S5.3 4.2 5.6 4c.2 0 .3 0 .3.1l1.3 2.3c0 .1 0 .2-.1.2-.1.2-.2.3-.3.5-.1.2-.2.4-.2.5 0 .1-.1.2-.2.2l-2.7-.1zM9.9 12c-.3.2-1.1.5-2 .5-.9 0-1.7-.3-2-.5-.1 0-.1-.2-.1-.3l1.3-2.3c0-.1.1-.1.2-.1.2.1.3.1.5.1s.4 0 .5-.1c.1 0 .2 0 .2.1l1.3 2.3c.2.2.2.3.1.3zm2.5-4.1L9.7 8c-.1 0-.2-.1-.2-.2 0-.2-.1-.4-.2-.5 0-.1-.2-.3-.3-.4-.1 0-.1-.1-.1-.2l1.3-2.3c.1-.1.2-.1.3-.1.3.2 1 .7 1.5 1.5s.6 1.6.6 2c0 0-.1.1-.2.1z"
      />
    </svg>
  );
}
