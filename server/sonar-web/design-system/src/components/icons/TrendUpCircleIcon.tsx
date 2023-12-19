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
import { themeColor, themeContrast } from '../../helpers';
import { CustomIcon, IconProps } from './Icon';

export function TrendUpCircleIcon(props: Readonly<IconProps>) {
  const theme = useTheme();

  const bgColor = themeColor('overviewCardErrorIcon')({ theme });
  const iconColor = themeContrast('overviewCardErrorIcon')({ theme });

  return (
    <CustomIcon height="36" viewBox="0 0 36 36" width="36" {...props}>
      <circle cx="18" cy="18" fill={bgColor} r="18" />
      <g clipPath="url(#clip0_2971_11471)">
        <path
          d="M20.8955 12.253C20.7186 12.1492 20.5107 12.1169 20.3175 12.1633C20.1242 12.2096 19.9615 12.3308 19.8652 12.5001C19.7688 12.6695 19.7467 12.8732 19.8036 13.0663C19.8605 13.2595 19.9919 13.4263 20.1688 13.5301L21.7029 14.4305L16.8273 15.1807C16.6524 15.2084 16.4961 15.2967 16.386 15.43C16.276 15.5633 16.2193 15.7331 16.2258 15.9094L16.4151 19.6577L11.2365 20.409C11.1385 20.4231 11.0453 20.4562 10.9621 20.5065C10.879 20.5568 10.8076 20.6233 10.752 20.7022C10.6963 20.7811 10.6576 20.8708 10.6379 20.9662C10.6183 21.0617 10.6181 21.161 10.6374 21.2584C10.6567 21.3559 10.6952 21.4496 10.7505 21.5343C10.8059 21.619 10.877 21.6929 10.96 21.7518C11.0429 21.8108 11.136 21.8537 11.2339 21.8779C11.3318 21.9022 11.4327 21.9075 11.5306 21.8934L17.3493 21.0493C17.5267 21.0238 17.6858 20.9361 17.798 20.802C17.9101 20.668 17.968 20.4964 17.9612 20.3181L17.8022 16.5791L22.3971 15.8686L21.3833 17.6502C21.287 17.8195 21.2648 18.0232 21.3218 18.2163C21.3787 18.4095 21.5101 18.5763 21.687 18.6802C21.8639 18.784 22.0718 18.8163 22.265 18.7699C22.4583 18.7236 22.6209 18.6024 22.7173 18.4331L24.5341 15.2403C24.5768 15.1605 24.6052 15.0735 24.6182 14.983L24.6157 14.8623C24.6171 14.8187 24.6153 14.7749 24.6102 14.7313C24.5971 14.6769 24.576 14.6244 24.5477 14.5753C24.5303 14.5366 24.51 14.4991 24.487 14.4631C24.4598 14.4263 24.4278 14.3931 24.3918 14.3646C24.3477 14.3042 24.293 14.2519 24.2305 14.2103L20.8955 12.253Z"
          fill={iconColor}
        />
      </g>
      <defs>
        <clipPath id="clip0_2971_11471">
          <rect fill="white" height="18" transform="translate(9 9)" width="18" />
        </clipPath>
      </defs>
    </CustomIcon>
  );
}
