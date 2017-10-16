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
import EditionBoxes from '../EditionBoxes';
import { EditionStatus } from '../../../api/marketplace';

const DEFAULT_STATUS: EditionStatus = {
  currentEditionKey: 'foo',
  nextEditionKey: '',
  installationStatus: 'NONE'
};

it('should display the edition boxes', () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({
    editions: {
      foo: {
        name: 'Foo',
        desc: 'Foo desc',
        download_link: 'download_url',
        more_link: 'more_url',
        request_license_link: 'license_url'
      },
      bar: {
        name: 'Bar',
        desc: 'Bar desc',
        download_link: 'download_url',
        more_link: 'more_url',
        request_license_link: 'license_url'
      }
    },
    loading: false
  });
  expect(wrapper).toMatchSnapshot();
});

it('should display an error message', () => {
  const wrapper = getWrapper();
  wrapper.setState({ loading: false, editionsError: true });
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <EditionBoxes editionStatus={DEFAULT_STATUS} updateCenterActive={true} {...props} />
  );
}
