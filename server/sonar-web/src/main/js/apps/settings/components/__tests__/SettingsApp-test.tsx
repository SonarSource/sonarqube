/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import ScreenPositionHelper from '../../../../components/common/ScreenPositionHelper';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from '../../../../helpers/pages';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import {
  ALM_INTEGRATION,
  ANALYSIS_SCOPE_CATEGORY,
  LANGUAGES_CATEGORY,
  NEW_CODE_PERIOD_CATEGORY,
  PULL_REQUEST_DECORATION_BINDING_CATEGORY
} from '../AdditionalCategoryKeys';
import { SettingsApp } from '../SettingsApp';

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  addWhitePageClass: jest.fn(),
  removeSideBarClass: jest.fn(),
  removeWhitePageClass: jest.fn()
}));

it('should render default view correctly', async () => {
  const wrapper = shallowRender();

  expect(addSideBarClass).toBeCalled();
  expect(addWhitePageClass).toBeCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find(ScreenPositionHelper).dive()).toMatchSnapshot();

  wrapper.unmount();

  expect(removeSideBarClass).toBeCalled();
  expect(removeWhitePageClass).toBeCalled();
});

it('should render newCodePeriod correctly', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category: NEW_CODE_PERIOD_CATEGORY } })
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render languages correctly', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category: LANGUAGES_CATEGORY } })
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render analysis scope correctly', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category: ANALYSIS_SCOPE_CATEGORY } })
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render ALM integration correctly', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category: ALM_INTEGRATION } })
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render pull request decoration binding correctly', async () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { category: PULL_REQUEST_DECORATION_BINDING_CATEGORY } })
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<SettingsApp['props']> = {}) {
  return shallow(
    <SettingsApp
      defaultCategory="general"
      fetchSettings={jest.fn().mockResolvedValue({})}
      location={mockLocation()}
      params={{}}
      router={mockRouter()}
      routes={[]}
      {...props}
    />
  );
}
