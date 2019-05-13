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

import { mount } from 'enzyme';
import * as React from 'react';
import { PageTracker } from '../PageTracker';
import { mockLocation, mockRouter } from '../../../helpers/testMocks';

jest.useFakeTimers();

beforeEach(() => {
  jest.clearAllTimers();

  (window as any).dataLayer = [];

  document.getElementsByTagName = jest.fn().mockImplementation(() => {
    return [document.body];
  });
});

it('should not trigger if no analytics system is given', () => {
  shallowRender();

  expect(setTimeout).not.toHaveBeenCalled();
});

it('should work for Google Analytics', () => {
  const wrapper = shallowRender({ trackingIdGA: '123' });
  const instance = wrapper.instance();
  instance.trackPage();

  expect(setTimeout).toHaveBeenCalledWith(expect.any(Function), 500);
});

it('should work for Google Tag Manager', () => {
  const wrapper = shallowRender({ trackingIdGTM: '123' });
  const instance = wrapper.instance();
  const dataLayer = (window as any).dataLayer;

  expect(dataLayer).toHaveLength(1);

  instance.trackPage();

  expect(setTimeout).toHaveBeenCalledWith(expect.any(Function), 500);

  jest.runAllTimers();

  expect(dataLayer).toHaveLength(2);
});

function shallowRender(props: Partial<PageTracker['props']> = {}) {
  return mount<PageTracker>(
    <PageTracker
      location={mockLocation()}
      params={{}}
      router={mockRouter()}
      routes={[]}
      {...props}
    />
  );
}
