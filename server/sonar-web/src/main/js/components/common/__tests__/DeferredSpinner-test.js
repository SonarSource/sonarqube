/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import { mount } from 'enzyme';
import DeferredSpinner from '../DeferredSpinner';

jest.useFakeTimers();

it('renders spinner after timeout', () => {
  const spinner = mount(<DeferredSpinner />);
  expect(spinner).toMatchSnapshot();
  jest.runAllTimers();
  spinner.update();
  expect(spinner).toMatchSnapshot();
});

it('add custom className', () => {
  const spinner = mount(<DeferredSpinner className="foo" />);
  jest.runAllTimers();
  spinner.update();
  expect(spinner).toMatchSnapshot();
});

it('renders children before timeout', () => {
  const spinner = mount(
    <DeferredSpinner>
      <div>foo</div>
    </DeferredSpinner>
  );
  expect(spinner).toMatchSnapshot();
  jest.runAllTimers();
  spinner.update();
  expect(spinner).toMatchSnapshot();
});

it('is controlled by loading prop', () => {
  const spinner = mount(
    <DeferredSpinner loading={false}>
      <div>foo</div>
    </DeferredSpinner>
  );
  expect(spinner).toMatchSnapshot();
  spinner.setProps({ loading: true });
  expect(spinner).toMatchSnapshot();
  jest.runAllTimers();
  spinner.update();
  expect(spinner).toMatchSnapshot();
  spinner.setProps({ loading: false });
  expect(spinner).toMatchSnapshot();
});
