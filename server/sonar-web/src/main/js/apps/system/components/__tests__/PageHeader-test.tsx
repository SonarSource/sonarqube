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
import PageHeader, { Props } from '../PageHeader';

jest.mock('sonar-ui-common/helpers/dates', () => ({
  toShortNotSoISOString: () => '2019-01-01'
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ loading: true, showActions: false })).toMatchSnapshot();
  expect(shallowRender({ serverId: 'foo-bar', version: '7.7.0.1234' })).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <PageHeader
      isCluster={true}
      loading={false}
      logLevel="INFO"
      onLogLevelChange={jest.fn()}
      showActions={true}
      {...props}
    />
  );
}
