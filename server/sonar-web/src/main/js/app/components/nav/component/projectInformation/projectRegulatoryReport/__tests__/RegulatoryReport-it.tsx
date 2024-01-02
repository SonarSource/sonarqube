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
import BranchesServiceMock from '../../../../../../../api/mocks/BranchesServiceMock';
import { renderComponent } from '../../../../../../../helpers/testReactTestingUtils';
import RegulatoryReport from '../RegulatoryReport';

jest.mock('../../../../../../../api/branches');

let handler: BranchesServiceMock;

beforeAll(() => {
  handler = new BranchesServiceMock();
});

afterEach(() => handler.resetBranches());

it('should open the regulatory report page', async () => {
  const user = userEvent.setup();
  renderRegulatoryReportApp();
  expect(await screen.findByText('regulatory_report.page')).toBeInTheDocument();
  expect(screen.getByText('regulatory_report.description1')).toBeInTheDocument();
  expect(screen.getByText('regulatory_report.description2')).toBeInTheDocument();

  const branchSelect = screen.getByRole('textbox');
  expect(branchSelect).toBeInTheDocument();

  await user.click(branchSelect);
  await user.keyboard('[ArrowDown][Enter]');

  const downloadButton = screen.getByText('download_verb');
  expect(downloadButton).toBeInTheDocument();

  expect(screen.queryByText('regulatory_page.download_start.sentence')).not.toBeInTheDocument();
  await user.click(downloadButton);
  expect(screen.getByText('regulatory_page.download_start.sentence')).toBeInTheDocument();
});

function renderRegulatoryReportApp() {
  renderComponent(<RegulatoryReport component={{ key: '', name: '' }} onClose={() => {}} />);
}
