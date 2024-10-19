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

export function SoftwareImpactSeverityMediumIcon({
  disabled,
  ...iconProps
}: IconProps & { disabled?: boolean }) {
  const theme = useTheme();
  const color = disabled
    ? 'iconSoftwareImpactSeverityDisabled'
    : 'iconSoftwareImpactSeverityMedium';

  return (
    <CustomIcon viewBox="0 0 14 14" {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 13.375C10.5208 13.375 13.375 10.5208 13.375 7C13.375 3.47918 10.5208 0.625 7 0.625C3.47918 0.625 0.625 3.47918 0.625 7C0.625 10.5208 3.47918 13.375 7 13.375ZM10.051 7.83547L7.53033 5.31482C7.23744 5.02193 6.76256 5.02193 6.46967 5.31482L3.94903 7.83547C3.65613 8.12836 3.65613 8.60324 3.94903 8.89613C4.24192 9.18902 4.71679 9.18902 5.00969 8.89613L7 6.90581L8.99031 8.89613C9.28321 9.18902 9.75808 9.18902 10.051 8.89613C10.3439 8.60324 10.3439 8.12836 10.051 7.83547Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
