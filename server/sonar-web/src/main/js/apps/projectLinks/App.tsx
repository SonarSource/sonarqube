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
import Helmet from 'react-helmet';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { createLink, deleteLink, getProjectLinks } from '../../api/projectLinks';
import Header from './Header';
import Table from './Table';

interface Props {
  component: Pick<T.Component, 'key'>;
}

interface State {
  links?: T.ProjectLink[];
  loading: boolean;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchLinks();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component.key !== this.props.component.key) {
      this.fetchLinks();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLinks = () => {
    this.setState({ loading: true });
    getProjectLinks(this.props.component.key).then(
      links => {
        if (this.mounted) {
          this.setState({ links, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleCreateLink = (name: string, url: string) => {
    return createLink({ name, projectKey: this.props.component.key, url }).then(link => {
      if (this.mounted) {
        this.setState(({ links = [] }) => ({
          links: [...links, link]
        }));
      }
    });
  };

  handleDeleteLink = (linkId: string) => {
    return deleteLink(linkId).then(() => {
      if (this.mounted) {
        this.setState(({ links = [] }) => ({
          links: links.filter(link => link.id !== linkId)
        }));
      }
    });
  };

  render() {
    return (
      <div className="page page-limited">
        <Helmet title={translate('project_links.page')} />
        <Header onCreate={this.handleCreateLink} />
        <DeferredSpinner loading={this.state.loading}>
          {this.state.links && <Table links={this.state.links} onDelete={this.handleDeleteLink} />}
        </DeferredSpinner>
      </div>
    );
  }
}
