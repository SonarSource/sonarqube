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
import ThemeContext from '../../components/theme';
import Initializer, { DEFAULT_LOCALE } from '../../helpers/init';
import testTheme from './testTheme';

Initializer.setLocale(DEFAULT_LOCALE).setMessages({}).setUrlContext('');

// Hack : override the default value of the context used for theme by emotion
// This allows tests to get the theme value without specifiying a theme provider
ThemeContext['_currentValue'] = testTheme;
ThemeContext['_currentValue2'] = testTheme;
