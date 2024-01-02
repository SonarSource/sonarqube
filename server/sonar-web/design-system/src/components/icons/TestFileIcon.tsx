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

export function TestFileIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });

  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M3.75 1.5a.25.25 0 0 0-.25.25v11.5c0 .138.112.25.25.25h8.5a.25.25 0 0 0 .25-.25V6H9.75A1.75 1.75 0 0 1 8 4.25V1.5H3.75Zm5.75.56v2.19c0 .138.112.25.25.25h2.19L9.5 2.06ZM2 1.75C2 .784 2.784 0 3.75 0h5.086c.464 0 .909.184 1.237.513l3.414 3.414c.329.328.513.773.513 1.237v8.086A1.75 1.75 0 0 1 12.25 15h-8.5A1.75 1.75 0 0 1 2 13.25V1.75Z"
        fill={fillColor}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M8.605 11.528v-1.514l-.016-1.486.016-1.058 2.544 2.544-2.544 2.544v-1.03ZM7.545 8.5v1.514L7.56 11.5l-.017 1.058L5 10.014 7.544 7.47V8.5Z"
        fill={fillColor}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
