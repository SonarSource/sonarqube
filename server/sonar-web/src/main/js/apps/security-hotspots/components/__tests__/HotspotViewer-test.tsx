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
import { getSecurityHotspotDetails } from '../../../../api/security-hotspots';
import { mockComponent } from '../../../../helpers/testMocks';
import HotspotViewer from '../HotspotViewer';

const hotspotKey = 'hotspot-key';

jest.mock('../../../../api/security-hotspots', () => ({
  getSecurityHotspotDetails: jest.fn().mockResolvedValue({ id: `I am a detailled hotspot` })
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(getSecurityHotspotDetails).toHaveBeenCalledWith(hotspotKey);

  const newHotspotKey = `new-${hotspotKey}`;
  wrapper.setProps({ hotspotKey: newHotspotKey });

  await waitAndUpdate(wrapper);
  expect(getSecurityHotspotDetails).toHaveBeenCalledWith(newHotspotKey);
});

function shallowRender(props?: Partial<HotspotViewer['props']>) {
  return shallow<HotspotViewer>(
    <HotspotViewer
      component={mockComponent()}
      hotspotKey={hotspotKey}
      onUpdateHotspot={jest.fn()}
      securityCategories={{ cat1: { title: 'cat1' } }}
      {...props}
    />
  );
}
