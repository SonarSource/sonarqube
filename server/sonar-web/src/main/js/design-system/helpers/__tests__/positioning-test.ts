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

import { PopupPlacement, popupPositioning } from '../positioning';

const toggleRect = {
  getBoundingClientRect: jest.fn().mockReturnValue({
    left: 400,
    top: 200,
    width: 50,
    height: 20,
  }),
} as Element & { getBoundingClientRect: jest.Mock };

const popupRect = {
  getBoundingClientRect: jest.fn().mockReturnValue({
    width: 200,
    height: 100,
  }),
} as Element & { getBoundingClientRect: jest.Mock };

beforeAll(() => {
  Object.defineProperties(document.documentElement, {
    clientWidth: {
      configurable: true,
      value: 1000,
    },
    clientHeight: {
      configurable: true,
      value: 1000,
    },
  });
});

it('should calculate positioning based on placement', () => {
  const fixes = { leftFix: 0, topFix: 0 };
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Bottom)).toMatchObject({
    left: 325,
    top: 220,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.BottomLeft)).toMatchObject({
    left: 400,
    top: 220,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.BottomRight)).toMatchObject({
    left: 250,
    top: 220,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Top)).toMatchObject({
    left: 325,
    top: 100,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.TopLeft)).toMatchObject({
    left: 400,
    top: 100,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.TopRight)).toMatchObject({
    left: 250,
    top: 100,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Left)).toMatchObject({
    left: 200,
    top: 160,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.LeftBottom)).toMatchObject({
    left: 200,
    top: 120,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.LeftTop)).toMatchObject({
    left: 200,
    top: 200,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Right)).toMatchObject({
    left: 450,
    top: 160,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.RightBottom)).toMatchObject({
    left: 450,
    top: 120,
    ...fixes,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.RightTop)).toMatchObject({
    left: 450,
    top: 200,
    ...fixes,
  });
});

it('should position the element in the boundaries of the screen', () => {
  toggleRect.getBoundingClientRect.mockReturnValueOnce({
    left: 0,
    top: 850,
    width: 50,
    height: 50,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Bottom)).toMatchObject({
    left: 4,
    leftFix: 79,
    top: 896,
    topFix: -4,
  });
  toggleRect.getBoundingClientRect.mockReturnValueOnce({
    left: 900,
    top: 0,
    width: 50,
    height: 50,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Top)).toMatchObject({
    left: 796,
    leftFix: -29,
    top: 4,
    topFix: 104,
  });
});

it('should position the element outside the boundaries of the screen when the toggle is outside', () => {
  toggleRect.getBoundingClientRect.mockReturnValueOnce({
    left: -100,
    top: 1100,
    width: 50,
    height: 50,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Bottom)).toMatchObject({
    left: -75,
    leftFix: 100,
    top: 1025,
    topFix: -125,
  });
  toggleRect.getBoundingClientRect.mockReturnValueOnce({
    left: 1500,
    top: -200,
    width: 50,
    height: 50,
  });
  expect(popupPositioning(toggleRect, popupRect, PopupPlacement.Top)).toMatchObject({
    left: 1325,
    leftFix: -100,
    top: -175,
    topFix: 125,
  });
});
