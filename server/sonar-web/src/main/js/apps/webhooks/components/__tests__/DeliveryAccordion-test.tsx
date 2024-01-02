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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { getDelivery } from '../../../../api/webhooks';
import { mockWebhookDelivery } from '../../../../helpers/mocks/webhook';
import { HttpStatus } from '../../../../helpers/request';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import DeliveryAccordion from '../DeliveryAccordion';

jest.mock('../../../../api/webhooks', () => ({
  getDelivery: jest.fn().mockResolvedValue({
    delivery: { payload: '{ "message": "This was successful" }' },
  }),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly for successful payloads', async () => {
  const user = userEvent.setup();
  renderDeliveryAccordion();
  expect(screen.getByLabelText('success')).toBeInTheDocument();

  await user.click(screen.getByRole('button'));
  expect(screen.getByText(`webhooks.delivery.response_x.${HttpStatus.Ok}`)).toBeInTheDocument();
  expect(screen.getByText('webhooks.delivery.duration_x.20ms')).toBeInTheDocument();
  expect(screen.getByText('webhooks.delivery.payload')).toBeInTheDocument();

  const codeSnippet = await screen.findByText('{ "message": "This was successful" }');
  expect(codeSnippet).toBeInTheDocument();
});

it('should render correctly for errored payloads', async () => {
  const user = userEvent.setup();
  (getDelivery as jest.Mock).mockResolvedValueOnce({
    delivery: { payload: '503 Service Unavailable' },
  });
  renderDeliveryAccordion({
    delivery: mockWebhookDelivery({ httpStatus: undefined, success: false }),
  });
  expect(screen.getByLabelText('error')).toBeInTheDocument();

  await user.click(screen.getByRole('button'));
  expect(
    screen.getByText('webhooks.delivery.response_x.webhooks.delivery.server_unreachable')
  ).toBeInTheDocument();

  const codeSnippet = await screen.findByText('503 Service Unavailable');
  expect(codeSnippet).toBeInTheDocument();
});

function renderDeliveryAccordion(props: Partial<DeliveryAccordion['props']> = {}) {
  return renderComponent(<DeliveryAccordion delivery={mockWebhookDelivery()} {...props} />);
}
