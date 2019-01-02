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
import * as React from 'react';
import { shallow } from 'enzyme';
import UpgradeOrganizationBox from '../UpgradeOrganizationBox';
import { hasMessage } from '../../../../helpers/l10n';
import { getSubscriptionPlans } from '../../../../api/billing';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';

jest.mock('../../../../helpers/l10n', () => {
  const l10n = require.requireActual('../../../../helpers/l10n');
  l10n.hasMessage = jest.fn().mockReturnValue(true);
  return l10n;
});

jest.mock('../../../../api/billing', () => ({
  getSubscriptionPlans: jest.fn().mockResolvedValue([{ maxNcloc: 100000, price: 10 }])
}));

const organization = { key: 'foo', name: 'Foo' };

beforeEach(() => {
  (hasMessage as jest.Mock<any>).mockClear();
  (getSubscriptionPlans as jest.Mock<any>).mockClear();
});

it('should not render', () => {
  (hasMessage as jest.Mock<any>).mockReturnValueOnce(false);
  expect(
    shallow(
      <UpgradeOrganizationBox onOrganizationUpgrade={jest.fn()} organization={organization} />
    ).type()
  ).toBeNull();
});

it('should render correctly', async () => {
  const wrapper = shallow(
    <UpgradeOrganizationBox onOrganizationUpgrade={jest.fn()} organization={organization} />
  );
  await waitAndUpdate(wrapper);
  expect(getSubscriptionPlans).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('Button'));
  expect(wrapper.find('UpgradeOrganizationModal').exists()).toBe(true);
});
