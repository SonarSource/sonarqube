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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import WebhooksMock from '../../../../api/mocks/WebhooksMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockWebhook } from '../../../../helpers/mocks/webhook';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { App, AppProps } from '../App';
import { WEBHOOKS_LIMIT } from '../PageActions';

const webhookService = new WebhooksMock();

beforeEach(() => {
  webhookService.reset();
});

describe('app should render correctly', () => {
  it('global webhooks', async () => {
    const { ui } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    expect(ui.webhookTable.get()).toBeInTheDocument();
    ui.checkWebhookRow(0, {
      name: 'Global webhook 1',
      url: 'https://example.com/1',
      secret: false,
      lastDeliveryDate: 'June 24, 2019',
    });
    ui.checkWebhookRow(1, {
      name: 'Global webhook 2',
      url: 'https://example.com/2',
      secret: true,
    });
  });

  it('project webhooks', async () => {
    const { ui } = getPageObject();
    webhookService.reset('project1');
    renderWebhooksApp({
      component: mockComponent({ key: 'project1' }),
    });
    await ui.waitForWebhooksLoaded();

    expect(ui.webhookTable.get()).toBeInTheDocument();
    ui.checkWebhookRow(0, {
      name: 'Project 1 webhook 1',
      url: 'https://example.com/1',
      secret: false,
      lastDeliveryDate: 'June 24, 2019',
    });
    ui.checkWebhookRow(1, {
      name: 'Project 1 webhook 2',
      url: 'https://example.com/2',
      secret: false,
    });
  });

  it('project with no webhook', async () => {
    const { ui } = getPageObject();
    webhookService.reset('project2');
    renderWebhooksApp({
      component: mockComponent({ key: 'project2' }),
    });
    await ui.waitForNoResults();

    expect(ui.webhookTable.query()).not.toBeInTheDocument();
  });
});

describe('webhook CRUD', () => {
  it('should not allow webhook creation when too many', async () => {
    const { ui } = getPageObject();
    webhookService.addWebhook(
      ...Array.from({ length: 8 }).map((_, i) => mockWebhook({ key: `newwebhook${i}` })),
    );
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    expect(ui.webhookCreateButton.get()).toBeDisabled();
    await expect(ui.webhookCreateButton.get()).toHaveATooltipWithContent(
      `webhooks.maximum_reached.${WEBHOOKS_LIMIT}`,
    );
  });

  // eslint-disable-next-line jest/expect-expect
  it('should allow to create a webhook', async () => {
    const { ui, user } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    await user.click(ui.webhookCreateButton.get());
    await ui.fillUpdateForm('new-webhook');
    ui.formShouldNotBeValid();
    await ui.fillUpdateForm(undefined, 'https://webhook.example.sonarqube.com');
    await ui.submitForm();

    ui.checkWebhookRow(2, {
      name: 'new-webhook',
      url: 'https://webhook.example.sonarqube.com',
      secret: false,
    });
  });

  // eslint-disable-next-line jest/expect-expect
  it('should allow to update a webhook', async () => {
    const { ui } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    await ui.clickWebhookRowAction(1, 'Global webhook 2', 'update_verb', 'menuitem');
    await ui.fillUpdateForm('modified-webhook', 'https://webhook.example.sonarqube.com', 'secret');
    await ui.submitForm();

    ui.checkWebhookRow(1, {
      name: 'modified-webhook',
      url: 'https://webhook.example.sonarqube.com',
      secret: true,
      lastDeliveryDate: 'webhooks.last_execution.none',
    });

    // Edit again, removing the secret
    await ui.clickWebhookRowAction(1, 'modified-webhook', 'update_verb', 'menuitem');
    await ui.fillUpdateForm(undefined, undefined, '');
    await ui.submitForm();

    ui.checkWebhookRow(1, {
      name: 'modified-webhook',
      url: 'https://webhook.example.sonarqube.com',
      secret: false,
      lastDeliveryDate: 'webhooks.last_execution.none',
    });

    // Edit once again, not touching the secret
    await ui.clickWebhookRowAction(1, 'modified-webhook', 'update_verb', 'menuitem');
    await ui.fillUpdateForm('modified-webhook2');
    await ui.submitForm();

    ui.checkWebhookRow(1, {
      name: 'modified-webhook',
      url: 'https://webhook.example.sonarqube.com',
      secret: false,
      lastDeliveryDate: 'webhooks.last_execution.none',
    });
  });

  it('should allow to delete a webhook', async () => {
    const { ui } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    expect(ui.webhookRow.getAll()).toHaveLength(3); // We count the header
    await ui.clickWebhookRowAction(0, 'Global webhook 1', 'delete', 'menuitem');
    await ui.submitForm();
    expect(ui.webhookRow.getAll()).toHaveLength(2);
  });
});

