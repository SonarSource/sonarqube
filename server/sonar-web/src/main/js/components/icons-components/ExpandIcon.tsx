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

export default function ExpandIcon({ className, fill = 'currentColor', size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <path
        d="M7.898 9.25a.247.247 0 0 1-.078.18l-2.593 2.593 1.125 1.125a.48.48 0 0 1 .148.352.48.48 0 0 1-.148.352A.48.48 0 0 1 6 14H2.5a.48.48 0 0 1-.352-.148A.48.48 0 0 1 2 13.5V10a.48.48 0 0 1 .148-.352A.48.48 0 0 1 2.5 9.5a.48.48 0 0 1 .352.148l1.125 1.125L6.57 8.18a.247.247 0 0 1 .36 0l.89.89a.247.247 0 0 1 .078.18zM14 2.5V6a.48.48 0 0 1-.148.352.48.48 0 0 1-.352.148.48.48 0 0 1-.352-.148l-1.125-1.125L9.43 7.82a.247.247 0 0 1-.36 0l-.89-.89a.247.247 0 0 1 0-.36l2.593-2.593-1.125-1.125A.48.48 0 0 1 9.5 2.5a.48.48 0 0 1 .148-.352A.48.48 0 0 1 10 2h3.5a.48.48 0 0 1 .352.148A.48.48 0 0 1 14 2.5z"
        style={{ fill }}
      />
    </Icon>
  );
}
