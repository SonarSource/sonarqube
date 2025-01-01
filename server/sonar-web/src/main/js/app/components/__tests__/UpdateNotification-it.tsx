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
import { addDays, formatISO, subDays } from 'date-fns';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { getSystemUpgrades } from '../../../api/system';
import { UpdateUseCase } from '../../../components/upgrade/utils';
import { mockAppState, mockCurrentUser } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import { EditionKey } from '../../../types/editions';
import { Permissions } from '../../../types/permissions';
import { ProductNameForUpgrade } from '../../../types/system';
import { CurrentUser } from '../../../types/users';
import { AppStateContext } from '../app-state/AppStateContext';
import { CurrentUserContext } from '../current-user/CurrentUserContext';
import { UpdateNotification } from '../update-notification/UpdateNotification';

jest.mock('../../../api/system', () => ({
  getSystemUpgrades: jest.fn(),
}));

const ui = {
  closeDialogBtn: byRole('button', { name: 'cancel' }),
  dialog: byRole('dialog', { name: 'system.system_upgrade' }),
  dialogErrorMessage: byRole('dialog').byText('admin_notification.update.current_version_inactive'),
  dialogWarningMessage: byRole('dialog').byText('admin_notification.update.new_patch'),
  hideIntermediateBtn: byRole('button', { name: 'system.hide_intermediate_versions' }),
  intermediateRegion: byRole('region', { name: 'system.hide_intermediate_versions' }),
  latestHeader: byRole('heading', { name: /system.latest_version/ }),
  latestLTAHeader: byRole('heading', { name: /system.lta_version/ }),
  learnMoreLink: byRole('link', { name: /learn_more/ }),
  openDialogBtn: byRole('button', { name: 'learn_more' }),
  patchHeader: byRole('heading', { name: /system.latest_patch/ }),
  showIntermediateBtn: byRole('button', { name: 'system.show_intermediate_versions' }),
  updateMessage: byRole('alert'),
};

const SQCBUpgrade = { downloadUrl: '', product: ProductNameForUpgrade.SonarQubeCommunityBuild };
const SQSUpgrade = { downloadUrl: '', product: ProductNameForUpgrade.SonarQubeServer };

