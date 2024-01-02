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
import styled from '@emotion/styled';
import React from 'react';
import { OPACITY_20_PERCENT } from '../../helpers/constants';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { Button, ButtonProps } from './Button';

interface ThirdPartyProps extends Omit<ButtonProps, 'Icon'> {
  iconPath: string;
  name: string;
}

export function ThirdPartyButton({ children, iconPath, name, ...buttonProps }: ThirdPartyProps) {
  const size = 16;
  return (
    <ThirdPartyButtonStyled {...buttonProps}>
      <img alt={name} className="sw-mr-1" height={size} src={iconPath} width={size} />
      {children}
    </ThirdPartyButtonStyled>
  );
}

const ThirdPartyButtonStyled: React.FC<React.PropsWithChildren<ButtonProps>> = styled(Button)`
  --background: ${themeColor('thirdPartyButton')};
  --backgroundHover: ${themeColor('thirdPartyButtonHover')};
  --color: ${themeContrast('thirdPartyButton')};
  --focus: ${themeColor('thirdPartyButtonBorder', OPACITY_20_PERCENT)};
  --border: ${themeBorder('default', 'thirdPartyButtonBorder')};
`;
