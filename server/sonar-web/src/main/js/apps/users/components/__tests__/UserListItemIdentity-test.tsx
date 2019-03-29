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
import UserListItemIdentity, { Props, ExternalProvider } from '../UserListItemIdentity';

describe('#UserListItemIdentity', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
  });

  function shallowRender(props: Partial<Props> = {}) {
    return shallow(
      <UserListItemIdentity
        identityProvider={{
          backgroundColor: 'blue',
          iconPath: 'icon/path',
          key: 'foo',
          name: 'Foo Provider'
        }}
        user={{
          active: true,
          email: 'obi.one@empire',
          externalProvider: 'foo',
          lastConnectionDate: '2019-01-18T15:06:33+0100',
          local: false,
          login: 'obi',
          name: 'One',
          scmAccounts: []
        }}
        {...props}
      />
    );
  }
});

describe('#ExternalProvider', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot();
  });

  it('should render the user external provider and identity', () => {
    expect(shallowRender({ identityProvider: undefined })).toMatchSnapshot();
  });

  function shallowRender(props: Partial<Props> = {}) {
    return shallow(
      <ExternalProvider
        identityProvider={{
          backgroundColor: 'blue',
          iconPath: 'icon/path',
          key: 'foo',
          name: 'Foo Provider'
        }}
        user={{
          active: true,
          email: 'obi.one@empire',
          externalProvider: 'foo',
          lastConnectionDate: '2019-01-18T15:06:33+0100',
          local: false,
          login: 'obi',
          name: 'One',
          scmAccounts: []
        }}
        {...props}
      />
    );
  }
});
