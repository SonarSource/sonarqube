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

import * as React from 'react';
import { Accordion, BasicSeparator, Link, Note } from '~design-system';
import { translate } from '../../helpers/l10n';
import { SystemUpgrade } from '../../types/system';
import DateFormatter from '../intl/DateFormatter';

interface Props {
  className?: string;
  upgrades: SystemUpgrade[];
}

interface State {
  showMore: boolean;
}

export default class SystemUpgradeIntermediate extends React.PureComponent<Props, State> {
  state: State = { showMore: false };

  toggleIntermediatVersions = () => {
    this.setState((state) => ({ showMore: !state.showMore }));
  };

  render() {
    const { showMore } = this.state;
    const { upgrades } = this.props;
    if (upgrades.length <= 0) {
      return null;
    }

    return (
      <div className={this.props.className}>
        <Accordion
          header={
            showMore
              ? translate('system.hide_intermediate_versions')
              : translate('system.show_intermediate_versions')
          }
          open={showMore}
          onClick={this.toggleIntermediatVersions}
        >
          {upgrades.map((upgrade, index) => (
            <Note className="sw-block sw-mb-4" key={upgrade.version}>
              {upgrade.releaseDate && (
                <DateFormatter date={upgrade.releaseDate} long>
                  {(formattedDate) => (
                    <p>
                      <b className="sw-mr-1">SonarQube {upgrade.version}</b>
                      {formattedDate}
                      {upgrade.changeLogUrl && (
                        <Link className="sw-ml-2" to={upgrade.changeLogUrl}>
                          {translate('system.release_notes')}
                        </Link>
                      )}
                    </p>
                  )}
                </DateFormatter>
              )}
              {upgrade.description && <p className="sw-mt-2">{upgrade.description}</p>}

              {index !== upgrades.length - 1 && <BasicSeparator />}
            </Note>
          ))}
        </Accordion>
      </div>
    );
  }
}
