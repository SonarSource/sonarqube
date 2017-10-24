/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import NavBarNotif from '../../../../components/nav/NavBarNotif';
import RestartForm from '../../../../components/common/RestartForm';
import { dismissErrorMessage, Edition, EditionStatus } from '../../../../api/marketplace';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  editions?: Edition[];
  editionStatus: EditionStatus;
  preventRestart: boolean;
  setEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  openRestart: boolean;
}

export default class SettingsEditionsNotif extends React.PureComponent<Props, State> {
  state: State = { openRestart: false };

  handleOpenRestart = () => this.setState({ openRestart: true });
  hanleCloseRestart = () => this.setState({ openRestart: false });

  handleDismissError = () =>
    dismissErrorMessage().then(
      () => this.props.setEditionStatus({ ...this.props.editionStatus, installError: undefined }),
      () => {}
    );

  renderRestartMsg(edition?: Edition) {
    const { editionStatus, preventRestart } = this.props;
    return (
      <NavBarNotif className="alert alert-success">
        <span>
          {edition ? (
            translateWithParameters(
              'marketplace.status_x.' + editionStatus.installationStatus,
              edition.name
            )
          ) : (
            translate('marketplace.status', editionStatus.installationStatus)
          )}
        </span>
        {!preventRestart && (
          <button className="js-restart spacer-left" onClick={this.handleOpenRestart}>
            {translate('marketplace.restart')}
          </button>
        )}
        {!preventRestart &&
        this.state.openRestart && <RestartForm onClose={this.hanleCloseRestart} />}
      </NavBarNotif>
    );
  }

  renderManualMsg(edition?: Edition) {
    const { editionStatus } = this.props;
    return (
      <NavBarNotif className="alert alert-danger">
        {edition ? (
          translateWithParameters(
            'marketplace.status_x.' + editionStatus.installationStatus,
            edition.name
          )
        ) : (
          translate('marketplace.status', editionStatus.installationStatus)
        )}
        {edition && (
          <a
            className="button spacer-left"
            download={`sonarqube-${edition.name}.zip`}
            href={edition.downloadUrl}
            target="_blank">
            {translate('marketplace.download_package')}
          </a>
        )}
        <a
          className="spacer-left"
          href="https://redirect.sonarsource.com/doc/how-to-install-an-edition.html"
          target="_blank">
          {translate('marketplace.how_to_install')}
        </a>
      </NavBarNotif>
    );
  }

  renderStatusAlert() {
    const { editionStatus } = this.props;
    const { installationStatus, nextEditionKey } = editionStatus;
    const nextEdition =
      this.props.editions && this.props.editions.find(edition => edition.key === nextEditionKey);

    switch (installationStatus) {
      case 'AUTOMATIC_IN_PROGRESS':
        return (
          <NavBarNotif className="alert alert-info">
            <i className="spinner spacer-right text-bottom" />
            <span>{translate('marketplace.status.AUTOMATIC_IN_PROGRESS')}</span>
          </NavBarNotif>
        );
      case 'AUTOMATIC_READY':
      case 'UNINSTALL_IN_PROGRESS':
        return this.renderRestartMsg(nextEdition);
      case 'MANUAL_IN_PROGRESS':
        return this.renderManualMsg(nextEdition);
    }
    return null;
  }

  render() {
    const { installError } = this.props.editionStatus;
    if (installError) {
      return (
        <NavBarNotif className="alert alert-danger" onCancel={this.handleDismissError}>
          {installError}
        </NavBarNotif>
      );
    }

    return this.renderStatusAlert();
  }
}
