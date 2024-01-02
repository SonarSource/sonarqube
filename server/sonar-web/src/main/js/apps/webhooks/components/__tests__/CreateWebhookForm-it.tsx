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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byLabelText, byRole } from 'testing-library-selector';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import CreateWebhookForm from '../CreateWebhookForm';

const ui = {
  nameInput: byRole('textbox', { name: 'webhooks.name field_required' }),
  urlInput: byRole('textbox', { name: 'webhooks.url field_required' }),
  secretInput: byLabelText('webhooks.secret'),
  secretInputMaskButton: byRole('button', { name: 'webhooks.secret.field_mask.link' }),
  createButton: byRole('button', { name: 'create' }),
  updateButton: byRole('button', { name: 'update_verb' }),
};

describe('Webhook form', () => {
  it('should correctly submit creation form', async () => {
    const user = userEvent.setup();
    const webhook = {
      name: 'foo',
      url: 'http://bar',
      secret: '',
    };
    const onDone = jest.fn();

    renderCreateWebhookForm({ onDone });

    expect(ui.nameInput.get()).toHaveValue('');
    expect(ui.urlInput.get()).toHaveValue('');
    expect(ui.secretInput.get()).toHaveValue('');
    expect(ui.createButton.get()).toBeDisabled();

    await user.type(ui.nameInput.get(), webhook.name);
    await user.type(ui.urlInput.get(), webhook.url);
    expect(ui.createButton.get()).toBeEnabled();

    await user.click(ui.createButton.get());
    expect(onDone).toHaveBeenCalledWith(webhook);
  });

  it('should correctly submit update form', async () => {
    const user = userEvent.setup();
    const webhook = {
      hasSecret: false,
      key: 'test-webhook-key',
      name: 'foo',
      url: 'http://bar',
    };
    const nameExtension = 'bar';
    const url = 'http://bar';
    const onDone = jest.fn();

    renderCreateWebhookForm({ onDone, webhook });

    expect(ui.nameInput.get()).toHaveValue(webhook.name);
    expect(ui.urlInput.get()).toHaveValue(webhook.url);
    expect(ui.secretInput.query()).not.toBeInTheDocument();
    expect(ui.secretInputMaskButton.get()).toBeInTheDocument();
    expect(ui.updateButton.get()).toBeDisabled();

    await user.type(ui.nameInput.get(), nameExtension);
    await user.clear(ui.urlInput.get());
    await user.type(ui.urlInput.get(), url);
    expect(ui.updateButton.get()).toBeEnabled();

    await user.click(ui.updateButton.get());
    expect(onDone).toHaveBeenCalledWith({
      name: `${webhook.name}${nameExtension}`,
      url,
      secret: undefined,
    });
  });

  it('should correctly submit update form with empty secret', async () => {
    const user = userEvent.setup();
    const webhook = {
      hasSecret: false,
      key: 'test-webhook-key',
      name: 'foo',
      url: 'http://bar',
    };
    const onDone = jest.fn();

    renderCreateWebhookForm({ onDone, webhook });

    await user.click(ui.secretInputMaskButton.get());
    expect(ui.updateButton.get()).toBeEnabled();

    await user.click(ui.updateButton.get());
    expect(onDone).toHaveBeenCalledWith({
      name: webhook.name,
      url: webhook.url,
      secret: '',
    });
  });

  it('should correctly submit update form with updated secret', async () => {
    const user = userEvent.setup();
    const webhook = {
      hasSecret: false,
      key: 'test-webhook-key',
      name: 'foo',
      url: 'http://bar',
    };
    const secret = 'test-webhook-secret';
    const onDone = jest.fn();

    renderCreateWebhookForm({ onDone, webhook });

    await user.click(ui.secretInputMaskButton.get());
    await user.type(ui.secretInput.get(), secret);

    await user.click(ui.updateButton.get());
    expect(onDone).toHaveBeenCalledWith({
      name: webhook.name,
      url: webhook.url,
      secret,
    });
  });
});

function renderCreateWebhookForm(props = {}) {
  return renderComponent(
    <CreateWebhookForm onClose={jest.fn()} onDone={jest.fn(() => Promise.resolve())} {...props} />
  );
}
