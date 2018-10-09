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
import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import OrganizationDetailsStep from '../OrganizationDetailsStep';
import { click } from '../../../../helpers/testUtils';
import { getOrganization } from '../../../../api/organizations';

jest.mock('../../../../api/organizations', () => ({
  getOrganization: jest.fn()
}));

beforeEach(() => {
  (getOrganization as jest.Mock).mockResolvedValue(undefined);
});

it('should render form', () => {
  const wrapper = shallow(
    <OrganizationDetailsStep
      finished={false}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      open={true}
    />
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.dive()).toMatchSnapshot();
  expect(getForm(wrapper)).toMatchSnapshot();
  expect(
    getForm(wrapper)
      .find('.js-additional-info')
      .prop('hidden')
  ).toBe(true);

  click(getForm(wrapper).find('ResetButtonLink'));
  wrapper.update();
  expect(
    getForm(wrapper)
      .find('.js-additional-info')
      .prop('hidden')
  ).toBe(false);
});

it('should validate', () => {
  const wrapper = shallow(
    <OrganizationDetailsStep
      finished={false}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      open={true}
    />
  );
  const instance = wrapper.instance() as OrganizationDetailsStep;

  expect(
    instance.handleValidate({ avatar: '', description: '', name: '', key: 'foo', url: '' })
  ).resolves.toEqual({});

  expect(
    instance.handleValidate({
      avatar: '',
      description: '',
      name: '',
      key: 'x'.repeat(256),
      url: ''
    })
  ).rejects.toEqual({ key: 'onboarding.create_organization.organization_name.error' });

  expect(
    instance.handleValidate({ avatar: 'bla', description: '', name: '', key: 'foo', url: '' })
  ).rejects.toEqual({ avatar: 'onboarding.create_organization.avatar.error' });

  expect(
    instance.handleValidate({
      avatar: '',
      description: '',
      name: 'x'.repeat(256),
      key: 'foo',
      url: ''
    })
  ).rejects.toEqual({ name: 'onboarding.create_organization.display_name.error' });

  expect(
    instance.handleValidate({ avatar: '', description: '', name: '', key: 'foo', url: 'bla' })
  ).rejects.toEqual({ url: 'onboarding.create_organization.url.error' });

  (getOrganization as jest.Mock).mockResolvedValue({});
  expect(
    instance.handleValidate({ avatar: '', description: '', name: '', key: 'foo', url: '' })
  ).rejects.toEqual({ key: 'onboarding.create_organization.organization_name.taken' });
});

it('should render result', () => {
  const wrapper = shallow(
    <OrganizationDetailsStep
      finished={true}
      onContinue={jest.fn()}
      onOpen={jest.fn()}
      open={false}
      organization={{ avatar: '', description: '', key: 'org', name: 'Organization', url: '' }}
    />
  );
  expect(wrapper.dive()).toMatchSnapshot();
});

function getForm(wrapper: ShallowWrapper) {
  return wrapper
    .dive()
    .find('ValidationForm')
    .dive()
    .dive()
    .children();
}
