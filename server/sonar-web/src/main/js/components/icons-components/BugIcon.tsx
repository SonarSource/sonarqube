/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Icon, { IconProps } from './Icon';

export default function BugIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M11 9h1.3l.5.8.8-.5-.8-1.3H11v-.3l2-2.3V3h-1v2l-1 1.2V5c-.1-.8-.7-1.5-1.4-1.9L11 1.8l-.7-.7-1.8 1.6-1.8-1.6-.7.7 1.5 1.3C6.7 3.5 6.1 4.2 6 5v1.1L5 5V3H4v2.3l2 2.3V8H4.2l-.7 1.2.8.5.4-.7H6v.3l-2 1.9V14h1v-2.4l1-1C6 12 7.1 13 8.4 13h.8c.7 0 1.4-.3 1.8-.9.3-.4.3-.9.2-1.4l.9.9V14h1v-2.8l-2-1.9V9zm-2 2H8V6h1v5z"
        style={{ fill }}
      />
    </Icon>
  );
}
