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

export function SoftwareImpactSeverityLowIcon({
  disabled,
  ...iconProps
}: IconProps & { disabled?: boolean }) {
  const theme = useTheme();
  const color = disabled ? 'iconSoftwareImpactSeverityDisabled' : 'iconSoftwareImpactSeverityLow';

  return (
    <CustomIcon viewBox="0 0 14 14" {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 13.375C10.5208 13.375 13.375 10.5208 13.375 7C13.375 3.47918 10.5208 0.625 7 0.625C3.47918 0.625 0.625 3.47918 0.625 7C0.625 10.5208 3.47918 13.375 7 13.375ZM3.94899 6.55761L6.46964 9.07825C6.76253 9.37115 7.2374 9.37115 7.5303 9.07825L10.0509 6.55761C10.3438 6.26472 10.3438 5.78984 10.0509 5.49695C9.75805 5.20406 9.28317 5.20406 8.99028 5.49695L6.99997 7.48727L5.00965 5.49695C4.71676 5.20406 4.24188 5.20406 3.94899 5.49695C3.6561 5.78984 3.6561 6.26472 3.94899 6.55761Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
