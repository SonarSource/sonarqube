/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as React from 'react';
import Icon, { IconProps } from './Icon';

interface Props extends IconProps {
  animated?: boolean;
  inverseDirection?: boolean;
}

export default function ArrowIcon({
  animated = false,
  fill = 'currentColor',
  inverseDirection = false,
  ...iconProps
}: Props) {
  const style: React.CSSProperties = {};
  if (inverseDirection) {
    style.transform = 'scaleX(-1)';
  }

  if (animated) {
    style.transition = 'transform 0.2s';
  }
  return (
    <Icon style={style} {...iconProps}>
      <path
        d="M13.99 6.867l.668.005H4.99l3.04-3.046a.79.79 0 00.23-.561.789.789 0 00-.23-.56l-.473-.474A.784.784 0 006.998 2a.784.784 0 00-.558.23L1.23 7.44A.783.783 0 001 8c0 .212.081.41.23.56l5.21 5.21c.149.148.347.23.558.23.212 0 .41-.082.559-.23l.472-.473a.782.782 0 000-1.106L4.956 9.128H14a.819.819 0 00.801-.81v-.67c0-.435-.376-.78-.812-.78z"
        fill={fill}
      />
    </Icon>
  );
}
