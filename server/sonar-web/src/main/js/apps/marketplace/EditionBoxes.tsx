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
import EditionBox from './components/EditionBox';
import { EDITIONS } from './utils';
import { getFormData } from '../../api/marketplace';

export interface Props {
  currentEdition?: string;
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
    this.fetchFormData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchFormData = () => {
    getFormData().then(
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
    return (
      <div className="spacer-bottom marketplace-editions">
        {EDITIONS.map(edition => (
          <EditionBox
            currentEdition={currentEdition || 'community'}
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
