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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { Location } from 'sonar-ui-common/helpers/urls';
import { isSonarCloud } from '../../../../helpers/system';
import BadgesModal from '../BadgesModal';

jest.mock('sonar-ui-common/helpers/urls', () => ({
  getHostUrl: () => 'host',
  getPathUrlAsString: (l: Location) => l.pathname
}));

jest.mock('../../../../helpers/urls', () => ({
  getProjectUrl: () => ({ pathname: '/dashboard' } as Location)
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