describe('when running SQS', () => {
  it('should not render update notification if user is not logged in', () => {
    renderUpdateNotification(undefined, { isLoggedIn: false });
    expect(getSystemUpgrades).not.toHaveBeenCalled();
    expect(ui.updateMessage.query()).not.toBeInTheDocument();
  });

  it('should not render update notification if user is not admin', () => {
    renderUpdateNotification(undefined, { permissions: { global: [] } });
    expect(getSystemUpgrades).not.toHaveBeenCalled();
    expect(ui.updateMessage.query()).not.toBeInTheDocument();
  });

  it('should not render update notification if no upgrades', () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    renderUpdateNotification();
    expect(getSystemUpgrades).toHaveBeenCalled();
    expect(ui.updateMessage.query()).not.toBeInTheDocument();
  });

  it('should show error message if upgrades call failed and the version has reached eol', async () => {
    jest.mocked(getSystemUpgrades).mockReturnValue(Promise.reject(new Error('error')));
    renderUpdateNotification(undefined, undefined, {
      versionEOL: formatISO(subDays(new Date(), 1), { representation: 'date' }),
    });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    expect(ui.openDialogBtn.query()).not.toBeInTheDocument();
    expect(ui.learnMoreLink.get()).toBeInTheDocument();
  });

  it('should not show the notification banner if there is no network connection and version has not reached the eol', () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [],
    });
    renderUpdateNotification(undefined, undefined, {
      versionEOL: formatISO(addDays(new Date(), 1), { representation: 'date' }),
    });
    expect(ui.updateMessage.query()).not.toBeInTheDocument();
  });

  it('should show the error banner if there is no network connection and version has reached the eol', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [],
    });
    renderUpdateNotification(undefined, undefined, {
      versionEOL: formatISO(subDays(new Date(), 1), { representation: 'date' }),
    });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    expect(ui.openDialogBtn.query()).not.toBeInTheDocument();
    expect(ui.learnMoreLink.get()).toBeInTheDocument();
  });

  it('active / latest / patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '10.5.1' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.5.1');
    expect(ui.patchHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / latest / several patches', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '10.5.2', releaseDate: '2021-08-02' },
        { ...SQSUpgrade, version: '10.5.1', releaseDate: '2021-08-01' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.5.2');
    expect(ui.dialog.get()).not.toHaveTextContent('10.5.1');
    expect(ui.patchHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.get()).toBeInTheDocument();
    await user.click(ui.showIntermediateBtn.get());
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
    expect(ui.hideIntermediateBtn.get()).toBeInTheDocument();
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.5.1August 1, 2021');
  });

  it('active / latest / new minor', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '10.6.0' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewVersion}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / latest / new minor + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '10.5.1', releaseDate: '2021-08-01' },
        { ...SQSUpgrade, version: '10.6.0', releaseDate: '2021-08-02' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewVersion}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.query()).not.toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.6.0');
    expect(ui.dialog.get()).not.toHaveTextContent('10.5.1');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.get()).toBeInTheDocument();
    await user.click(ui.showIntermediateBtn.get());
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.5.1August 1, 2021');
  });

  it('no longer active / latest / new minor', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '10.6.0', releaseDate: '2021-08-02' },
        { ...SQSUpgrade, version: '10.7.0', releaseDate: '2021-08-03' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: false,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogErrorMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.7.0');
    expect(ui.dialog.get()).not.toHaveTextContent('10.6.0');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.get()).toBeInTheDocument();
    await user.click(ui.showIntermediateBtn.get());
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.6.0August 2, 2021');
  });

  it('no longer active / latest / new minor + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '10.5.1', releaseDate: '2021-08-01' },
        { ...SQSUpgrade, version: '10.6.0', releaseDate: '2021-08-02' },
        { ...SQSUpgrade, version: '10.7.0', releaseDate: '2021-08-03' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: false,
    });
    const user = userEvent.setup();
    renderUpdateNotification();
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogErrorMessage.get()).toBeInTheDocument();
    expect(ui.dialogWarningMessage.query()).not.toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.7.0');
    expect(ui.dialog.get()).not.toHaveTextContent('10.6.0');
    expect(ui.dialog.get()).not.toHaveTextContent('10.5.1');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.get()).toBeInTheDocument();
    await user.click(ui.showIntermediateBtn.get());
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.6.0August 2, 2021');
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.5.1August 1, 2021');
  });

  it('active / lta / patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '9.9.1' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '9.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('9.9.1');
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    // If the current version is an LTA version, we don't show Patch header, we show Latest LTA header
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / lta / new minor', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '10.0.0' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '9.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewVersion}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.query()).not.toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.0.0');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.latestLTAHeader.query()).not.toBeInTheDocument();
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / lta / new minor + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '9.9.1' },
        { ...SQSUpgrade, version: '10.0.0' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '9.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('10.0.0');
    expect(ui.dialog.get()).toHaveTextContent('9.9.1');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    // If the current version is an LTA version, we don't show Patch header, we show Latest LTA header
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / prev lta / new lta + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '8.9.1' },
        { ...SQSUpgrade, version: '9.9.0' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '8.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('8.9.1');
    expect(ui.dialog.get()).toHaveTextContent('9.9.0');
    expect(ui.latestHeader.query()).not.toBeInTheDocument();
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / prev lta / new lta + new minor + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '8.9.1' },
        { ...SQSUpgrade, version: '9.9.0' },
        { ...SQSUpgrade, version: '10.0.0' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '8.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.NewPatch}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogWarningMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('8.9.1');
    expect(ui.dialog.get()).toHaveTextContent('9.9.0');
    expect(ui.dialog.get()).toHaveTextContent('10.0.0');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('active / no uogrades', () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    renderUpdateNotification(undefined, undefined, { version: '8.9.0' });

    expect(screen.queryByText('admin_notification.update')).not.toBeInTheDocument();
  });

  it('no longer active / prev lta / new lta', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '9.9.0' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: false,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '8.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogErrorMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('9.9.0');
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('no longer active / prev lta / new lta + patch', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '8.9.1' },
        { ...SQSUpgrade, version: '9.9.0' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: false,
    });
    const user = userEvent.setup();
    renderUpdateNotification(undefined, undefined, { version: '8.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogErrorMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('9.9.0');
    expect(ui.dialog.get()).not.toHaveTextContent('8.9.1');
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.query()).not.toBeInTheDocument();
  });

  it('no longer active / prev lta / new lta + patch + new minors', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQSUpgrade, version: '8.9.1', releaseDate: '2021-08-01' },
        { ...SQSUpgrade, version: '9.9.0', releaseDate: '2021-08-02' },
        { ...SQSUpgrade, version: '9.9.1', releaseDate: '2021-08-03' },
        { ...SQSUpgrade, version: '10.0.0', releaseDate: '2021-08-04' },
        { ...SQSUpgrade, version: '10.1.0', releaseDate: '2021-08-05' },
        { ...SQSUpgrade, version: '10.1.1', releaseDate: '2021-08-06' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: false,
    });
    const user = userEvent.setup();
    renderUpdateNotification(true, undefined, { version: '8.9.0' });
    expect(await ui.updateMessage.find()).toHaveTextContent(
      `admin_notification.update.${UpdateUseCase.CurrentVersionInactive}`,
    );
    await user.click(ui.openDialogBtn.get());
    expect(ui.dialogErrorMessage.get()).toBeInTheDocument();
    expect(ui.dialog.get()).toHaveTextContent('9.9.1');
    expect(ui.dialog.get()).not.toHaveTextContent('9.9.0');
    expect(ui.dialog.get()).toHaveTextContent('10.1.1');
    expect(ui.dialog.get()).not.toHaveTextContent('10.1.0');
    expect(ui.dialog.get()).not.toHaveTextContent('10.0.0');
    expect(ui.dialog.get()).not.toHaveTextContent('8.9.1');
    expect(ui.latestHeader.get()).toBeInTheDocument();
    expect(ui.latestLTAHeader.get()).toBeInTheDocument();
    expect(ui.patchHeader.query()).not.toBeInTheDocument();
    expect(ui.showIntermediateBtn.getAll()).toHaveLength(2);
    await user.click(ui.showIntermediateBtn.getAt(0));
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.1.0August 5, 2021');
    expect(ui.intermediateRegion.get()).toHaveTextContent('10.0.0August 4, 2021');
    expect(ui.intermediateRegion.get()).not.toHaveTextContent('9.9.0');
    await user.click(ui.hideIntermediateBtn.get());
    await user.click(ui.showIntermediateBtn.getAt(1));
    expect(ui.intermediateRegion.get()).toHaveTextContent('9.9.0August 2, 2021');
    expect(ui.intermediateRegion.get()).not.toHaveTextContent('10.1.0');
    expect(ui.intermediateRegion.get()).not.toHaveTextContent('10.0.0');
  });
});

