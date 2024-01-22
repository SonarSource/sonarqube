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
import { Card, Link } from 'design-system';
import * as React from 'react';
import { getMarketplaceNavigation } from '../../api/navigation';
import { getAllEditionsAbove, getEditionUrl } from '../../helpers/editions';
import { translate } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import EditionBox from './components/EditionBox';

export interface Props {
  currentEdition?: EditionKey;
}

interface State {
  serverId?: string;
  ncloc?: number;
}

export default class EditionBoxes extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData = () => {
    getMarketplaceNavigation().then(
      (formData) => {
        if (this.mounted) {
          this.setState({ ...formData });
        }
      },
      () => {},
    );
  };

  render() {
    const { currentEdition } = this.props;
    const { serverId, ncloc } = this.state;
    const visibleEditions = getAllEditionsAbove(currentEdition);

    if (visibleEditions.length <= 0) {
      return null;
    }

    return (
      <div className="sw-mt-4 sw-flex sw-gap-4">
        {visibleEditions.map((edition) => (
          <Card
            key={edition.key}
            className="sw-max-w-1/2 sw-flex-1 sw-flex sw-flex-col sw-justify-between"
          >
            <EditionBox edition={edition} />

            <div className="sw-mt-4">
              <Link to={getEditionUrl(edition, { ncloc, serverId, sourceEdition: currentEdition })}>
                {translate('marketplace.request_free_trial')}
              </Link>
            </div>
          </Card>
        ))}
      </div>
    );
  }
}
