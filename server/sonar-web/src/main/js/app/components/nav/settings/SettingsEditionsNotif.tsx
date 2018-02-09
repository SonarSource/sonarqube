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

  renderStatusMsg(edition?: Edition) {
    const { editionStatus } = this.props;
    return (
      <NavBarNotif className="alert alert-info">
        <i className="spinner spacer-right text-bottom" />
        <span>
          {edition
            ? translateWithParameters(
                'marketplace.edition_status_x.' + editionStatus.installationStatus,
                edition.name
              )
            : translate('marketplace.edition_status', editionStatus.installationStatus)}
        </span>
      </NavBarNotif>
    );
  }

  renderRestartMsg(edition?: Edition) {
    const { editionStatus, preventRestart } = this.props;
    return (
      <NavBarNotif className="alert alert-success">
        <span>
          {edition
            ? translateWithParameters(
                'marketplace.edition_status_x.' + editionStatus.installationStatus,
                edition.name
              )
            : translate('marketplace.edition_status', editionStatus.installationStatus)}
        </span>
        {edition &&
          edition.key === 'datacenter' && (
            <span className="little-spacer-left">
              <FormattedMessage
                defaultMessage={translate('marketplace.see_documentation_to_enable_cluster')}
                id="marketplace.see_documentation_to_enable_cluster"
                values={{
                  url: (
                    <a
                      href="https://redirect.sonarsource.com/doc/data-center-edition.html"
                      target="_blank">
                      {edition.name}
                    </a>
                  )
                }}
              />
            </span>
          )}
        {!preventRestart && (
          <button className="js-restart spacer-left" onClick={this.handleOpenRestart} type="button">
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
        {edition
          ? translateWithParameters(
              'marketplace.edition_status_x.' + editionStatus.installationStatus,
              edition.name
            )
          : translate('marketplace.edition_status', editionStatus.installationStatus)}
        <a
          className="spacer-left"
          href={
            edition && edition.key === 'datacenter'
              ? 'https://redirect.sonarsource.com/doc/data-center-edition.html'
              : 'https://redirect.sonarsource.com/doc/how-to-install-an-edition.html'
          }
          target="_blank">
          {translate('marketplace.how_to_install')}
        </a>
        {edition && (
          <a
            className="button spacer-left"
            download={`sonarqube-${edition.name}.zip`}
            href={edition.downloadUrl}
            target="_blank">
            {translate('marketplace.download_package')}
          </a>
        )}
      </NavBarNotif>
    );
  }

  renderStatusAlert() {
    const { currentEditionKey, installationStatus, nextEditionKey } = this.props.editionStatus;
    const nextEdition =
      this.props.editions && this.props.editions.find(edition => edition.key === nextEditionKey);
    const currentEdition =
      this.props.editions &&
      this.props.editions.find(
        edition =>
          edition.key === currentEditionKey || (!currentEditionKey && edition.key === 'community')
      );

    switch (installationStatus) {
      case 'AUTOMATIC_IN_PROGRESS':
        return this.renderStatusMsg(nextEdition);
      case 'AUTOMATIC_READY':
        return this.renderRestartMsg(nextEdition);
      case 'UNINSTALL_IN_PROGRESS':
        return this.renderRestartMsg(currentEdition);
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
