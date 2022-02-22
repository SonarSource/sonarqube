/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import BoxedTabs, { BoxedTabsProps } from '../../../../components/controls/BoxedTabs';
import { mockHotspot, mockHotspotRule } from '../../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../../helpers/testMocks';
import HotspotViewerTabs, { TabKeys } from '../HotspotViewerTabs';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('risk');

  const onSelect: (tab: TabKeys) => void = wrapper.find(BoxedTabs).prop('onSelect');

  onSelect(TabKeys.VulnerabilityDescription);
  expect(wrapper).toMatchSnapshot('vulnerability');

  onSelect(TabKeys.FixRecommendation);
  expect(wrapper).toMatchSnapshot('fix');

  expect(
    shallowRender({
      hotspot: mockHotspot({
        creationDate: undefined,
        rule: mockHotspotRule({
          riskDescription: undefined,
          fixRecommendations: undefined,
          vulnerabilityDescription: undefined
        })
      })
    })
      .find<BoxedTabsProps<string>>(BoxedTabs)
      .props().tabs
  ).toHaveLength(1);

  expect(
    shallowRender({
      hotspot: mockHotspot({
        comment: [
          {
            createdAt: '2019-01-01',
            htmlText: '<strong>test</strong>',
            key: 'comment-key',
            login: 'me',
            markdown: '*test*',
            updatable: false,
            user: mockUser()
          }
        ]
      })
    })
  ).toMatchSnapshot('with comments or changelog element');
});

it('should filter empty tab', () => {
  const count = shallowRender({
    hotspot: mockHotspot({
      rule: mockHotspotRule()
    })
  }).state().tabs.length;

  expect(
    shallowRender({
      hotspot: mockHotspot({
        rule: mockHotspotRule({ riskDescription: undefined })
      })
    }).state().tabs.length
  ).toBe(count - 1);
});

it('should select first tab on hotspot update', () => {
  const wrapper = shallowRender();
  const onSelect: (tab: TabKeys) => void = wrapper.find(BoxedTabs).prop('onSelect');

  onSelect(TabKeys.VulnerabilityDescription);
  expect(wrapper.state().currentTab.key).toBe(TabKeys.VulnerabilityDescription);
  wrapper.setProps({ hotspot: mockHotspot({ key: 'new_key' }) });
  expect(wrapper.state().currentTab.key).toBe(TabKeys.Code);
});

it('should select first tab when hotspot location is selected and is not undefined', () => {
  const wrapper = shallowRender();
  const onSelect: (tab: TabKeys) => void = wrapper.find(BoxedTabs).prop('onSelect');

  onSelect(TabKeys.VulnerabilityDescription);
  expect(wrapper.state().currentTab.key).toBe(TabKeys.VulnerabilityDescription);

  wrapper.setProps({ selectedHotspotLocation: 1 });
  expect(wrapper.state().currentTab.key).toBe(TabKeys.Code);

  onSelect(TabKeys.VulnerabilityDescription);
  expect(wrapper.state().currentTab.key).toBe(TabKeys.VulnerabilityDescription);

  wrapper.setProps({ selectedHotspotLocation: undefined });
  expect(wrapper.state().currentTab.key).toBe(TabKeys.VulnerabilityDescription);
});

function shallowRender(props?: Partial<HotspotViewerTabs['props']>) {
  return shallow<HotspotViewerTabs>(
    <HotspotViewerTabs
      codeTabContent={<div>CodeTabContent</div>}
      hotspot={mockHotspot()}
      {...props}
    />
  );
}
