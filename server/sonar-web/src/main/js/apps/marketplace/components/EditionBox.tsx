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
  editionKey: string;
  editionStatus?: EditionStatus;
  onInstall: (edition: Edition) => void;
}

export default class EditionBox extends React.PureComponent<Props> {
  handleInstall = () => this.props.onInstall(this.props.edition);

  render() {
    const { edition, editionKey, editionStatus } = this.props;
    const isInstalled = editionStatus && editionStatus.currentEditionKey === editionKey;
    const isInstalling = editionStatus && editionStatus.nextEditionKey === editionKey;
    const installInProgress =
      editionStatus && editionStatus.installationStatus === 'AUTOMATIC_IN_PROGRESS';
    return (
      <div className="boxed-group boxed-group-inner marketplace-edition">
        {isInstalled &&
        !isInstalling && (
          <span className="marketplace-edition-badge badge badge-normal-size">
            <CheckIcon size={14} className="little-spacer-right text-text-top" />
            {translate('marketplace.installed')}
          </span>
        )}
        {isInstalling && (
          <span className="marketplace-edition-badge badge badge-normal-size">
            {translate('marketplace.installing')}
          </span>
        )}
        <div>
          <h3 className="spacer-bottom">{edition.name}</h3>
          <p>{edition.desc}</p>
        </div>
        <div className="marketplace-edition-action spacer-top">
          <a href={edition.more_link} target="_blank">
            {translate('marketplace.learn_more')}
          </a>
          {!isInstalled && (
            <button disabled={installInProgress} onClick={this.handleInstall}>
              {translate('marketplace.install')}
            </button>
          )}
          {isInstalled && (
            <button className="button-red" disabled={installInProgress}>
              {translate('marketplace.uninstall')}
            </button>
          )}
        </div>
      </div>
    );
  }
}
