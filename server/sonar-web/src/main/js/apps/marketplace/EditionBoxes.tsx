/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import EditionBox from './components/EditionBox';
import LicenseEditionForm from './components/LicenseEditionForm';
import UninstallEditionForm from './components/UninstallEditionForm';
import { sortEditions } from './utils';
import { Edition, EditionStatus } from '../../api/marketplace';
import { translate } from '../../helpers/l10n';

export interface Props {
  canInstall: boolean;
  canUninstall: boolean;
  editions?: Edition[];
  editionStatus?: EditionStatus;
  loading: boolean;
  updateCenterActive: boolean;
  updateEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  installEdition?: Edition;
  openUninstallForm: boolean;
}

export default class EditionBoxes extends React.PureComponent<Props, State> {
  state: State = { openUninstallForm: false };

  handleOpenLicenseForm = (edition: Edition) => this.setState({ installEdition: edition });
  handleCloseLicenseForm = () => this.setState({ installEdition: undefined });

  handleOpenUninstallForm = () => this.setState({ openUninstallForm: true });
  handleCloseUninstallForm = () => this.setState({ openUninstallForm: false });

  renderForms(sortedEditions: Edition[], installedIdx?: number) {
    const { canInstall, canUninstall, editionStatus } = this.props;
    const { installEdition, openUninstallForm } = this.state;
    const installEditionIdx =
      installEdition && sortedEditions.findIndex(edition => edition.key === installEdition.key);

    if (canInstall && installEdition) {
      return (
        <LicenseEditionForm
          edition={installEdition}
          editions={sortedEditions}
          isDowngrade={
            installedIdx !== undefined &&
            installEditionIdx !== undefined &&
            installEditionIdx < installedIdx
          }
          onClose={this.handleCloseLicenseForm}
          updateEditionStatus={this.props.updateEditionStatus}
        />
      );
    }

    if (canUninstall && openUninstallForm && editionStatus && editionStatus.currentEditionKey) {
      return (
        <UninstallEditionForm
          edition={sortedEditions.find(edition => edition.key === editionStatus.currentEditionKey)}
          editionStatus={editionStatus}
          onClose={this.handleCloseUninstallForm}
          updateEditionStatus={this.props.updateEditionStatus}
        />
      );
    }

    return null;
  }

  render() {
    const { canInstall, canUninstall, editions, loading } = this.props;

    if (loading) {
      return <i className="big-spacer-bottom spinner" />;
    }

    if (!editions) {
      return (
        <div className="spacer-bottom marketplace-editions">
          <span className="alert alert-info">
            <FormattedMessage
              defaultMessage={translate('marketplace.editions_unavailable')}
              id="marketplace.editions_unavailable"
              values={{
                url: (
                  <a href="https://redirect.sonarsource.com/editions/editions.html" target="_blank">
                    SonarSource.com
                  </a>
                )
              }}
            />
          </span>
        </div>
      );
    }

    const sortedEditions = sortEditions(editions);
    const status = this.props.editionStatus || { installationStatus: 'NONE' };
    const inProgressStatus = [
      'AUTOMATIC_IN_PROGRESS',
      'AUTOMATIC_READY',
      'UNINSTALL_IN_PROGRESS'
    ].includes(status.installationStatus);
    const installedIdx = sortedEditions.findIndex(
      edition => edition.key === status.currentEditionKey
    );
    const nextIdx = sortedEditions.findIndex(edition => edition.key === status.nextEditionKey);
    const currentIdx = inProgressStatus ? nextIdx : installedIdx;
    return (
      <div className="spacer-bottom marketplace-editions">
        <EditionBox
          actionLabel={translate('marketplace.downgrade')}
          disableAction={inProgressStatus}
          displayAction={canUninstall && currentIdx > 0}
          edition={sortedEditions[0]}
          editionStatus={status}
          key={sortedEditions[0].key}
          onAction={this.handleOpenUninstallForm}
        />
        {sortedEditions
          .slice(1)
          .map((edition, idx) => (
            <EditionBox
              actionLabel={
                currentIdx > idx + 1
                  ? translate('marketplace.downgrade')
                  : translate('marketplace.upgrade')
              }
              disableAction={inProgressStatus}
              displayAction={canInstall && currentIdx !== idx + 1}
              edition={edition}
              editionStatus={status}
              key={edition.key}
              onAction={this.handleOpenLicenseForm}
            />
          ))}

        {this.renderForms(sortedEditions, installedIdx)}
      </div>
    );
  }
}
