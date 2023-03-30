/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockBranch, mockMainBranch } from '../../../../../../../helpers/mocks/branch-like';
import { renderComponent } from '../../../../../../../helpers/testReactTestingUtils';
import { BranchLike } from '../../../../../../../types/branch-like';
import RegulatoryReport from '../RegulatoryReport';

jest.mock('../../../../../../../api/branches');

let handler: BranchesServiceMock;

beforeAll(() => {
  handler = new BranchesServiceMock();
});

afterEach(() => handler.resetBranches());

describe('RegulatoryReport tests', () => {
  it('should open the regulatory report page', async () => {
    const user = userEvent.setup();
    renderRegulatoryReportApp();
    expect(await screen.findByText('regulatory_report.page')).toBeInTheDocument();
    expect(screen.getByText('regulatory_report.description1')).toBeInTheDocument();
    expect(screen.getByText('regulatory_report.description2')).toBeInTheDocument();
    expect(
      screen.getByText('regulatory_page.available_branches_info.only_keep_when_inactive')
    ).toBeInTheDocument();
    expect(
      screen.getByText('regulatory_page.available_branches_info.more_info')
    ).toBeInTheDocument();

    const branchSelect = screen.getByRole('combobox', { name: 'regulatory_page.select_branch' });
    expect(branchSelect).toBeInTheDocument();

    await user.click(branchSelect);
    await user.keyboard('[ArrowDown][Enter]');

    const downloadButton = screen.getByRole('link', { name: 'download_verb' });
    expect(downloadButton).toBeInTheDocument();

    expect(screen.queryByText('regulatory_page.download_start.sentence')).not.toBeInTheDocument();
    await user.click(downloadButton);
    expect(screen.getByText('regulatory_page.download_start.sentence')).toBeInTheDocument();
  });

  it('should display warning message if there is no available branch', async () => {
    handler.emptyBranches();
    renderRegulatoryReportApp();

    expect(await screen.findByText('regulatory_report.page')).toBeInTheDocument();

    expect(screen.getByText('regulatory_page.no_available_branch')).toBeInTheDocument();

    const downloadButton = screen.getByRole('link', { name: 'download_verb' });
    expect(downloadButton).toBeInTheDocument();
    expect(downloadButton).toHaveClass('disabled');
  });

  it('should automatically select passed branch if compatible', async () => {
    const compatibleBranch = mockBranch({ name: 'compatible-branch' });
    handler.addBranch(compatibleBranch);
    renderRegulatoryReportApp(compatibleBranch);

    expect(await screen.findByText('regulatory_report.page')).toBeInTheDocument();

    const downloadButton = screen.getByRole<HTMLAnchorElement>('link', { name: 'download_verb' });
    expect(downloadButton).toBeInTheDocument();
    expect(downloadButton).not.toHaveClass('disabled');
    expect(downloadButton.href).toContain(compatibleBranch.name);
  });

  it('should automatically select main branch if present and passed branch is not compatible', async () => {
    handler.emptyBranches();
    const mainBranch = mockMainBranch({ name: 'main' });
    const notCompatibleBranch = mockBranch({
      name: 'not-compatible-branch',
      excludedFromPurge: false,
    });
    handler.addBranch(mainBranch);
    handler.addBranch(notCompatibleBranch);
    renderRegulatoryReportApp(notCompatibleBranch);

    expect(await screen.findByText('regulatory_report.page')).toBeInTheDocument();

    const downloadButton = screen.getByRole<HTMLAnchorElement>('link', { name: 'download_verb' });
    expect(downloadButton).toBeInTheDocument();
    expect(downloadButton).not.toHaveClass('disabled');
    expect(downloadButton.href).toContain(mainBranch.name);
  });
});

function renderRegulatoryReportApp(branchLike?: BranchLike) {
  renderComponent(
    <RegulatoryReport
      component={{ key: '', name: '' }}
      branchLike={branchLike}
      onClose={() => {}}
    />
  );
}
