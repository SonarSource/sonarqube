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
import EditionBoxBadge from './EditionBoxBadge';
import { Edition, EditionStatus } from '../../../api/marketplace';
import { translate } from '../../../helpers/l10n';

interface Props {
  canInstall: boolean;
  canUninstall: boolean;
  edition: Edition;
  editionStatus?: EditionStatus;
  isDowngrade?: boolean;
  onInstall: (edition: Edition) => void;
  onUninstall: () => void;
}

export default class EditionBox extends React.PureComponent<Props> {
  handleInstall = () => this.props.onInstall(this.props.edition);

  renderActions(isInstalled?: boolean, installInProgress?: boolean) {
    const { canInstall, canUninstall, editionStatus } = this.props;
    const uninstallInProgress =
      editionStatus && editionStatus.installationStatus === 'UNINSTALL_IN_PROGRESS';

    if (canInstall && !isInstalled) {
      return (
        <button disabled={installInProgress || uninstallInProgress} onClick={this.handleInstall}>
          {this.props.isDowngrade
            ? translate('marketplace.downgrade')
            : translate('marketplace.upgrade')}
        </button>
      );
    }
    if (canUninstall && isInstalled) {
      return (
        <button
          className="button-red"
          disabled={installInProgress || uninstallInProgress}
          onClick={this.props.onUninstall}>
          {translate('marketplace.uninstall')}
        </button>
      );
    }

    return null;
  }

  render() {
    const { edition, editionStatus } = this.props;
    const isInstalled = editionStatus && editionStatus.currentEditionKey === edition.key;
    const installInProgress =
      editionStatus &&
      ['AUTOMATIC_IN_PROGRESS', 'AUTOMATIC_READY'].includes(editionStatus.installationStatus);
    return (
      <div className="boxed-group boxed-group-inner marketplace-edition">
        {editionStatus && <EditionBoxBadge editionKey={edition.key} status={editionStatus} />}
        <div>
          <h3 className="spacer-bottom">{edition.name}</h3>
          <p>{edition.textDescription}</p>
        </div>
        <div className="marketplace-edition-action spacer-top">
          <a href={edition.homeUrl} target="_blank">
            {translate('marketplace.learn_more')}
          </a>
          {this.renderActions(isInstalled, installInProgress)}
        </div>
      </div>
    );
  }
}
