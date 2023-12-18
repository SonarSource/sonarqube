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

export function HighImpactCircleIcon(props: Readonly<IconProps>) {
  const theme = useTheme();

  const bgColor = themeColor('overviewCardErrorIcon')({
    theme,
  });
  const iconColor = themeContrast('overviewCardErrorIcon')({
    theme,
  });

  return (
    <CustomIcon height="36" viewBox="0 0 36 36" width="36" {...props}>
      <circle cx="18" cy="18" fill={bgColor} r="18" />
      <path
        clipRule="evenodd"
        d="M17.875 25.75C22.2242 25.75 25.75 22.2242 25.75 17.875C25.75 13.5258 22.2242 10 17.875 10C13.5258 10 10 13.5258 10 17.875C10 22.2242 13.5258 25.75 17.875 25.75ZM14.6622 16.111C14.5628 16.1619 14.5 16.2661 14.5 16.38V20.9489C14.5 21.1589 14.7047 21.3043 14.8965 21.2306L17.772 20.1254C17.8384 20.0998 17.9116 20.0998 17.978 20.1254L20.8535 21.2306C21.0453 21.3043 21.25 21.1589 21.25 20.9489V16.38C21.25 16.2661 21.1872 16.1619 21.0878 16.111L18.0062 14.5318C17.9236 14.4894 17.8264 14.4894 17.7438 14.5318L14.6622 16.111Z"
        fill={iconColor}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
