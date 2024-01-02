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
import React from 'react';
import { ButtonLink } from '../../../../components/controls/buttons';
import { mockHotspot, mockHotspotRule } from '../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { RiskExposure } from '../../../../types/security-hotspots';
import {
  HotspotPrimaryLocationBox,
  HotspotPrimaryLocationBoxProps,
} from '../HotspotPrimaryLocationBox';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useRef: jest.fn(),
    useEffect: jest.fn(),
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('User logged in');
  expect(shallowRender({ currentUser: mockCurrentUser() })).toMatchSnapshot('User not logged in ');
});

it.each([[RiskExposure.HIGH], [RiskExposure.MEDIUM], [RiskExposure.LOW]])(
  'should indicate risk exposure: %s',
  (vulnerabilityProbability) => {
    const wrapper = shallowRender({
      hotspot: mockHotspot({ rule: mockHotspotRule({ vulnerabilityProbability }) }),
    });

    expect(wrapper.hasClass(`hotspot-risk-exposure-${vulnerabilityProbability}`)).toBe(true);
  }
);

it('should handle click', () => {
  const onCommentClick = jest.fn();
  const wrapper = shallowRender({ onCommentClick });

  wrapper.find(ButtonLink).simulate('click');

  expect(onCommentClick).toHaveBeenCalled();
});

it('should scroll on load if no secondary locations selected', () => {
  const node = document.createElement('div');
  (React.useRef as jest.Mock).mockImplementationOnce(() => ({ current: node }));
  (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());

  const scroll = jest.fn();
  shallowRender({ scroll });

  expect(scroll).toHaveBeenCalled();
});

it('should not scroll on load if a secondary location is selected', () => {
  const node = document.createElement('div');
  (React.useRef as jest.Mock).mockImplementationOnce(() => ({ current: node }));
  (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());

  const scroll = jest.fn();
  shallowRender({ scroll, secondaryLocationSelected: true });

  expect(scroll).not.toHaveBeenCalled();
});

it('should not scroll on load if node is not defined', () => {
  (React.useRef as jest.Mock).mockImplementationOnce(() => ({ current: undefined }));
  (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());

  const scroll = jest.fn();
  shallowRender({ scroll });

  expect(scroll).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<HotspotPrimaryLocationBoxProps> = {}) {
  return shallow(
    <HotspotPrimaryLocationBox
      currentUser={mockLoggedInUser()}
      hotspot={mockHotspot()}
      onCommentClick={jest.fn()}
      scroll={jest.fn()}
      secondaryLocationSelected={false}
      {...props}
    />
  );
}