describe('should properly show deliveries', () => {
  it('should render delivery list', async () => {
    const { ui } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    await ui.clickWebhookRowAction(0, 'Global webhook 1', 'webhooks.deliveries.show', 'menuitem');
    ui.checkDeliveryRow(1, {
      date: 'June 24, 2019',
      status: 'success',
    });
    ui.checkDeliveryRow(2, {
      date: 'June 23, 2019',
      status: 'success',
    });
    ui.checkDeliveryRow(3, {
      date: 'June 22, 2019',
      status: 'error',
    });
    ui.checkDeliveryRow(4, {
      date: 'June 21, 2019',
      status: 'success',
    });

    await ui.toggleDeliveryRow(2);
    expect(screen.getByText('webhooks.delivery.response_x.200')).toBeInTheDocument();
    expect(screen.getByText('webhooks.delivery.duration_x.1s')).toBeInTheDocument();
    expect(screen.getByText('{ "id": "global-webhook-1-delivery-0" }')).toBeInTheDocument();

    await ui.toggleDeliveryRow(2);
    await ui.toggleDeliveryRow(3);
    expect(
      screen.getByText('webhooks.delivery.response_x.webhooks.delivery.server_unreachable'),
    ).toBeInTheDocument();
    expect(screen.getByText('webhooks.delivery.duration_x.1s')).toBeInTheDocument();
    expect(screen.getByText('{ "id": "global-webhook-1-delivery-1" }')).toBeInTheDocument();
  });

  it('should render delivery modal', async () => {
    const { ui } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    await ui.clickWebhookLatestDelivery(0, 'Global webhook 1');
    expect(screen.getByText('webhooks.delivery.response_x.200')).toBeInTheDocument();
    expect(screen.getByText('webhooks.delivery.duration_x.20ms')).toBeInTheDocument();
    expect(screen.getByText('{ "id": "global-delivery1" }')).toBeInTheDocument();
  });

  it('should handle pagination', async () => {
    const { ui, user } = getPageObject();
    renderWebhooksApp();
    await ui.waitForWebhooksLoaded();

    await ui.clickWebhookRowAction(0, 'Global webhook 1', 'webhooks.deliveries.show', 'menuitem');
    expect(screen.getByText('x_of_y_shown.10.16')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'show_more' }));
    expect(screen.getByText('x_of_y_shown.16.16')).toBeInTheDocument();
  });
});

function renderWebhooksApp(overrides: Partial<AppProps> = {}) {
  return renderComponent(<App {...overrides} />);
}

