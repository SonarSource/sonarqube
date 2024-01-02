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
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 14C10.866 14 14 10.866 14 7C14 3.13401 10.866 0 7 0C3.13401 0 0 3.13401 0 7C0 10.866 3.13401 14 7 14ZM10.2981 7.96967L7.53033 5.2019C7.23744 4.90901 6.76256 4.90901 6.46967 5.2019L3.7019 7.96967C3.40901 8.26256 3.40901 8.73744 3.7019 9.03033C3.9948 9.32322 4.46967 9.32322 4.76256 9.03033L7 6.79289L9.23744 9.03033C9.53033 9.32322 10.0052 9.32322 10.2981 9.03033C10.591 8.73744 10.591 8.26256 10.2981 7.96967Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
