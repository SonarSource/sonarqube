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
import { Theme, ThemeConsumer } from '../theme';

export interface IconProps extends React.AriaAttributes {
  className?: string;
  fill?: string;
  size?: number;
}

interface Props extends React.AriaAttributes {
  children: React.ReactNode;
  className?: string;
  size?: number;
  style?: React.CSSProperties;

  // try to avoid using these:
  width?: number;
  height?: number;
  viewBox?: string;
}

export default function Icon({
  children,
  className,
  size = 16,
  style,
  height = size,
  width = size,
  viewBox = '0 0 16 16',
  ...iconProps
}: Props) {
  return (
    <svg
      className={className}
      height={height}
      style={{
        fillRule: 'evenodd',
        clipRule: 'evenodd',
        strokeLinejoin: 'round',
        strokeMiterlimit: 1.41421,
        ...style,
      }}
      version="1.1"
      viewBox={viewBox}
      width={width}
      xmlnsXlink="http://www.w3.org/1999/xlink"
      xmlSpace="preserve"
      {...iconProps}>
      {children}
    </svg>
  );
}

interface ThemedProps extends Props {
  children: (themeContext: { theme: Theme }) => React.ReactNode;
}

export function ThemedIcon({ children, ...iconProps }: ThemedProps) {
  return (
    <ThemeConsumer>{(theme) => <Icon {...iconProps}>{children({ theme })}</Icon>}</ThemeConsumer>
  );
}
