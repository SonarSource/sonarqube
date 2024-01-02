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
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 14C10.866 14 14 10.866 14 7C14 3.13401 10.866 0 7 0C3.13401 0 0 3.13401 0 7C0 10.866 3.13401 14 7 14ZM3.7019 6.46256L6.46967 9.23033C6.76256 9.52322 7.23744 9.52322 7.53033 9.23033L10.2981 6.46256C10.591 6.16967 10.591 5.6948 10.2981 5.4019C10.0052 5.10901 9.53033 5.10901 9.23744 5.4019L7 7.63934L4.76256 5.4019C4.46967 5.10901 3.9948 5.10901 3.7019 5.4019C3.40901 5.6948 3.40901 6.16967 3.7019 6.46256Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
