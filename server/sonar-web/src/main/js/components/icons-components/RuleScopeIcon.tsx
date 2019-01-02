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

export default function RuleScopeIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M8 3.071c2.724 0 4.929 2.204 4.929 4.929s-2.204 4.929-4.929 4.929c-2.724 0-4.929-2.204-4.929-4.929s2.204-4.929 4.929-4.929zM8 1.357c-3.669 0-6.643 2.974-6.643 6.643s2.974 6.643 6.643 6.643 6.643-2.974 6.643-6.643-2.974-6.643-6.643-6.643zM8 6.286c0.945 0 1.714 0.769 1.714 1.714s-0.769 1.714-1.714 1.714-1.714-0.769-1.714-1.714 0.769-1.714 1.714-1.714zM8 4.571c-1.893 0-3.429 1.535-3.429 3.429s1.535 3.429 3.429 3.429 3.429-1.535 3.429-3.429-1.535-3.429-3.429-3.429z"
        style={{ fill }}
      />
    </Icon>
  );
}
