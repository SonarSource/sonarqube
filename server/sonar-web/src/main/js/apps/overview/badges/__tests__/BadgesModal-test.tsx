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
import BadgesModal from '../BadgesModal';
import { click } from '../../../../helpers/testUtils';
import { isSonarCloud } from '../../../../helpers/system';
import { Location } from '../../../../helpers/urls';

jest.mock('../../../../helpers/urls', () => ({
  getHostUrl: () => 'host',
  getProjectUrl: () => ({ pathname: '/dashboard' } as Location),
  getPathUrlAsString: (l: Location) => l.pathname
}));
jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

const shortBranch: T.ShortLivingBranch = {
  isMain: false,
  mergeBranch: '',
  name: 'branch-6.6',
  type: 'SHORT'
};

it('should display the modal after click on sonarcloud', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const wrapper = shallow(
    <BadgesModal branchLike={shortBranch} metrics={{}} project="foo" qualifier="TRK" />
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('Button'));
  expect(wrapper.find('Modal')).toMatchSnapshot();
});

it('should display the modal after click on sonarqube', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);
  const wrapper = shallow(
    <BadgesModal branchLike={shortBranch} metrics={{}} project="foo" qualifier="TRK" />
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('Button'));
  expect(wrapper.find('Modal')).toMatchSnapshot();
});
