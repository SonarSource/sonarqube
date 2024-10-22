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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import { Title } from '~design-system';
import { translate } from '../../helpers/l10n';
import CreationModal from './CreationModal';

interface Props {
  onCreate: (name: string, url: string) => Promise<void>;
}

interface State {
  creationModal: boolean;
}

export default class Header extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { creationModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCreateClick = () => {
    this.setState({ creationModal: true });
  };

  handleCreationModalClose = () => {
    if (this.mounted) {
      this.setState({ creationModal: false });
    }
  };

  render() {
    return (
      <>
        <header className="sw-mt-8 sw-mb-4">
          <div className="sw-flex sw-justify-between">
            <Title className="sw-mb-4">{translate('project_links.page')}</Title>
            <Button
              id="create-project-link"
              onClick={this.handleCreateClick}
              variety={ButtonVariety.Primary}
            >
              {translate('create')}
            </Button>
          </div>
          <p>{translate('project_links.page.description')}</p>
        </header>
        {this.state.creationModal && (
          <CreationModal onClose={this.handleCreationModalClose} onSubmit={this.props.onCreate} />
        )}
      </>
    );
  }
}
