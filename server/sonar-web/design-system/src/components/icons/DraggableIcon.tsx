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
import styled from '@emotion/styled';
import { themeColor, themeContrast } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

export function DraggableIcon({
  fill = 'currentColor',
  ...iconProps
}: IconProps & { x: number; y: number }) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });
  const innerFillColor = themeContrast(fill)({ theme });

  return (
    <StyledCustomIcon {...iconProps}>
      <circle cx="8" cy="8" fill={fillColor} r="8" />
      <rect fill={innerFillColor} height="7" width="1" x="6" y="5" />
      <rect fill={innerFillColor} height="7" width="1" x="9" y="5" />
    </StyledCustomIcon>
  );
}

const StyledCustomIcon = styled(CustomIcon)`
  cursor: ew-resize;
`;
