/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Edition, EditionStatus } from '../../../../api/marketplace';
import EditionBox from '../EditionBox';

const DEFAULT_STATUS: EditionStatus = {
  currentEditionKey: '',
  nextEditionKey: '',
  installationStatus: 'NONE'
};

const DEFAULT_EDITION: Edition = {
  name: 'Foo',
  desc: 'Foo desc',
  download_link: 'download_url',
  more_link: 'more_url',
  request_license_link: 'license_url'
};

it('should display the edition', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should display installed badge', () => {
  expect(
    getWrapper({
      editionStatus: {
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
      editionStatus: {
        currentEditionKey: 'foo',
        nextEditionKey: 'foo',
        installationStatus: 'NONE'
      }
    })
  ).toMatchSnapshot();
});

it('should disable install button', () => {
  expect(
    getWrapper({
      editionStatus: {
        currentEditionKey: 'foo',
        nextEditionKey: '',
        installationStatus: 'AUTOMATIC_IN_PROGRESS'
      }
    })
  ).toMatchSnapshot();
});

it('should disable uninstall button', () => {
  expect(
    getWrapper({
      editionStatus: {
        currentEditionKey: '',
        nextEditionKey: 'foo',
        installationStatus: 'AUTOMATIC_IN_PROGRESS'
      }
    })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <EditionBox
      edition={DEFAULT_EDITION}
      editionKey="foo"
      editionStatus={DEFAULT_STATUS}
      {...props}
    />
  );
}
