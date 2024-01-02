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
import { sendTestEmail } from '../../../../api/settings';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { EmailForm } from '../EmailForm';

jest.mock('../../../../helpers/request', () => ({
  parseError: jest.fn().mockResolvedValue('Error message'),
}));

jest.mock('../../../../api/settings', () => ({
  sendTestEmail: jest.fn().mockResolvedValue(null),
}));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('default');
  wrapper.setState({ loading: true });
  expect(wrapper).toMatchSnapshot('sending');
  wrapper.setState({ loading: false, success: 'email@example.com' });
  expect(wrapper).toMatchSnapshot('success');
  wrapper.setState({ success: undefined, error: 'Some error message' });
  expect(wrapper).toMatchSnapshot('error');
});

it('should correctly control the inputs', () => {
  const wrapper = shallowRender();

  change(wrapper.find('#test-email-to'), 'new@recipient.com');
  expect(wrapper.state().recipient).toBe('new@recipient.com');

  change(wrapper.find('#test-email-subject'), 'New subject');
  expect(wrapper.state().subject).toBe('New subject');

  change(wrapper.find('#test-email-message'), 'New message');
  expect(wrapper.state().message).toBe('New message');
});

it('should correctly test the email sending', async () => {
  const wrapper = shallowRender();

  submit(wrapper.find('form'));
  expect(sendTestEmail).toHaveBeenCalledWith(
    'luke@skywalker.biz',
    'email_configuration.test.subject',
    'email_configuration.test.message_text'
  );
  expect(wrapper.state().loading).toBe(true);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().error).toBeUndefined();
  expect(wrapper.state().success).toBe('luke@skywalker.biz');

  (sendTestEmail as jest.Mock).mockRejectedValueOnce(null);

  submit(wrapper.find('form'));

  await waitAndUpdate(wrapper);

  expect(wrapper.state().success).toBeUndefined();
  expect(wrapper.state().error).toBe('Error message');
});

function shallowRender(props: Partial<EmailForm['props']> = {}) {
  return shallow<EmailForm>(
    <EmailForm currentUser={mockLoggedInUser({ email: 'luke@skywalker.biz' })} {...props} />
  );
}
