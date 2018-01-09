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
import * as classNames from 'classnames';
import DateFormatter from '../../../../components/intl/DateFormatter';
import { SystemUpgrade } from '../../../../api/system';
import { translate } from '../../../../helpers/l10n';

interface Props {
  className?: string;
  upgrades: SystemUpgrade[];
}

interface State {
  showMore: boolean;
}

export default class SystemUpgradeIntermediate extends React.PureComponent<Props, State> {
  state: State = { showMore: false };

  toggleIntermediatVersions = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.setState(state => ({ showMore: !state.showMore }));
  };

  render() {
    const { showMore } = this.state;
    const { upgrades } = this.props;
    if (upgrades.length <= 0) {
      return null;
    }

    return (
      <div className={this.props.className}>
        <a
          className="button-link little-spacer-bottom"
          href="#"
          onClick={this.toggleIntermediatVersions}>
          {showMore
            ? translate('system.hide_intermediate_versions')
            : translate('system.show_intermediate_versions')}
          <i
            className={classNames('little-spacer-left', {
              'icon-arrow-down': !showMore,
              'icon-arrow-up': showMore
            })}
          />
        </a>
        {showMore &&
          upgrades.map(upgrade => (
            <div key={upgrade.version} className="note system-upgrade-intermediate">
              <DateFormatter date={upgrade.releaseDate} long={true}>
                {formattedDate => (
                  <p>
                    <b className="little-spacer-right">SonarQube {upgrade.version}</b>
                    {formattedDate}
                    {upgrade.changeLogUrl && (
                      <a className="spacer-left" href={upgrade.changeLogUrl} target="_blank">
                        {translate('system.release_notes')}
                      </a>
                    )}
                  </p>
                )}
              </DateFormatter>
              <p className="little-spacer-top">{upgrade.description}</p>
            </div>
          ))}
      </div>
    );
  }
}
