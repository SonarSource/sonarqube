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
import { searchQualityProfiles } from '../../../../api/quality-profiles';
import { getRulesApp } from '../../../../api/rules';
import ScreenPositionHelper from '../../../../components/common/ScreenPositionHelper';
import {
  mockCurrentUser,
  mockLocation,
  mockQualityProfile,
  mockRouter,
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { App } from '../App';

jest.mock('../../../../components/common/ScreenPositionHelper');

jest.mock('../../../../api/rules', () => {
  const { mockRule } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getRulesApp: jest.fn().mockResolvedValue({ canWrite: true, repositories: [] }),
    searchRules: jest.fn().mockResolvedValue({
      actives: [],
      rawActives: [],
      facets: [],
      rawFacets: [],
      p: 0,
      ps: 100,
      rules: [mockRule(), mockRule()],
      total: 0,
    }),
  };
});

jest.mock('../../../../api/quality-profiles', () => ({
  searchQualityProfiles: jest.fn().mockResolvedValue({ profiles: [] }),
}));

jest.mock('../../../../helpers/system', () => ({
  getReactDomContainerSelector: () => '#content',
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('loaded');
  expect(wrapper.find(ScreenPositionHelper).dive()).toMatchSnapshot(
    'loaded (ScreenPositionHelper)'
  );
});

describe('renderBulkButton', () => {
  it('should be null when the user is not logged in', () => {
    const wrapper = shallowRender({
      currentUser: mockCurrentUser(),
    });
    expect(wrapper.instance().renderBulkButton()).toMatchSnapshot();
  });

  it('should be null when the user does not have the sufficient permission', () => {
    (getRulesApp as jest.Mock).mockReturnValueOnce({ canWrite: false, repositories: [] });

    const wrapper = shallowRender();
    expect(wrapper.instance().renderBulkButton()).toMatchSnapshot();
  });

  it('should show bulk change button when user has global admin rights on quality profiles', async () => {
    (getRulesApp as jest.Mock).mockReturnValueOnce({ canWrite: true, repositories: [] });
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);

    expect(wrapper.instance().renderBulkButton()).toMatchSnapshot();
  });

  it('should show bulk change button when user has edit rights on specific quality profile', async () => {
    (getRulesApp as jest.Mock).mockReturnValueOnce({ canWrite: false, repositories: [] });
    (searchQualityProfiles as jest.Mock).mockReturnValueOnce({
      profiles: [mockQualityProfile({ key: 'foo', actions: { edit: true } }), mockQualityProfile()],
    });

    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);

    expect(wrapper.instance().renderBulkButton()).toMatchSnapshot();
  });
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      currentUser={mockCurrentUser({
        isLoggedIn: true,
      })}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
