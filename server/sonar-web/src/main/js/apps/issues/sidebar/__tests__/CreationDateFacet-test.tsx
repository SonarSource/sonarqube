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
import * as React from 'react';
import { IntlShape } from 'react-intl';
import { mockComponent } from '../../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../../types/component';
import { CreationDateFacet } from '../CreationDateFacet';

it('should render correctly', () => {
  expect(shallowRender({ open: false })).toMatchSnapshot('closed');
  expect(shallowRender()).toMatchSnapshot('clear');
  expect(shallowRender({ createdAt: '2019.05.21T13:33:00Z' })).toMatchSnapshot('created at');
  expect(
    shallowRender({
      createdAfter: new Date('2019.04.29T13:33:00Z'),
      createdAfterIncludesTime: true,
    })
  ).toMatchSnapshot('created after');
  expect(
    shallowRender({
      createdAfter: new Date('2019.04.29T13:33:00Z'),
      createdAfterIncludesTime: true,
    })
  ).toMatchSnapshot('created after timestamp');
  expect(shallowRender({ component: mockComponent() })).toMatchSnapshot('project');
  expect(
    shallowRender({ component: mockComponent({ qualifier: ComponentQualifier.Portfolio }) })
  ).toMatchSnapshot('portfolio');
});

it.each([
  ['week', '1w'],
  ['month', '1m'],
  ['year', '1y'],
])('should render correctly for createdInLast %s', (_, createdInLast) => {
  expect(shallowRender({ component: mockComponent(), createdInLast })).toMatchSnapshot();
});

function shallowRender(props?: Partial<CreationDateFacet['props']>) {
  return shallow<CreationDateFacet>(
    <CreationDateFacet
      component={undefined}
      fetching={false}
      createdAfter={undefined}
      createdAfterIncludesTime={false}
      createdAt=""
      createdBefore={undefined}
      createdInLast=""
      inNewCodePeriod={false}
      intl={
        {
          formatDate: (date: string) => 'formatted.' + date,
        } as IntlShape
      }
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      stats={undefined}
      {...props}
    />
  );
}
