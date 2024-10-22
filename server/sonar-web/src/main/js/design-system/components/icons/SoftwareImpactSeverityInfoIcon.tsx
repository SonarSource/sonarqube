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
import { IconInfo } from '@sonarsource/echoes-react';
import { IconProps } from './Icon';

const defaultIconSize = 15;

export function SoftwareImpactSeverityInfoIcon({
  disabled,
  ...iconProps
}: IconProps & { disabled?: boolean }) {
  const color = disabled ? 'echoes-color-icon-disabled' : 'echoes-color-icon-info';

  return <StyledIconInfo color={color} {...iconProps} />;
}

// Info icon is the only one that is imported from echoes, so we need to adjust its size
const StyledIconInfo = styled(IconInfo)`
  ${(props: IconProps & { disabled?: boolean }) => {
    let size = props.width ?? props.height;
    size = size ? size + 1 : defaultIconSize;

    return `
    font-size: ${size}px;
    margin-left: -0.5px;
    `;
  }};
`;
