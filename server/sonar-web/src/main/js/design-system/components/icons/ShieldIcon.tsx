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

export function ShieldIcon({ fill = 'currentColor', ...iconProps }: Readonly<IconProps>) {
  const theme = useTheme();

  return (
    <CustomIcon viewBox="0 0 20 20" width={20} height={20} {...iconProps}>
      <g fill={themeColor(fill)({ theme })}>
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M7.78736 9.00555C7.7045 9.06668 7.62013 9.12592 7.53432 9.1832C7.14912 9.44032 6.7348 9.658 6.29688 9.83071C6.7348 10.0034 7.14912 10.2211 7.53432 10.4782C7.62013 10.5355 7.7045 10.5947 7.78736 10.6559C8.31646 11.0462 8.78415 11.5139 9.17451 12.043C9.23564 12.1259 9.29488 12.2102 9.35216 12.2961C9.60928 12.6813 9.82696 13.0956 9.99967 13.5335C10.1724 13.0956 10.3901 12.6813 10.6472 12.2961C10.7045 12.2102 10.7637 12.1259 10.8248 12.043C11.2152 11.5139 11.6829 11.0462 12.212 10.6559C12.2949 10.5947 12.3792 10.5355 12.465 10.4782C12.8502 10.2211 13.2646 10.0034 13.7025 9.83071C13.2646 9.658 12.8502 9.44032 12.465 9.1832C12.3792 9.12592 12.2949 9.06668 12.212 9.00555C11.6829 8.61519 11.2152 8.14751 10.8248 7.6184C10.7637 7.53555 10.7045 7.45118 10.6472 7.36537C10.3901 6.98017 10.1724 6.56585 9.99967 6.12793C9.82696 6.56585 9.60928 6.98017 9.35216 7.36537C9.29488 7.45118 9.23564 7.53555 9.17451 7.61841C8.78415 8.14751 8.31646 8.61519 7.78736 9.00555ZM8.98407 9.83071C9.35249 10.1379 9.69243 10.4779 9.99968 10.8463C10.3069 10.4779 10.6469 10.1379 11.0153 9.83071C10.6469 9.52347 10.3069 9.18353 9.99967 8.81511C9.69243 9.18353 9.35249 9.52347 8.98407 9.83071Z"
          fill={themeColor(fill)({ theme })}
        />
        <path
          d="M9.99973 18.1777C8.06916 17.6916 6.47539 16.5839 5.21844 14.8548C3.96148 13.1256 3.33301 11.2055 3.33301 9.09434V4.01099L9.99973 1.51099L16.6664 4.01099V9.09434C16.6664 11.2055 16.038 13.1256 14.781 14.8548C13.5241 16.5839 11.9303 17.6916 9.99973 18.1777ZM9.99973 16.4277C11.4442 15.9694 12.6386 15.0527 13.5831 13.6777C14.5275 12.3027 14.9998 10.7749 14.9998 9.09434V5.15683L9.99973 3.28182L4.99969 5.15683V9.09434C4.99969 10.7749 5.47191 12.3027 6.41636 13.6777C7.36082 15.0527 8.55527 15.9694 9.99973 16.4277Z"
          fill={themeColor(fill)({ theme })}
        />
      </g>
    </CustomIcon>
  );
}
