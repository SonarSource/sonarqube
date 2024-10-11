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
import { themeColor } from '../../helpers';
import { CustomIcon, IconProps } from './Icon';

export function SoftwareImpactSeverityHighIcon({
  disabled,
  ...iconProps
}: IconProps & { disabled?: boolean }) {
  const theme = useTheme();

  const color = disabled
    ? 'var(--echoes-color-icon-disabled)'
    : themeColor('iconSoftwareImpactSeverityHigh')({ theme });

  return (
    <CustomIcon viewBox="0 0 14 14" {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 13.375C10.5208 13.375 13.375 10.5208 13.375 7C13.375 3.47918 10.5208 0.625 7 0.625C3.47918 0.625 0.625 3.47918 0.625 7C0.625 10.5208 3.47918 13.375 7 13.375ZM4.3983 5.57213C4.31781 5.61338 4.26697 5.6977 4.26697 5.78993V9.48856C4.26697 9.65858 4.43265 9.77626 4.58796 9.71657L6.91569 8.82188C6.96948 8.80121 7.02875 8.80121 7.08253 8.82188L9.41026 9.71657C9.56557 9.77626 9.73125 9.65858 9.73125 9.48856V5.78993C9.73125 5.6977 9.68041 5.61338 9.59992 5.57213L7.10536 4.29371C7.03847 4.25944 6.95975 4.25944 6.89286 4.29371L4.3983 5.57213Z"
        fill={color}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
