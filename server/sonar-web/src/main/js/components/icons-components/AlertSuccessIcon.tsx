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

export default function AlertSuccessIcon({ className, fill = theme.green, size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M12.607 6.554q0-0.25-0.161-0.411l-0.813-0.804q-0.17-0.17-0.402-0.17t-0.402 0.17l-3.643 3.634-2.018-2.018q-0.17-0.17-0.402-0.17t-0.402 0.17l-0.813 0.804q-0.161 0.161-0.161 0.411 0 0.241 0.161 0.402l3.232 3.232q0.17 0.17 0.402 0.17 0.241 0 0.411-0.17l4.848-4.848q0.161-0.161 0.161-0.402zM14.857 8q0 1.866-0.92 3.442t-2.496 2.496-3.442 0.92-3.442-0.92-2.496-2.496-0.92-3.442 0.92-3.442 2.496-2.496 3.442-0.92 3.442 0.92 2.496 2.496 0.92 3.442z"
        style={{ fill }}
      />
    </Icon>
  );
}
