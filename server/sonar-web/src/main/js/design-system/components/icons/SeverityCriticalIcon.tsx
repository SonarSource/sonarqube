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
import { themeColor, themeContrast } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

export function SeverityCriticalIcon({ fill = 'iconSeverityMajor', ...iconProps }: IconProps) {
  const theme = useTheme();

  return (
    <CustomIcon {...iconProps}>
      <circle cx="8" cy="8" fill={themeColor(fill)({ theme })} r="7" />
      <path
        d="M5 6.67113C5 6.56986 5.05583 6.47727 5.14421 6.43198L7.88334 5.02823C7.95678 4.99059 8.04322 4.99059 8.11666 5.02823L10.8558 6.43198C10.9442 6.47727 11 6.56986 11 6.67113V10.7324C11 10.9191 10.8181 11.0483 10.6475 10.9827L8.0916 10.0003C8.03254 9.97763 7.96746 9.97763 7.9084 10.0003L5.35247 10.9827C5.18192 11.0483 5 10.9191 5 10.7324V6.67113Z"
        fill={themeContrast(fill)({ theme })}
      />
    </CustomIcon>
  );
}
