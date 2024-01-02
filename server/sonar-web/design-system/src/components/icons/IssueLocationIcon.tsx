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

export function IssueLocationIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });

  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M8 11C8.79565 11 9.55871 10.6839 10.1213 10.1213C10.6839 9.55871 11 8.79565 11 8C11 7.20435 10.6839 6.44129 10.1213 5.87868C9.55871 5.31607 8.79565 5 8 5C7.20435 5 6.44129 5.31607 5.87868 5.87868C5.31607 6.44129 5 7.20435 5 8C5 8.79565 5.31607 9.55871 5.87868 10.1213C6.44129 10.6839 7.20435 11 8 11ZM8 7C7.73478 7 7.48043 7.10536 7.29289 7.29289C7.10536 7.48043 7 7.73478 7 8C7 8.26522 7.10536 8.51957 7.29289 8.70711C7.48043 8.89464 7.73478 9 8 9C8.26522 9 8.51957 8.89464 8.70711 8.70711C8.89464 8.51957 9 8.26522 9 8C9 7.73478 8.89464 7.48043 8.70711 7.29289C8.51957 7.10536 8.26522 7 8 7Z"
        fill={fillColor}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
