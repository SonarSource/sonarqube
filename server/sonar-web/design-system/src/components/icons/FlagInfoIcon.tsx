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

export function FlagInfoIcon({ fill = 'iconInfo', ...iconProps }: IconProps) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      <path
        d="M14 8A6 6 0 1 1 2 8a6 6 0 0 1 12 0Zm-5 3a1 1 0 1 1-2 0V8a1 1 0 0 1 2 0v3ZM8 6a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z"
        style={{ fill: themeColor(fill)({ theme }) }}
      />
    </CustomIcon>
  );
}
