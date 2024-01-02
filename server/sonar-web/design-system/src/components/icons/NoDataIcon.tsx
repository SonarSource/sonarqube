/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { CustomIcon, IconProps } from './Icon';

export function NoDataIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M16 8C16 12.4183 12.4183 16 8 16C5.5106 16 3.28676 14.863 1.81951 13.0799L15.4913 5.1865C15.8201 6.06172 16 7.00986 16 8ZM14.5574 3.41624L0.750565 11.3876C0.269025 10.3589 0 9.21089 0 8C0 3.58172 3.58172 0 8 0C10.7132 0 13.1109 1.35064 14.5574 3.41624Z"
        fill="#E1E6F3"
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
