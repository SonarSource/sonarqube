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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import BoxedTabs, { BoxedTabsProps } from '../../../../components/controls/BoxedTabs';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../../helpers/testMocks';
import { mockEvent } from '../../../../helpers/testUtils';
import { RuleDescriptionSections } from '../../../coding-rules/rule';
import HotspotViewerTabs, { TabKeys } from '../HotspotViewerTabs';

const originalAddEventListener = window.addEventListener;
const originalRemoveEventListener = window.removeEventListener;

beforeEach(() => {
  Object.defineProperty(window, 'addEventListener', {
    value: jest.fn(),
  });
  Object.defineProperty(window, 'removeEventListener', {
    value: jest.fn(),
  });
});

afterEach(() => {
  Object.defineProperty(window, 'addEventListener', {
    value: originalAddEventListener,
  });
  Object.defineProperty(window, 'removeEventListener', {
    value: originalRemoveEventListener,
  });
});

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
      }),
      ruleDescriptionSections: undefined,
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
            user: mockUser(),
          },
        ],
      }),
    })
  ).toMatchSnapshot('with comments or changelog element');
});

it('should filter empty tab', () => {
  const count = shallowRender({
    hotspot: mockHotspot(),
  }).state().tabs.length;

  expect(
    shallowRender({
      ruleDescriptionSections: [
        {
          key: RuleDescriptionSections.ROOT_CAUSE,
          content: 'cause',
        },
        {
          key: RuleDescriptionSections.HOW_TO_FIX,
          content: 'how',
        },
      ],
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

describe('keyboard navigation', () => {
  const tabList = [
    TabKeys.Code,
    TabKeys.RiskDescription,
    TabKeys.VulnerabilityDescription,
    TabKeys.FixRecommendation,
  ];
  const wrapper = shallowRender();

  it.each([
    ['selecting next', 0, KeyboardKeys.RightArrow, 1],
    ['selecting previous', 1, KeyboardKeys.LeftArrow, 0],
    ['selecting previous, non-existent', 0, KeyboardKeys.LeftArrow, 0],
    ['selecting next, non-existent', 3, KeyboardKeys.RightArrow, 3],
  ])('should work when %s', (_, start, key, expected) => {
    wrapper.setState({ currentTab: wrapper.state().tabs[start] });
    wrapper.instance().handleKeyboardNavigation(mockEvent({ key }));

    expect(wrapper.state().currentTab.key).toBe(tabList[expected]);
  });
});

it("shouldn't navigate when ctrl or command are pressed with up and down", () => {
  const wrapper = mount<HotspotViewerTabs>(
    <HotspotViewerTabs codeTabContent={<div>CodeTabContent</div>} hotspot={mockHotspot()} />
  );

  wrapper.setState({ currentTab: wrapper.state().tabs[0] });
  wrapper
    .instance()
    .handleKeyboardNavigation(mockEvent({ key: KeyboardKeys.LeftArrow, metaKey: true }));

  expect(wrapper.state().currentTab.key).toBe(TabKeys.Code);
});

it('should navigate when up and down key are pressed', () => {
  const wrapper = mount<HotspotViewerTabs>(
    <HotspotViewerTabs codeTabContent={<div>CodeTabContent</div>} hotspot={mockHotspot()} />
  );

  expect(window.addEventListener).toHaveBeenCalled();

  wrapper.unmount();

  expect(window.removeEventListener).toHaveBeenCalled();
});

function shallowRender(props?: Partial<HotspotViewerTabs['props']>) {
  return shallow<HotspotViewerTabs>(
    <HotspotViewerTabs
      codeTabContent={<div>CodeTabContent</div>}
      hotspot={mockHotspot()}
      ruleDescriptionSections={[
        {
          key: RuleDescriptionSections.ASSESS_THE_PROBLEM,
          content: 'assess',
        },
        {
          key: RuleDescriptionSections.ROOT_CAUSE,
          content: 'cause',
        },
        {
          key: RuleDescriptionSections.HOW_TO_FIX,
          content: 'how',
        },
      ]}
      {...props}
    />
  );
}
