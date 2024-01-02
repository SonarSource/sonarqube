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
import { themeColor } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import { DuplicationEnum, DuplicationLabel } from '../types/measures';
import { NoDataIcon } from './icons';

interface Props {
  rating?: DuplicationLabel;
  size?: 'xs' | 'sm' | 'md';
}

const SIZE_TO_PX_MAPPING = { xs: 16, sm: 24, md: 36 };

export function DuplicationsIndicator({ size = 'sm', rating }: Props) {
  const theme = useTheme();
  const sizePX = SIZE_TO_PX_MAPPING[size];

  if (rating === undefined) {
    return <NoDataIcon height={sizePX} width={sizePX} />;
  }

  const primaryColor = themeColor(`duplicationsIndicator.${rating}`)({ theme });
  const secondaryColor = themeColor('duplicationsIndicatorSecondary')({ theme });

  return (
    <RatingSVG
      primaryColor={primaryColor}
      rating={rating}
      secondaryColor={secondaryColor}
      size={sizePX}
    />
  );
}

interface SVGProps {
  primaryColor: string;
  rating: DuplicationLabel;
  secondaryColor: string;
  size: number;
}

function RatingSVG({ primaryColor, rating, secondaryColor, size }: SVGProps) {
  return (
    <svg height={size} role="img" viewBox="0 0 16 16" width={size}>
      <circle cx="8" cy="8" fill={primaryColor} r="2" />
      {isDefined(rating) &&
        {
          [DuplicationEnum.A]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14c3.3137 0 6-2.6863 6-6 0-3.31371-2.6863-6-6-6-3.31371 0-6 2.68629-6 6 0 3.3137 2.68629 6 6 6Zm0 2c4.4183 0 8-3.5817 8-8 0-4.41828-3.5817-8-8-8-4.41828 0-8 3.58172-8 8 0 4.4183 3.58172 8 8 8Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <circle cx="8" cy="8" fill={primaryColor} r="2" />
            </>
          ),
          [DuplicationEnum.B]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14c3.3137 0 6-2.6863 6-6 0-3.31371-2.6863-6-6-6-3.31371 0-6 2.68629-6 6 0 3.3137 2.68629 6 6 6Zm0 2c4.4183 0 8-3.5817 8-8 0-4.41828-3.5817-8-8-8-4.41828 0-8 3.58172-8 8 0 4.4183 3.58172 8 8 8Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <path
                d="M8 0c.81879 0 1.63272.125698 2.4134.372702L9.81002 2.27953A5.99976 5.99976 0 0 0 8 2V0Z"
                fill={primaryColor}
              />
            </>
          ),
          [DuplicationEnum.C]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14c3.3137 0 6-2.6863 6-6 0-3.31371-2.6863-6-6-6-3.31371 0-6 2.68629-6 6 0 3.3137 2.68629 6 6 6Zm0 2c4.4183 0 8-3.5817 8-8 0-4.41828-3.5817-8-8-8-4.41828 0-8 3.58172-8 8 0 4.4183 3.58172 8 8 8Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <path
                d="M8 0c1.89071 2e-8 3.7203.669649 5.1643 1.89017l-1.2911 1.52746C10.7902 2.50224 9.41803 2 8 2V0Z"
                fill={primaryColor}
              />
            </>
          ),
          [DuplicationEnum.D]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14C11.3137 14 14 11.3137 14 8C14 4.68629 11.3137 2 8 2C4.68629 2 2 4.68629 2 8C2 11.3137 4.68629 14 8 14ZM8 16C12.4183 16 16 12.4183 16 8C16 3.58172 12.4183 0 8 0C3.58172 0 0 3.58172 0 8C0 12.4183 3.58172 16 8 16Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <path
                d="M8 0a7.9999 7.9999 0 0 1 4.5815 1.44181 7.99949 7.99949 0 0 1 2.9301 3.80574l-1.8779.68811A6.00009 6.00009 0 0 0 8 2V0Z"
                fill={primaryColor}
              />
            </>
          ),
          [DuplicationEnum.E]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14C11.3137 14 14 11.3137 14 8C14 4.68629 11.3137 2 8 2C4.68629 2 2 4.68629 2 8C2 11.3137 4.68629 14 8 14ZM8 16C12.4183 16 16 12.4183 16 8C16 3.58172 12.4183 0 8 0C3.58172 0 0 3.58172 0 8C0 12.4183 3.58172 16 8 16Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <path
                d="M8 0a8 8 0 0 1 5.0686 1.81054 8.00033 8.00033 0 0 1 2.7744 4.61211l-1.9608.39434a5.99958 5.99958 0 0 0-2.0808-3.45908A5.99972 5.99972 0 0 0 8 2V0Z"
                fill={primaryColor}
              />
            </>
          ),
          [DuplicationEnum.F]: (
            <>
              <path
                clipRule="evenodd"
                d="M8 14C11.3137 14 14 11.3137 14 8C14 4.68629 11.3137 2 8 2C4.68629 2 2 4.68629 2 8C2 11.3137 4.68629 14 8 14ZM8 16C12.4183 16 16 12.4183 16 8C16 3.58172 12.4183 0 8 0C3.58172 0 0 3.58172 0 8C0 12.4183 3.58172 16 8 16Z"
                fill={secondaryColor}
                fillRule="evenodd"
              />
              <path
                d="M8 0a8.0002 8.0002 0 0 1 5.6569 13.6569l-1.4143-1.4143a5.9993 5.9993 0 0 0 1.3007-6.5387A5.9999 5.9999 0 0 0 8 2V0Z"
                fill={primaryColor}
              />
            </>
          ),
        }[rating]}
    </svg>
  );
}
