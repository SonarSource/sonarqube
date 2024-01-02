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
import { useTheme } from '@emotion/react';
import { themeColor } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

export function FlagWarningIcon({ fill = 'iconWarning', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        d="M14.41 12.55a1 1 0 0 1-.893 1.45H2.625a1 1 0 0 1-.892-1.45L7.178 1.766a1 1 0 0 1 1.786 0l5.445 10.782ZM7 6a1 1 0 1 1 2 0v3a1 1 0 1 1-2 0V6Zm1 5a1 1 0 1 0 0 2 1 1 0 0 0 0-2Z"
        style={{ fill: themeColor(fill)({ theme }) }}
      />
    </CustomIcon>
  );
}
