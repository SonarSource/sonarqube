/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { renderAdminApp } from '../../../../helpers/testReactTestingUtils';
import { screen } from '@testing-library/react';
import { AdminPageExtension } from '../../../../types/extension';
import routes from '../../routes';
import userEvent from '@testing-library/user-event';
import AuditLogsServiceMock from '../../../../api/mocks/AuditLogsServiceMock';

jest.mock('../../../../api/settings');

const extensions = [
  { key: AdminPageExtension.GovernanceConsole, name: 'Portfolios' },
  { key: 'license/app', name: 'License Manager' },
  { key: 'license/support', name: 'Support' }
];

let handler: AuditLogsServiceMock;

beforeAll(() => {
  handler = new AuditLogsServiceMock();
});

afterEach(() => handler.resetSettingvalues());

jest.setTimeout(30_000);

it('should render audit logs page correctly', async () => {
  renderAuditLogs();
  expect(await screen.findByText('audit_logs.page')).toBeInTheDocument();
});

it('should handle download button click', async () => {
  const user = userEvent.setup();
  renderAuditLogs();
  const downloadButton = await screen.findByText('download_verb');
  expect(downloadButton).toBeInTheDocument();

  await user.click(downloadButton);

  expect(await screen.findByText('download_verb')).toHaveAttribute('href', '#');
  expect(screen.getByText('audit_logs.download_start.sentence.1')).toBeInTheDocument();
  expect(screen.getByText('audit_logs.download_start.sentence.2')).toBeInTheDocument();
  expect(screen.getByText('audit_logs.download_start.sentence.3')).toBeInTheDocument();
});

function renderAuditLogs() {
  renderAdminApp('admin/audit', routes, {}, { adminPages: extensions });
}
