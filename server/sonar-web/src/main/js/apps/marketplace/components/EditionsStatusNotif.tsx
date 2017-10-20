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
import RestartForm from '../../../components/common/RestartForm';
import CloseIcon from '../../../components/icons-components/CloseIcon';
import { dismissErrorMessage, Edition, EditionStatus } from '../../../api/marketplace';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  editions?: Edition[];
  editionStatus: EditionStatus;
  readOnly: boolean;
  updateEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  openRestart: boolean;
}

export default class EditionsStatusNotif extends React.PureComponent<Props, State> {
  state: State = { openRestart: false };

  handleOpenRestart = () => this.setState({ openRestart: true });
  hanleCloseRestart = () => this.setState({ openRestart: false });

  handleDismissError = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    dismissErrorMessage().then(
      () =>
        this.props.updateEditionStatus({ ...this.props.editionStatus, installError: undefined }),
      () => {}
    );
  };

  renderStatusAlert() {
    const { editionStatus, readOnly } = this.props;
    const { installationStatus, nextEditionKey } = editionStatus;
    const nextEdition =
      this.props.editions && this.props.editions.find(edition => edition.key === nextEditionKey);

    const editionStatusMessage = nextEdition
      ? translateWithParameters('marketplace.status_x.' + installationStatus, nextEdition.name)
      : translate('marketplace.status', installationStatus);

    switch (installationStatus) {
      case 'AUTOMATIC_IN_PROGRESS':
        return (
          <div className="alert alert-info">
            <i className="spinner spacer-right text-bottom" />
            <span>{translate('marketplace.status.AUTOMATIC_IN_PROGRESS')}</span>
          </div>
        );
      case 'AUTOMATIC_READY':
      case 'UNINSTALL_IN_PROGRESS':
        return (
          <div className="alert alert-success">
            <span>{editionStatusMessage}</span>
            {!readOnly && (
              <button className="js-restart spacer-left" onClick={this.handleOpenRestart}>
                {translate('marketplace.restart')}
              </button>
            )}
            {!readOnly &&
            this.state.openRestart && <RestartForm onClose={this.hanleCloseRestart} />}
          </div>
        );
      case 'MANUAL_IN_PROGRESS':
        return (
          <div className="alert alert-danger">
            {editionStatusMessage}
            <p className="spacer-left">
              {nextEdition && (
                <a
                  className="button spacer-right"
                  download={`sonarqube-${nextEdition.name}.zip`}
                  href={nextEdition.downloadUrl}
                  target="_blank">
                  {translate('marketplace.download_package')}
                </a>
              )}
              <a
                href="https://redirect.sonarsource.com/doc/how-to-install-an-edition.html"
                target="_blank">
                {translate('marketplace.how_to_install')}
              </a>
            </p>
            <a className="little-spacer-left" href="https://www.sonarsource.com" target="_blank">
              {translate('marketplace.how_to_install')}
            </a>
          </div>
        );
    }
    return null;
  }

  render() {
    const { installError } = this.props.editionStatus;
    return (
      <div>
        {installError && (
          <div className="alert alert-danger diplay-flex-row">
            {installError}
            <a
              className="pull-right button-link text-danger"
              href="#"
              onClick={this.handleDismissError}>
              <CloseIcon />
            </a>
          </div>
        )}
        {this.renderStatusAlert()}
      </div>
    );
  }
}
