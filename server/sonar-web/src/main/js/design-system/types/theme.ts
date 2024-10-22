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

import { lightTheme } from '../theme';

export type InputSizeKeys = 'small' | 'medium' | 'large' | 'full' | 'auto';

type LightTheme = typeof lightTheme;
type ThemeColor = string | number[];
export interface Theme extends Omit<LightTheme, 'colors' | 'contrasts'> {
  colors: {
    [key in keyof LightTheme['colors']]: ThemeColor;
  };
  contrasts: {
    [key in keyof LightTheme['colors'] & keyof LightTheme['contrasts']]: ThemeColor;
  };
}

export type ThemeColors = keyof Theme['colors'];
export type ThemeContrasts = keyof Theme['contrasts'];

type RGBColor = `rgb(${number},${number},${number})`;
type RGBAColor = `rgba(${number},${number},${number},${number})`;
type CSSCustomProp = `var(--${string})`;
export type CSSColor = CSSCustomProp | RGBColor | RGBAColor;

export interface ThemedProps {
  theme: Theme;
}
