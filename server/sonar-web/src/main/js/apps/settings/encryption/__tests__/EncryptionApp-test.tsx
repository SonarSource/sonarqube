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
import { checkSecretKey, generateSecretKey } from '../../../../api/settings';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import EncryptionApp from '../EncryptionApp';

jest.mock('../../../../api/settings', () => ({
  checkSecretKey: jest.fn().mockResolvedValue({ secretKeyAvailable: true }),
  generateSecretKey: jest.fn().mockResolvedValue({ secretKey: 'secret' }),
}));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');
  expect(wrapper.setState({ loading: false, secretKeyAvailable: false })).toMatchSnapshot(
    'generate form'
  );
  expect(wrapper.setState({ secretKeyAvailable: true })).toMatchSnapshot('encryption form');
});

it('should correctly check a key', async () => {
  const wrapper = shallowRender();
  wrapper.instance().checkSecretKey();
  await waitAndUpdate(wrapper);
  expect(checkSecretKey).toHaveBeenCalled();
  expect(wrapper.state().secretKeyAvailable).toBe(true);
});

it('should correctly generate a key', async () => {
  const wrapper = shallowRender();
  wrapper.instance().generateSecretKey();
  await waitAndUpdate(wrapper);
  expect(generateSecretKey).toHaveBeenCalled();
  expect(wrapper.state().secretKey).toBe('secret');
  expect(wrapper.state().secretKeyAvailable).toBe(false);
});

function shallowRender(props: Partial<EncryptionApp['props']> = {}) {
  return shallow<EncryptionApp>(<EncryptionApp {...props} />);
}
