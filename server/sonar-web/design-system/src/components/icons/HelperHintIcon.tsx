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
import { Fragment } from 'react';
import { themeColor, themeContrast } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

type Props = IconProps & {
  raised?: boolean;
};

export function HelperHintIcon({ raised, ...iconProps }: Props) {
  const theme = useTheme();
  return (
    <CustomIcon {...iconProps}>
      {raised ? (
        // eslint-disable-next-line react/jsx-fragments
        <Fragment>
          <circle cx="8" cy="8" fill={themeColor('iconHelperHintRaised')({ theme })} r="7" />
          <path
            d="M6.82812 10.2301H8.44318V10.0852C8.4517 9.25426 8.75 8.86648 9.4233 8.46165C10.2202 7.98864 10.7401 7.36222 10.7401 6.3608C10.7401 4.86932 9.53835 4 7.84659 4C6.29972 4 5.03835 4.80966 5 6.5142H6.73864C6.7642 5.8196 7.27983 5.44886 7.83807 5.44886C8.41335 5.44886 8.87784 5.83239 8.87784 6.42472C8.87784 6.98295 8.47301 7.35369 7.94886 7.68608C7.23295 8.13778 6.83239 8.59375 6.82812 10.0852V10.2301ZM7.66761 12.9574C8.21307 12.9574 8.68608 12.5014 8.69034 11.9347C8.68608 11.3764 8.21307 10.9205 7.66761 10.9205C7.10511 10.9205 6.64063 11.3764 6.64489 11.9347C6.64063 12.5014 7.10511 12.9574 7.66761 12.9574Z"
            fill={themeContrast('iconHelperHintRaised')({ theme })}
          />
        </Fragment>
      ) : (
        // eslint-disable-next-line react/jsx-fragments
        <Fragment>
          <circle cx="8" cy="8" fill={themeColor('iconHelperHint')({ theme })} r="7" />
          <path
            d="M6.82812 10.2301h1.61506v-.1449c.00852-.83094.30682-1.21872.98012-1.62355.7969-.47301 1.3168-1.09943 1.3168-2.10085C10.7401 4.86932 9.53835 4 7.84659 4 6.29972 4 5.03835 4.80966 5 6.5142h1.73864c.02556-.6946.54119-1.06534 1.09943-1.06534.57528 0 1.03977.38353 1.03977.97586 0 .55823-.40483.92897-.92898 1.26136-.71591.4517-1.11647.90767-1.12074 2.39912v.1449Zm.83949 2.7273c.54546 0 1.01847-.456 1.02273-1.0227-.00426-.5583-.47727-1.0142-1.02273-1.0142-.5625 0-1.02698.4559-1.02272 1.0142-.00426.5667.46022 1.0227 1.02272 1.0227Z"
            fill={themeContrast('iconHelperHint')({ theme })}
          />
        </Fragment>
      )}
    </CustomIcon>
  );
}
