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
import { addNoFooterPageClass } from 'sonar-ui-common/helpers/pages';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getSecurityHotspots } from '../../../api/security-hotspots';
import { mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { getStandards } from '../../../helpers/security-standard';
import { mockComponent } from '../../../helpers/testMocks';
import SecurityHotspotsApp from '../SecurityHotspotsApp';

jest.mock('sonar-ui-common/helpers/pages', () => ({
  addNoFooterPageClass: jest.fn(),
  removeNoFooterPageClass: jest.fn()
}));

jest.mock('../../../api/security-hotspots', () => ({
  getSecurityHotspots: jest.fn().mockResolvedValue({ hotspots: [], rules: [] })
}));

jest.mock('../../../helpers/security-standard', () => ({
  getStandards: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should load data correctly', async () => {
  const sonarsourceSecurity = { cat1: { title: 'cat 1' } };
  (getStandards as jest.Mock).mockResolvedValue({ sonarsourceSecurity });

  const hotspots = [mockRawHotspot()];
  (getSecurityHotspots as jest.Mock).mockResolvedValue({
    hotspots
  });

  const wrapper = shallowRender();

  expect(wrapper.state().loading).toBe(true);

  expect(addNoFooterPageClass).toBeCalled();
  expect(getStandards).toBeCalled();
  expect(getSecurityHotspots).toBeCalled();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().hotspots).toEqual(hotspots);
  expect(wrapper.state().selectedHotspotKey).toBe(hotspots[0].key);
  expect(wrapper.state().securityCategories).toBe(sonarsourceSecurity);

  expect(wrapper.state());
});

function shallowRender(props: Partial<SecurityHotspotsApp['props']> = {}) {
  return shallow<SecurityHotspotsApp>(
    <SecurityHotspotsApp branchLike={mockMainBranch()} component={mockComponent()} {...props} />
  );
}
