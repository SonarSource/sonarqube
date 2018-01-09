/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { EditionStatus } from '../../../../api/marketplace';
import EditionBoxBadge from '../EditionBoxBadge';

const DEFAULT_STATUS: EditionStatus = {
  currentEditionKey: '',
  nextEditionKey: '',
  installationStatus: 'NONE'
};

it('should display installed badge', () => {
  expect(
    getWrapper({
      status: {
        currentEditionKey: 'foo',
        nextEditionKey: '',
        installationStatus: 'NONE'
      }
    })
  ).toMatchSnapshot();
});

it('should display installing badge', () => {
  expect(
    getWrapper({
      status: {
        currentEditionKey: 'foo',
        nextEditionKey: 'foo',
        installationStatus: 'AUTOMATIC_IN_PROGRESS'
      }
    })
  ).toMatchSnapshot();
});

it('should display pending badge', () => {
  expect(
    getWrapper({
      status: {
        currentEditionKey: '',
        nextEditionKey: 'foo',
        installationStatus: 'AUTOMATIC_READY'
      }
    })
  ).toMatchSnapshot();
});

it('should not display a badge', () => {
  expect(
    getWrapper({
      status: {
        currentEditionKey: '',
        nextEditionKey: 'bar',
        installationStatus: 'AUTOMATIC_READY'
      }
    }).type()
  ).toBeNull();
});

function getWrapper(props = {}) {
  return shallow(<EditionBoxBadge editionKey="foo" status={DEFAULT_STATUS} {...props} />);
}
