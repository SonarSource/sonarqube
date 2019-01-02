/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as colors from '../colors';

describe('#stringToColor', () => {
  it('should return a color for a text', () => {
    expect(colors.stringToColor('skywalker')).toBe('#97f047');
  });
});

describe('#isDarkColor', () => {
  it('should be dark', () => {
    expect(colors.isDarkColor('#000000')).toBeTruthy();
    expect(colors.isDarkColor('#222222')).toBeTruthy();
    expect(colors.isDarkColor('#000')).toBeTruthy();
  });
  it('should be light', () => {
    expect(colors.isDarkColor('#FFFFFF')).toBeFalsy();
    expect(colors.isDarkColor('#CDCDCD')).toBeFalsy();
    expect(colors.isDarkColor('#FFF')).toBeFalsy();
  });
});

describe('#getTextColor', () => {
  it('should return dark color', () => {
    expect(colors.getTextColor('#FFF', 'dark', 'light')).toBe('dark');
    expect(colors.getTextColor('#FFF')).toBe('#222');
  });
  it('should return light color', () => {
    expect(colors.getTextColor('#000', 'dark', 'light')).toBe('light');
    expect(colors.getTextColor('#000')).toBe('#fff');
  });
});
