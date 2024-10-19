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
import { LargeCenteredLayout, PageContentFontWrapper, Spinner } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { createLink, deleteLink, getProjectLinks } from '../../api/projectLinks';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import { translate } from '../../helpers/l10n';
import { Component, ProjectLink } from '../../types/types';
import Header from './Header';
import ProjectLinkTable from './ProjectLinkTable';

interface Props {
  component: Component;
}

interface State {
  links?: ProjectLink[];
  loading: boolean;
}

export class ProjectLinksApp extends React.PureComponent<Props, State> {
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
    const {
      component: { key },
    } = this.props;

    this.setState({ loading: true });
    getProjectLinks(key).then(
      (links) => {
        if (this.mounted) {
          this.setState({ links, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  handleCreateLink = (name: string, url: string) => {
    const {
      component: { key },
    } = this.props;

    return createLink({ name, projectKey: key, url }).then((link) => {
      if (this.mounted) {
        this.setState(({ links = [] }) => ({
          links: [...links, link],
        }));
      }
    });
  };

  handleDeleteLink = (linkId: string) => {
    return deleteLink(linkId).then(() => {
      if (this.mounted) {
        this.setState(({ links = [] }) => ({
          links: links.filter((link) => link.id !== linkId),
        }));
      }
    });
  };

  render() {
    const { loading, links } = this.state;
    return (
      <LargeCenteredLayout>
        <PageContentFontWrapper className="sw-my-8 sw-typo-default">
          <Helmet defer={false} title={translate('project_links.page')} />
          <Header onCreate={this.handleCreateLink} />
          <Spinner loading={loading}>
            <ProjectLinkTable links={links ?? []} onDelete={this.handleDeleteLink} />
          </Spinner>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withComponentContext(ProjectLinksApp);
