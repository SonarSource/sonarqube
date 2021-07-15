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
import { css, ThemeContext as EmotionThemeContext } from '@emotion/core';
import emotionStyled, { CreateStyled } from '@emotion/styled';
import {
  ThemeProvider as EmotionThemeProvider,
  ThemeProviderProps,
  useTheme as emotionUseTheme,
  withTheme,
} from 'emotion-theming';
import * as React from 'react';

export interface Theme {
  colors: T.Dict<string>;
  sizes: T.Dict<string>;
  rawSizes: T.Dict<number>;
  fonts: T.Dict<string>;
  zIndexes: T.Dict<string>;
  others: T.Dict<string>;
}

export interface ThemedProps {
  theme: Theme;
}

const ThemeContext = EmotionThemeContext as React.Context<Theme>;

export const styled = emotionStyled as CreateStyled<Theme>;
export const ThemeConsumer = ThemeContext.Consumer;
export const ThemeProvider = EmotionThemeProvider as React.ProviderExoticComponent<
  ThemeProviderProps<Theme>
>;
export const useTheme = emotionUseTheme as () => Theme;

export function themeGet(type: keyof Theme, name: string | number) {
  return function ({ theme }: Partial<ThemedProps>) {
    return theme?.[type][name];
  };
}
export function themeColor(name: keyof Theme['colors']) {
  return themeGet('colors', name);
}
export function themeSize(name: keyof Theme['sizes']) {
  return themeGet('sizes', name);
}

export { css, withTheme };
export default ThemeContext;
