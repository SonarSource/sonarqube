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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockDefinition } from '../../../../helpers/mocks/settings';
import { scrollToElement } from '../../../../helpers/scrolling';
import SettingsSearchRenderer, { SettingsSearchRendererProps } from '../SettingsSearchRenderer';

jest.mock('../../../../helpers/scrolling', () => ({
  scrollToElement: jest.fn(),
}));

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useRef: jest.fn(),
    useEffect: jest.fn(),
  };
});

afterAll(() => {
  jest.clearAllMocks();
});

it('should render correctly when closed', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly when open', () => {
  expect(shallowRender({ showResults: true })).toMatchSnapshot('no results');
  expect(
    shallowRender({
      results: [mockDefinition({ name: 'Foo!' }), mockDefinition({ key: 'bar' })],
      selectedResult: 'bar',
      showResults: true,
    })
  ).toMatchSnapshot('results');
});

it('should scroll to selected element', () => {
  const scrollable = {};
  const scrollableRef = { current: scrollable };
  const selected = {};
  const selectedRef = { current: selected };

  (React.useRef as jest.Mock)
    .mockImplementationOnce(() => scrollableRef)
    .mockImplementationOnce(() => selectedRef);
  (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());

  shallowRender();

  expect(scrollToElement).toHaveBeenCalled();
});

function shallowRender(overrides: Partial<SettingsSearchRendererProps> = {}) {
  return shallow<SettingsSearchRendererProps>(
    <SettingsSearchRenderer
      searchQuery=""
      showResults={false}
      onClickOutside={jest.fn()}
      onMouseOverResult={jest.fn()}
      onSearchInputChange={jest.fn()}
      onSearchInputFocus={jest.fn()}
      onSearchInputKeyDown={jest.fn()}
      {...overrides}
    />
  );
}
