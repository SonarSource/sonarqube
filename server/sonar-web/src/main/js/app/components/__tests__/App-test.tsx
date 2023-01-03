/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { connect } from 'react-redux';
import { fetchLanguages as realFetchLanguages } from '../../../store/rootActions';
import { App } from '../App';

jest.mock('react-redux', () => ({
  connect: jest.fn(() => (a: any) => a)
}));

jest.mock('../../../store/rootReducer', () => ({
  getGlobalSettingValue: jest.fn((_, key: string) => ({
    value: key === 'sonar.lf.enableGravatar' ? 'true' : 'http://gravatar.com'
  }))
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ enableGravatar: true, gravatarServerUrl: 'http://example.com' })
  ).toMatchSnapshot('with gravatar');
});

it('should correctly fetch available languages', () => {
  const fetchLanguages = jest.fn();
  shallowRender({ fetchLanguages });
  expect(fetchLanguages).toBeCalled();
});

it('should correctly set the scrollbar width as a custom property', () => {
  shallowRender();
  expect(document.body.style.getPropertyValue('--sbw')).toBe('0px');
});

describe('redux', () => {
  it('should correctly map state and dispatch props', () => {
    const [mapStateToProps, mapDispatchToProps] = (connect as jest.Mock).mock.calls[0];

    expect(mapStateToProps({})).toEqual({
      enableGravatar: true,
      gravatarServerUrl: 'http://gravatar.com'
    });
    expect(mapDispatchToProps).toEqual(
      expect.objectContaining({ fetchLanguages: realFetchLanguages })
    );
  });
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App fetchLanguages={jest.fn()} enableGravatar={false} gravatarServerUrl="" {...props} />
  );
}
