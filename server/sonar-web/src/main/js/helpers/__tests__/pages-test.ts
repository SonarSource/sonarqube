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
import {
  addNoFooterPageClass,
  addSideBarClass,
  addWhitePageClass,
  removeNoFooterPageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../pages';

describe('class adders', () => {
  it.each([
    [addSideBarClass, 'sidebar-page'],
    [addNoFooterPageClass, 'no-footer-page'],
    [addWhitePageClass, 'white-page'],
  ])('%s should add the class', (fct, cls) => {
    const toggle = jest.spyOn(document.body.classList, 'toggle');
    fct();
    expect(toggle).toHaveBeenCalledWith(cls, true);
  });
});

describe('class removers', () => {
  it.each([
    [removeSideBarClass, 'sidebar-page'],
    [removeNoFooterPageClass, 'no-footer-page'],
    [removeWhitePageClass, 'white-page'],
  ])('%s should add the class', (fct, cls) => {
    const toggle = jest.spyOn(document.body.classList, 'toggle');
    fct();
    expect(toggle).toHaveBeenCalledWith(cls, false);
  });
});
