/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getMarketplaceNavigation } from '../../api/nav';
import EditionBox from './components/EditionBox';
import { EDITIONS } from './utils';

export interface Props {
  currentEdition?: T.EditionKey;
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
      formData => {
        if (this.mounted) {
          this.setState({ ...formData });
        }
      },
      () => {}
    );
  };

  render() {
    const { currentEdition } = this.props;
    const { serverId, ncloc } = this.state;
    const currentEditionIdx = EDITIONS.findIndex(edition => edition.key === currentEdition);
    const visibleEditions = EDITIONS.slice(currentEditionIdx + 1);

    if (visibleEditions.length <= 0) {
      return null;
    }

    return (
      <div className="spacer-bottom marketplace-editions">
        {visibleEditions.map(edition => (
          <EditionBox
            currentEdition={currentEdition}
            edition={edition}
            key={edition.key}
            ncloc={ncloc}
            serverId={serverId}
          />
        ))}
      </div>
    );
  }
}
