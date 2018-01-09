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
import EditionBoxBadge from './EditionBoxBadge';
import { Edition, EditionStatus } from '../../../api/marketplace';
import { translate } from '../../../helpers/l10n';

interface Props {
  actionLabel: string;
  disableAction: boolean;
  displayAction: boolean;
  edition: Edition;
  editionStatus?: EditionStatus;
  onAction: (edition: Edition) => void;
}

export default class EditionBox extends React.PureComponent<Props> {
  handleAction = () => this.props.onAction(this.props.edition);

  render() {
    const { disableAction, displayAction, edition, editionStatus } = this.props;
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
          {displayAction && (
            <button disabled={disableAction} onClick={this.handleAction}>
              {this.props.actionLabel}
            </button>
          )}
        </div>
      </div>
    );
  }
}