function getPageObject() {
  const user = userEvent.setup();

  const selectors = {
    webhookCreateButton: byRole('button', { name: 'create' }),
    webhookTable: byRole('table'),
    webhookRow: byRole('row'),
    noWebhook: byText('webhooks.no_result'),
    formDialog: byRole('dialog'),
    formNameInput: byRole('textbox', { name: 'webhooks.name field_required' }),
    formUrlInput: byRole('textbox', { name: 'webhooks.url field_required' }),
    formSecretInput: byLabelText(/webhooks.secret/),
    formSecretInputMaskButton: byRole('link', { name: 'webhooks.secret.field_mask.link' }),
    formUpdateButton: byRole('menuitem', { name: 'update_verb' }),
  };

  const ui = {
    ...selectors,

    // General
    waitForWebhooksLoaded: async () => {
      expect(await selectors.webhookTable.find()).toBeInTheDocument();
    },
    waitForNoResults: async () => {
      expect(await selectors.noWebhook.find()).toBeInTheDocument();
    },

    // Row-related
    getWebhookRow: (index: number) => {
      return selectors.webhookRow.getAll()[index + 1];
    },
    clickWebhookRowAction: async (
      rowIndex: number,
      webhookName: string,
      actionName: string,
      role: string = 'button',
    ) => {
      const row = ui.getWebhookRow(rowIndex);
      await user.click(
        within(row).getByRole('button', { name: `webhooks.show_actions.${webhookName}` }),
      );
      await user.click(within(row).getByRole(role, { name: actionName }));
    },
    clickWebhookLatestDelivery: async (rowIndex: number, webhookName: string) => {
      const row = ui.getWebhookRow(rowIndex);
      await user.click(
        within(row).getByRole('button', {
          name: `webhooks.last_execution.open_for_x.${webhookName}`,
        }),
      );
    },
    checkWebhookRow: (
      index: number,
      expected: { lastDeliveryDate?: string; name: string; secret: boolean; url: string },
    ) => {
      const row = ui.getWebhookRow(index);
      const [name, url, secret, lastDelivery] = within(row).getAllByRole('cell');
      expect(name).toHaveTextContent(expected.name);
      expect(url).toHaveTextContent(expected.url);
      expect(secret).toHaveTextContent(expected.secret ? 'yes' : 'no');
      expect(lastDelivery).toHaveTextContent(
        expected.lastDeliveryDate
          ? new RegExp(expected.lastDeliveryDate)
          : 'webhooks.last_execution.none',
      );
    },

    // Creation/Update form
    fillUpdateForm: async (name?: string, url?: string, secret?: string) => {
      if (name !== undefined) {
        await user.clear(selectors.formNameInput.get());
        await user.type(selectors.formNameInput.get(), name);
      }
      if (url !== undefined) {
        await user.clear(selectors.formUrlInput.get());
        await user.type(selectors.formUrlInput.get(), url);
      }
      if (secret !== undefined) {
        // Do we have to update secret
        const secretMaskButton = selectors.formSecretInputMaskButton.get();
        if (secretMaskButton) {
          await user.click(secretMaskButton);
        }
        await user.clear(selectors.formSecretInput.get());
        if (secret) {
          await user.type(selectors.formSecretInput.get(), secret);
        }
      }
    },
    getFormSubmitButton: () => {
      const form = selectors.formDialog.get();
      return within(form)
        .getAllByRole('button')
        .filter((b: HTMLButtonElement) => b.type === 'submit')[0];
    },
    formShouldNotBeValid: () => {
      expect(ui.getFormSubmitButton()).toBeDisabled();
    },
    submitForm: async () => {
      const submitBtn = ui.getFormSubmitButton();
      await user.click(submitBtn);
      await waitFor(() => expect(selectors.formDialog.query()).not.toBeInTheDocument());
    },

    // Deliveries
    getDeliveryRow: (index: number) => {
      const dialog = selectors.formDialog.get();
      const rows = within(dialog).getAllByRole('heading');
      return rows[index];
    },
    checkDeliveryRow: (index: number, expected: { date: string; status: 'success' | 'error' }) => {
      const row = ui.getDeliveryRow(index);
      expect(row).toHaveTextContent(new RegExp(expected.date));
      const status = within(row).getByLabelText(expected.status);
      expect(status).toBeInTheDocument();
    },
    toggleDeliveryRow: async (index: number) => {
      const row = ui.getDeliveryRow(index);
      await user.click(within(row).getByRole('button'));
    },
  };

  return {
    ui,
    user,
  };
}
