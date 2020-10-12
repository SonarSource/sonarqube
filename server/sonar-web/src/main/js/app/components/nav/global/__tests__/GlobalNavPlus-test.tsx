/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getAlmSettings } from '../../../../../api/alm-settings';
import { getComponentNavigation } from '../../../../../api/nav';
import CreateFormShim from '../../../../../apps/portfolio/components/CreateFormShim';
import { mockLoggedInUser, mockRouter } from '../../../../../helpers/testMocks';
import { getComponentAdminUrl, getComponentOverviewUrl } from '../../../../../helpers/urls';
import { AlmKeys } from '../../../../../types/alm-settings';
import { ComponentQualifier } from '../../../../../types/component';
import { GlobalNavPlus } from '../GlobalNavPlus';

const PROJECT_CREATION_RIGHT = 'provisioning';
const APP_CREATION_RIGHT = 'applicationcreator';
const PORTFOLIO_CREATION_RIGHT = 'portfoliocreator';

jest.mock('../../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue(null)
}));

jest.mock('../../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([])
}));

jest.mock('../../../../../api/nav', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({})
}));

jest.mock('../../../../../helpers/urls', () => ({
  getComponentOverviewUrl: jest.fn(),
  getComponentAdminUrl: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly when no rights', async () => {
  const wrapper = shallowRender([], {});
  expect(wrapper.type()).toBeNull();
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).not.toBeCalled();
});

it('should render correctly if branches not enabled', async () => {
  const wrapper = shallowRender([PROJECT_CREATION_RIGHT], { branchesEnabled: false });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getAlmSettings).not.toBeCalled();
});

it('should render correctly', async () => {
  expect(
    shallowRender([APP_CREATION_RIGHT, PORTFOLIO_CREATION_RIGHT, PROJECT_CREATION_RIGHT], {})
  ).toMatchSnapshot('no governance');

  const wrapper = shallowRender(
    [APP_CREATION_RIGHT, PORTFOLIO_CREATION_RIGHT, PROJECT_CREATION_RIGHT],
    { enableGovernance: true }
  );
  await waitAndUpdate(wrapper);
  wrapper.setState({ boundAlms: ['bitbucket'] });
  expect(wrapper).toMatchSnapshot('full rights and alms');
});

it('should load correctly', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce([
    { alm: AlmKeys.Azure, key: 'A1' }, // No azure onboarding for now
    { alm: AlmKeys.Bitbucket, key: 'B1' },
    { alm: AlmKeys.GitHub, key: 'GH1' },
    { alm: AlmKeys.GitLab, key: 'GL1', url: 'ok' }
  ]);

  const wrapper = shallowRender([PROJECT_CREATION_RIGHT], {});

  await waitAndUpdate(wrapper);

  expect(getAlmSettings).toBeCalled();
  expect(wrapper.state().boundAlms).toEqual([AlmKeys.Bitbucket, AlmKeys.GitHub, AlmKeys.GitLab]);
});

it('should load without gitlab when no url', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce([{ alm: AlmKeys.GitLab, key: 'GL1' }]);

  const wrapper = shallowRender([PROJECT_CREATION_RIGHT], {});

  await waitAndUpdate(wrapper);

  expect(getAlmSettings).toBeCalled();
  expect(wrapper.state().boundAlms).toEqual([]);
});

it('should display component creation form', () => {
  const wrapper = shallowRender([PORTFOLIO_CREATION_RIGHT], { enableGovernance: true });

  wrapper.instance().handleComponentCreationClick(ComponentQualifier.Portfolio);
  wrapper.setState({ governanceReady: true });

  expect(wrapper.find(CreateFormShim).exists()).toBe(true);
});

describe('handleComponentCreate', () => {
  (getComponentNavigation as jest.Mock)
    .mockResolvedValueOnce({
      configuration: { showSettings: true }
    })
    .mockResolvedValueOnce({});

  const portfolio = { key: 'portfolio', qualifier: ComponentQualifier.Portfolio };

  const wrapper = shallowRender([], { enableGovernance: true });

  it('should redirect to admin', async () => {
    wrapper.instance().handleComponentCreate(portfolio);
    await waitAndUpdate(wrapper);
    expect(getComponentAdminUrl).toBeCalledWith(portfolio.key, portfolio.qualifier);
    expect(wrapper.state().creatingComponent).toBeUndefined();
  });

  it('should redirect to dashboard', async () => {
    wrapper.instance().handleComponentCreate(portfolio);
    await waitAndUpdate(wrapper);

    expect(getComponentOverviewUrl).toBeCalledWith(portfolio.key, portfolio.qualifier);
  });
});

function shallowRender(
  permissions: string[] = [],
  { enableGovernance = false, branchesEnabled = true }
) {
  let qualifiers: ComponentQualifier[];
  if (enableGovernance) {
    qualifiers = [ComponentQualifier.Portfolio, ComponentQualifier.Application];
  } else if (branchesEnabled) {
    qualifiers = [ComponentQualifier.Application];
  } else {
    qualifiers = [];
  }
  return shallow<GlobalNavPlus>(
    <GlobalNavPlus
      appState={{
        branchesEnabled,
        qualifiers
      }}
      currentUser={mockLoggedInUser({ permissions: { global: permissions } })}
      router={mockRouter()}
    />
  );
}
