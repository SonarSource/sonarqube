/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  const color = disabled ? 'iconSoftwareImpactSeverityDisabled' : 'iconSoftwareImpactSeverityHigh';

  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M7 14C10.866 14 14 10.866 14 7C14 3.13401 10.866 0 7 0C3.13401 0 0 3.13401 0 7C0 10.866 3.13401 14 7 14ZM4.14421 5.43198C4.05583 5.47727 4 5.56986 4 5.67113V9.73238C4 9.91906 4.18192 10.0483 4.35247 9.98273L6.9084 9.00033C6.96746 8.97763 7.03254 8.97763 7.0916 9.00033L9.64753 9.98273C9.81808 10.0483 10 9.91906 10 9.73238V5.67113C10 5.56986 9.94417 5.47727 9.85579 5.43198L7.11666 4.02823C7.04322 3.99059 6.95678 3.99059 6.88334 4.02823L4.14421 5.43198Z"
        fill={themeColor(color)({ theme })}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
