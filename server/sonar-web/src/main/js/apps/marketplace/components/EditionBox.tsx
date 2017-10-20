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
import CheckIcon from '../../../components/icons-components/CheckIcon';
import { Edition, EditionStatus } from '../../../api/marketplace';
import { translate } from '../../../helpers/l10n';

interface Props {
  edition: Edition;
  editionStatus?: EditionStatus;
  onInstall: (edition: Edition) => void;
  onUninstall: () => void;
  readOnly: boolean;
}

export default class EditionBox extends React.PureComponent<Props> {
  handleInstall = () => this.props.onInstall(this.props.edition);

  renderBadge(isInstalled?: boolean, installInProgress?: boolean) {
    const { edition, editionStatus } = this.props;
    const installReady = editionStatus && editionStatus.installationStatus === 'AUTOMATIC_READY';
    const isInstalling =
      installInProgress && editionStatus && editionStatus.nextEditionKey === edition.key;
    if (isInstalling) {
      return (
        <span className="marketplace-edition-badge badge badge-normal-size">
          {installReady ? translate('marketplace.pending') : translate('marketplace.installing')}
        </span>
      );
    }
    if (isInstalled) {
      return (
        <span className="marketplace-edition-badge badge badge-normal-size">
          <CheckIcon size={14} className="little-spacer-right text-text-top" />
          {translate('marketplace.installed')}
        </span>
      );
    }
    return null;
  }

  render() {
    const { edition, editionStatus, readOnly } = this.props;
    const isInstalled = editionStatus && editionStatus.currentEditionKey === edition.key;
    const installInProgress =
      editionStatus &&
      ['AUTOMATIC_IN_PROGRESS', 'AUTOMATIC_READY'].includes(editionStatus.installationStatus);
    return (
      <div className="boxed-group boxed-group-inner marketplace-edition">
        {this.renderBadge(isInstalled, installInProgress)}
        <div>
          <h3 className="spacer-bottom">{edition.name}</h3>
          <p>{edition.textDescription}</p>
        </div>
        <div className="marketplace-edition-action spacer-top">
          <a href={edition.homeUrl} target="_blank">
            {translate('marketplace.learn_more')}
          </a>
          {!readOnly &&
          !isInstalled && (
            <button disabled={installInProgress} onClick={this.handleInstall}>
              {translate('marketplace.install')}
            </button>
          )}
          {!readOnly &&
          isInstalled && (
            <button
              className="button-red"
              disabled={installInProgress}
              onClick={this.props.onUninstall}>
              {translate('marketplace.uninstall')}
            </button>
          )}
        </div>
      </div>
    );
  }
}
