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
import { ALM_KEYS } from '../../../../../types/alm-settings';
import PRDecorationBindingRenderer, {
  PRDecorationBindingRendererProps
} from '../PRDecorationBindingRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loading: false })).toMatchSnapshot();
});

it('should render single instance correctly', () => {
  const singleInstance = {
    key: 'single',
    url: 'http://single.url',
    alm: ALM_KEYS.GITHUB
  };
  expect(
    shallowRender({
      loading: false,
      instances: [singleInstance]
    })
  ).toMatchSnapshot();
});

it('should render multiple instances correctly', () => {
  const urls = ['http://github.enterprise.com', 'http://bbs.enterprise.com'];
  const instances = [
    {
      alm: ALM_KEYS.GITHUB,
      key: 'i1',
      url: urls[0]
    },
    {
      alm: ALM_KEYS.GITHUB,
      key: 'i2',
      url: urls[0]
    },
    {
      alm: ALM_KEYS.BITBUCKET,
      key: 'i3',
      url: urls[1]
    },
    {
      alm: ALM_KEYS.AZURE,
      key: 'i4'
    }
  ];

  //unfilled
  expect(
    shallowRender({
      instances,
      loading: false
    })
  ).toMatchSnapshot();

  // filled
  expect(
    shallowRender({
      formData: {
        key: 'i1',
        repository: 'account/repo'
      },
      instances,
      loading: false,
      originalData: {
        key: 'i1',
        repository: 'account/repo'
      }
    })
  ).toMatchSnapshot();
});

it('should display action state correctly', () => {
  const urls = ['http://url.com'];
  const instances = [{ key: 'key', url: urls[0], alm: ALM_KEYS.GITHUB }];

  expect(shallowRender({ instances, loading: false, saving: true })).toMatchSnapshot();
  expect(shallowRender({ instances, loading: false, success: true })).toMatchSnapshot();
  expect(
    shallowRender({
      instances,
      isValid: true,
      loading: false
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<PRDecorationBindingRendererProps> = {}) {
  return shallow(
    <PRDecorationBindingRenderer
      formData={{
        key: '',
        repository: ''
      }}
      instances={[]}
      isValid={false}
      loading={true}
      onFieldChange={jest.fn()}
      onReset={jest.fn()}
      onSubmit={jest.fn()}
      originalData={undefined}
      saving={false}
      success={false}
      {...props}
    />
  );
}
