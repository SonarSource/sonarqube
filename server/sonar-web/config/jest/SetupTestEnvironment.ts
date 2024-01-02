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
const content = document.createElement('div');
content.id = 'content';
document.documentElement.appendChild(content);

const baseUrl = '';
(window as any).baseUrl = baseUrl;

Element.prototype.scrollIntoView = () => {};

jest.mock('../../src/main/js/helpers/l10n', () => ({
  ...jest.requireActual('../../src/main/js/helpers/l10n'),
  hasMessage: () => true,
  translate: (...keys: string[]) => keys.join('.'),
  translateWithParameters: (messageKey: string, ...parameters: Array<string | number>) =>
    [messageKey, ...parameters].join('.'),
}));
