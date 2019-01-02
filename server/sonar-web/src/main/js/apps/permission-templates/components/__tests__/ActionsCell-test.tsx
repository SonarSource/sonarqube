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
import { ActionsCell } from '../ActionsCell';

const SAMPLE = {
  createdAt: '2018-01-01',
  id: 'id',
  name: 'name',
  permissions: [],
  defaultFor: []
};

function renderActionsCell(props?: Partial<ActionsCell['props']>) {
  return shallow(
    <ActionsCell
      permissionTemplate={SAMPLE}
      refresh={() => true}
      router={{ replace: jest.fn() }}
      topQualifiers={['TRK', 'VW']}
      {...props}
    />
  );
}

it('should set default', () => {
  const setDefault = renderActionsCell().find('.js-set-default');
  expect(setDefault.length).toBe(2);
  expect(setDefault.at(0).prop('data-qualifier')).toBe('TRK');
  expect(setDefault.at(1).prop('data-qualifier')).toBe('VW');
});

it('should not set default', () => {
  const permissionTemplate = { ...SAMPLE, defaultFor: ['TRK', 'VW'] };
  const setDefault = renderActionsCell({ permissionTemplate }).find('.js-set-default');
  expect(setDefault.length).toBe(0);
});

it('should display all qualifiers for default organization', () => {
  const organization = { isDefault: true, key: 'org' };
  const setDefault = renderActionsCell({ organization }).find('.js-set-default');
  expect(setDefault.length).toBe(2);
  expect(setDefault.at(0).prop('data-qualifier')).toBe('TRK');
  expect(setDefault.at(1).prop('data-qualifier')).toBe('VW');
});

it('should display only projects for custom organization', () => {
  const organization = { isDefault: false, key: 'org' };
  const setDefault = renderActionsCell({ organization }).find('.js-set-default');
  expect(setDefault.length).toBe(1);
  expect(setDefault.at(0).prop('data-qualifier')).toBe('TRK');
});