describe('when running SQCB', () => {
  it('should not render update notification if no upgrades', () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    renderUpdateNotification(undefined, undefined, { edition: EditionKey.community });

    expect(getSystemUpgrades).toHaveBeenCalled();

    expect(ui.updateMessage.query()).not.toBeInTheDocument();
  });

  it('SQCB upgrade available, no SQS upgrade', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQCBUpgrade, version: '25.2' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    renderUpdateNotification(undefined, undefined, { edition: EditionKey.community });

    expect(await ui.updateMessage.find()).toHaveTextContent(
      'admin_notification.update.new_sqcb_version',
    );

    expect(
      screen.queryByText('admin_notification.update.new_sqs_version_when_running_sqcb.banner'),
    ).not.toBeInTheDocument();
  });

  it('non-patch SQS upgrade available, no SQCB upgrade', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '2025.2' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    const user = userEvent.setup();

    renderUpdateNotification(undefined, undefined, { edition: EditionKey.community });

    expect(
      screen.queryByText('admin_notification.update.new_sqcb_version'),
    ).not.toBeInTheDocument();

    expect(await ui.updateMessage.find()).toHaveTextContent(
      'admin_notification.update.new_sqs_version_when_running_sqcb.banner',
    );

    await user.click(ui.openDialogBtn.get());

    expect(
      await screen.findByText(
        'admin_notification.update.new_sqs_version_when_running_sqcb.upgrade',
      ),
    ).toBeInTheDocument();

    await user.click(ui.closeDialogBtn.get());

    expect(
      screen.queryByText('admin_notification.update.new_sqs_version_when_running_sqcb.upgrade'),
    ).not.toBeInTheDocument();
  });

  it('only patch SQS upgrade available, no SQCB upgrade', () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [{ ...SQSUpgrade, version: '2025.2.3' }],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    renderUpdateNotification(undefined, undefined, { edition: EditionKey.community });

    expect(
      screen.queryByText('admin_notification.update.new_sqcb_version'),
    ).not.toBeInTheDocument();

    expect(
      screen.queryByText('admin_notification.update.new_sqs_version_when_running_sqcb.banner'),
    ).not.toBeInTheDocument();
  });

  it('both SQCB and non-patch-SQS upgrades available', async () => {
    jest.mocked(getSystemUpgrades).mockResolvedValue({
      upgrades: [
        { ...SQCBUpgrade, version: '25.4' },
        { ...SQSUpgrade, version: '2025.4' },
      ],
      latestLTA: '9.9',
      updateCenterRefresh: '',
      installedVersionActive: true,
    });

    const user = userEvent.setup();

    renderUpdateNotification(true, undefined, { edition: EditionKey.community });

    expect(await ui.updateMessage.findAll()).toHaveLength(2);

    await user.click(ui.openDialogBtn.get());

    expect(
      await screen.findByText(
        'admin_notification.update.new_sqs_version_when_running_sqcb.upgrade',
      ),
    ).toBeInTheDocument();

    await user.keyboard('{Escape}');

    expect(
      screen.queryByText('admin_notification.update.new_sqs_version_when_running_sqcb.upgrade'),
    ).not.toBeInTheDocument();
  });
});

function renderUpdateNotification(
  dissmissable = false,
  user?: Partial<CurrentUser>,
  appState: Partial<AppState> = {},
) {
  return renderComponent(
    <CurrentUserContext.Provider
      value={{
        currentUser: mockCurrentUser({
          isLoggedIn: true,
          permissions: { global: [Permissions.Admin] },
          ...user,
        }),
        updateCurrentUserHomepage: () => {},
        updateDismissedNotices: () => {},
      }}
    >
      <AppStateContext.Provider
        value={mockAppState({
          edition: EditionKey.developer,
          version: '10.5.0',
          // versionEOL is a date in the past to be sure that it is not used when we have data from upgrades endpoint
          versionEOL: '2020-01-01',
          ...appState,
        })}
      >
        <UpdateNotification dismissable={dissmissable} />
      </AppStateContext.Provider>
    </CurrentUserContext.Provider>,
  );
}
