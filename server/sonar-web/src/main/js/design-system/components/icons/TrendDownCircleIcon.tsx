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
import { themeColor, themeContrast } from '../../helpers';
import { CustomIcon, IconProps } from './Icon';

export function TrendDownCircleIcon(props: Readonly<IconProps>) {
  const theme = useTheme();

  const bgColor = themeColor('overviewCardSuccessIcon')({ theme });
  const iconColor = themeContrast('overviewCardSuccessIcon')({ theme });

  return (
    <CustomIcon height="36" viewBox="0 0 36 36" width="36" {...props}>
      <circle cx="18" cy="16" fill={bgColor} r="16" />
      <path
        d="M23.3203 20.3404C23.3658 20.3291 23.4095 20.3116 23.4503 20.2886C23.4845 20.2687 23.5158 20.2441 23.5433 20.2156C23.567 20.1872 23.5886 20.157 23.6078 20.1254C23.6375 20.0862 23.6614 20.0428 23.6785 19.9967L23.6922 19.9051C23.7046 19.8587 23.7097 19.8107 23.7072 19.7627L23.6579 19.6389C23.6794 19.5866 23.6912 19.5308 23.6927 19.4743L22.4586 16.3778C22.3931 16.2136 22.2651 16.0821 22.1026 16.0122C21.9402 15.9424 21.7567 15.9399 21.5924 16.0054C21.4282 16.0709 21.2967 16.1989 21.2268 16.3613C21.157 16.5238 21.1545 16.7073 21.22 16.8715L21.9185 18.6241L18.0143 17.3095L18.5396 13.9999C18.5644 13.8431 18.5326 13.6826 18.4497 13.5473C18.3668 13.4119 18.2383 13.3106 18.0874 13.2615L13.1375 11.6461C13.0541 11.6189 12.9662 11.6084 12.8788 11.6152C12.7914 11.622 12.7062 11.646 12.628 11.6858C12.5499 11.7256 12.4804 11.7805 12.4235 11.8472C12.3666 11.9139 12.3234 11.9912 12.2964 12.0746C12.2485 12.2228 12.254 12.3831 12.3119 12.5277C12.348 12.6188 12.4038 12.7007 12.4751 12.7678C12.5465 12.8348 12.6318 12.8853 12.7249 12.9157L17.1303 14.3534L16.599 17.6297C16.5746 17.7848 16.6057 17.9435 16.6868 18.0779C16.7679 18.2123 16.8939 18.3137 17.0425 18.3643L21.1545 19.7683L19.7302 20.336C19.5659 20.4015 19.4344 20.5295 19.3646 20.6919C19.2947 20.8544 19.2923 21.0379 19.3577 21.2021C19.4232 21.3664 19.5512 21.4979 19.7137 21.5677C19.8761 21.6376 20.0596 21.64 20.2238 21.5746L23.3203 20.3404Z"
        fill={iconColor}
      />
    </CustomIcon>
  );
}
