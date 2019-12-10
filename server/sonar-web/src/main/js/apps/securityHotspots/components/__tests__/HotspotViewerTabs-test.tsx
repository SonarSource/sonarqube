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
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import {
  mockDetailledHotspot,
  mockDetailledHotspotRule
} from '../../../../helpers/mocks/security-hotspots';
import HotspotViewerTabs, { HotspotViewerTabsProps, Tabs } from '../HotspotViewerTabs';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('risk');

  const onSelect = wrapper.find(BoxedTabs).prop('onSelect') as (tab: Tabs) => void;

  if (!onSelect) {
    fail('onSelect should be defined');
  } else {
    onSelect(Tabs.VulnerabilityDescription);
    expect(wrapper).toMatchSnapshot('vulnerability');

    onSelect(Tabs.FixRecommendation);
    expect(wrapper).toMatchSnapshot('fix');
  }

  expect(
    shallowRender({
      hotspot: mockDetailledHotspot({
        rule: mockDetailledHotspotRule({ riskDescription: undefined })
      })
    })
  ).toMatchSnapshot('empty tab');

  expect(
    shallowRender({
      hotspot: mockDetailledHotspot({
        rule: mockDetailledHotspotRule({
          riskDescription: undefined,
          fixRecommendations: undefined,
          vulnerabilityDescription: undefined
        })
      })
    })
  ).toMatchSnapshot('no tabs');
});

function shallowRender(props?: Partial<HotspotViewerTabsProps>) {
  return shallow(<HotspotViewerTabs hotspot={mockDetailledHotspot()} {...props} />);
}
