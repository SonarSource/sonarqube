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

export function SnoozeCircleIcon(props: Readonly<IconProps>) {
  const theme = useTheme();

  const bgColor = themeColor('overviewCardWarningIcon')({ theme });
  const iconColor = themeContrast('overviewCardWarningIcon')({ theme });

  return (
    <CustomIcon height="36" viewBox="0 0 36 36" width="36" {...props}>
      <circle cx="18" cy="18" fill={bgColor} r="18" />
      <path
        d="M16.5319 17.2149H18.4624L15.7318 20.2936C15.3281 20.7536 15.6658 21.4613 16.2897 21.4613H19.4681C19.8718 21.4613 20.2021 21.1428 20.2021 20.7536C20.2021 20.3643 19.8718 20.0458 19.4681 20.0458H17.5376L20.2682 16.9672C20.6719 16.5072 20.3342 15.7994 19.7103 15.7994H16.5319C16.1282 15.7994 15.7979 16.1179 15.7979 16.5072C15.7979 16.8964 16.1282 17.2149 16.5319 17.2149ZM24.8265 13.9735C24.5696 14.2707 24.1071 14.3132 23.7915 14.0655L21.538 12.2537C21.2297 11.9989 21.1857 11.553 21.4499 11.2558C21.7069 10.9585 22.1693 10.9161 22.4849 11.1638L24.7384 12.9756C25.0467 13.2304 25.0907 13.6762 24.8265 13.9735ZM11.1735 13.9735C11.4304 14.2778 11.8929 14.3132 12.2012 14.0655L14.4546 12.2537C14.7703 11.9989 14.8143 11.553 14.5501 11.2558C14.2931 10.9514 13.8307 10.9161 13.5224 11.1638L11.2616 12.9756C10.9533 13.2304 10.9093 13.6762 11.1735 13.9735ZM18 13.6762C20.8334 13.6762 23.1382 15.8985 23.1382 18.6304C23.1382 21.3622 20.8334 23.5845 18 23.5845C15.1666 23.5845 12.8618 21.3622 12.8618 18.6304C12.8618 15.8985 15.1666 13.6762 18 13.6762ZM18 12.2608C14.3519 12.2608 11.3937 15.1129 11.3937 18.6304C11.3937 22.1478 14.3519 25 18 25C21.6481 25 24.6063 22.1478 24.6063 18.6304C24.6063 15.1129 21.6481 12.2608 18 12.2608Z"
        fill={iconColor}
      />
    </CustomIcon>
  );
}
