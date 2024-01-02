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

export function FlagSuccessIcon({ fill = 'iconSuccess', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        d="M8 14A6 6 0 1 0 8 2a6 6 0 0 0 0 12Zm3.207-6.793a1 1 0 0 0-1.414-1.414L7 8.586 5.707 7.293a1 1 0 0 0-1.414 1.414l2 2a1 1 0 0 0 1.414 0l3.5-3.5Z"
        style={{ fill: themeColor(fill)({ theme }) }}
      />
    </CustomIcon>
  );
}
