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
import { shallow } from 'enzyme';
import * as React from 'react';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getComponentNavigation } from '../../../api/nav';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import ProjectRowActions, { Props } from '../ProjectRowActions';

jest.mock('../../../api/nav', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({})
}));

it('renders correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

describe('restore access', () => {
  beforeAll(() => {
    jest.resetAllMocks();
    (getComponentNavigation as jest.Mock).mockResolvedValue({
      configuration: {
        showPermissions: false
      }
    });
  });

  it('shows the restore access action', async () => {
    const wrapper = shallowRender();
    wrapper.instance().handleDropdownOpen();
    await waitAndUpdate(wrapper);

    expect(getComponentNavigation).toBeCalledWith({ component: 'foo' });
    expect(wrapper.find('.js-restore-access').exists()).toBe(true);
  });

  it('shows the restore access modal', async () => {
    const wrapper = shallowRender();
    wrapper.instance().handleDropdownOpen();
    await waitAndUpdate(wrapper);

    click(wrapper.find('.js-restore-access'));
    expect(wrapper.find('RestoreAccessModal')).toMatchSnapshot();

    wrapper.instance().handleRestoreAccessDone();
    await waitAndUpdate(wrapper);
    expect(wrapper.find('.js-restore-access').exists()).toBe(false);
    expect(wrapper.find('RestoreAccessModal').exists()).toBe(false);
  });
});

describe('permissions', () => {
  beforeAll(() => {
    jest.resetAllMocks();
    (getComponentNavigation as jest.Mock).mockResolvedValue({
      configuration: {
        showPermissions: true
      }
    });
  });

  it('shows the update permissions action', async () => {
    const wrapper = shallowRender();
    wrapper.instance().handleDropdownOpen();
    await waitAndUpdate(wrapper);
    expect(wrapper.find('.js-edit-permissions').exists()).toBe(true);
  });

  it('shows the apply permission template modal', async () => {
    const wrapper = shallowRender();
    wrapper.instance().handleDropdownOpen();
    await waitAndUpdate(wrapper);

    click(wrapper.find('.js-apply-template'));
    expect(wrapper.find('ApplyTemplate')).toMatchSnapshot();

    wrapper.instance().handleApplyTemplateClose();
    await waitAndUpdate(wrapper);
    expect(wrapper.find('ApplyTemplate').exists()).toBe(false);
  });
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow<ProjectRowActions>(
    <ProjectRowActions
      currentUser={mockLoggedInUser()}
      organization="org"
      project={{
        id: 'foo',
        key: 'foo',
        name: 'Foo',
        organization: 'bar',
        qualifier: 'TRK',
        visibility: 'private'
      }}
      {...props}
    />
  );
}
