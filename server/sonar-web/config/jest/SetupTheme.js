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

import { ThemeContext } from '@emotion/react';
// WARNING! Using '~design-system' below would break tests in the src/main/js/design-system folder!
import { lightTheme } from '../../src/main/js/design-system/theme/light';

// Hack : override the default value of the context used for theme by emotion
// This allows tests to get the theme value without specifiying a theme provider
ThemeContext['_currentValue'] = lightTheme;
ThemeContext['_currentValue2'] = lightTheme;
