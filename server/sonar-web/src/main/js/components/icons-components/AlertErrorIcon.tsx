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
import * as theme from '../../app/theme';

export default function AlertErrorIcon({ className, fill = theme.red, size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M11.402 10.018q0-0.232-0.17-0.402l-1.616-1.616 1.616-1.616q0.17-0.17 0.17-0.402 0-0.241-0.17-0.411l-0.804-0.804q-0.17-0.17-0.411-0.17-0.232 0-0.402 0.17l-1.616 1.616-1.616-1.616q-0.17-0.17-0.402-0.17-0.241 0-0.411 0.17l-0.804 0.804q-0.17 0.17-0.17 0.411 0 0.232 0.17 0.402l1.616 1.616-1.616 1.616q-0.17 0.17-0.17 0.402 0 0.241 0.17 0.411l0.804 0.804q0.17 0.17 0.411 0.17 0.232 0 0.402-0.17l1.616-1.616 1.616 1.616q0.17 0.17 0.402 0.17 0.241 0 0.411-0.17l0.804-0.804q0.17-0.17 0.17-0.411zM14.857 8q0 1.866-0.92 3.442t-2.496 2.496-3.442 0.92-3.442-0.92-2.496-2.496-0.92-3.442 0.92-3.442 2.496-2.496 3.442-0.92 3.442 0.92 2.496 2.496 0.92 3.442z"
        style={{ fill }}
      />
    </Icon>
  );
}
