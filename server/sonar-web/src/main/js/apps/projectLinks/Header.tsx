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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
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
        <header className="page-header">
          <h1 className="page-title">{translate('project_links.page')}</h1>
          <div className="page-actions">
            <Button id="create-project-link" onClick={this.handleCreateClick}>
              {translate('create')}
            </Button>
          </div>
          <div className="page-description">{translate('project_links.page.description')}</div>
        </header>
        {this.state.creationModal && (
          <CreationModal onClose={this.handleCreationModalClose} onSubmit={this.props.onCreate} />
        )}
      </>
    );
  }
}
