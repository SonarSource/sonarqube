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
import * as classNames from 'classnames';
import * as React from 'react';
import Helmet from 'react-helmet';
import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import Search from './Search';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import { Component as CodeComponent } from '../types';
import { retrieveComponentChildren, retrieveComponent, loadMoreChildren } from '../utils';
import ListFooter from '../../../components/controls/ListFooter';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { parseError } from '../../../helpers/request';
import { getBranchName } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import { Component, Branch } from '../../../app/types';
import '../code.css';

interface Props {
  branch?: Branch;
  component: Component;
  location: { query: { [x: string]: string } };
}

interface State {
  baseComponent?: CodeComponent;
  breadcrumbs: Array<CodeComponent>;
  components?: Array<CodeComponent>;
  error?: string;
  loading: boolean;
  page: number;
  searchResults?: Array<CodeComponent>;
  sourceViewer?: CodeComponent;
  total: number;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    breadcrumbs: [],
    total: 0,
    page: 0
  };

  componentDidMount() {
    this.mounted = true;
    this.handleComponentChange();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component || prevProps.branch !== this.props.branch) {
      this.handleComponentChange();
    } else if (prevProps.location !== this.props.location) {
      this.handleUpdate();
    }
  }

  componentWillUnmount() {
    clearBucket();
    this.mounted = false;
  }

  handleComponentChange() {
    const { branch, component } = this.props;

    // we already know component's breadcrumbs,
    addComponentBreadcrumbs(component.key, component.breadcrumbs);

    this.setState({ loading: true });
    const isPortfolio = ['VW', 'SVW'].includes(component.qualifier);
    retrieveComponentChildren(component.key, isPortfolio, getBranchName(branch))
      .then(() => {
        addComponent(component);
        if (this.mounted) {
          this.handleUpdate();
        }
      })
      .catch(e => {
        if (this.mounted) {
          this.setState({ loading: false });
          parseError(e).then(this.handleError);
        }
      });
  }

  loadComponent(componentKey: string) {
    this.setState({ loading: true });

    const isPortfolio = ['VW', 'SVW'].includes(this.props.component.qualifier);
    retrieveComponent(componentKey, isPortfolio, getBranchName(this.props.branch))
      .then(r => {
        if (this.mounted) {
          if (['FIL', 'UTS'].includes(r.component.qualifier)) {
            this.setState({
              loading: false,
              sourceViewer: r.component,
              breadcrumbs: r.breadcrumbs,
              searchResults: undefined
            });
          } else {
            this.setState({
              loading: false,
              baseComponent: r.component,
              components: r.components,
              breadcrumbs: r.breadcrumbs,
              total: r.total,
              page: r.page,
              sourceViewer: undefined,
              searchResults: undefined
            });
          }
        }
      })
      .catch(e => {
        if (this.mounted) {
          this.setState({ loading: false });
          parseError(e).then(this.handleError);
        }
      });
  }

  handleUpdate() {
    const { component, location } = this.props;
    const { selected } = location.query;
    const finalKey = selected || component.key;

    this.loadComponent(finalKey);
  }

  handleLoadMore = () => {
    const { baseComponent, components, page } = this.state;
    if (!baseComponent || !components) {
      return;
    }
    const isPortfolio = ['VW', 'SVW'].includes(this.props.component.qualifier);
    loadMoreChildren(baseComponent.key, page + 1, isPortfolio, getBranchName(this.props.branch))
      .then(r => {
        if (this.mounted) {
          this.setState({
            components: [...components, ...r.components],
            page: r.page,
            total: r.total
          });
        }
      })
      .catch(e => {
        if (this.mounted) {
          this.setState({ loading: false });
          parseError(e).then(this.handleError);
        }
      });
  };

  handleError = (error: string) => {
    if (this.mounted) {
      this.setState({ error });
    }
  };

  render() {
    const { branch, component, location } = this.props;
    const {
      loading,
      error,
      baseComponent,
      components,
      breadcrumbs,
      total,
      sourceViewer
    } = this.state;
    const branchName = getBranchName(branch);

    const shouldShowBreadcrumbs = breadcrumbs.length > 1;

    const componentsClassName = classNames('boxed-group', 'boxed-group-inner', 'spacer-top', {
      'new-loading': loading
    });

    return (
      <div className="page page-limited">
        <Helmet title={translate('code.page')} />

        {error && <div className="alert alert-danger">{error}</div>}

        <Search
          branch={branchName}
          component={component}
          location={location}
          onError={this.handleError}
        />

        <div className="code-components">
          {shouldShowBreadcrumbs && (
            <Breadcrumbs branch={branchName} breadcrumbs={breadcrumbs} rootComponent={component} />
          )}

          {sourceViewer === undefined &&
            components !== undefined && (
              <div className={componentsClassName}>
                <Components
                  baseComponent={baseComponent}
                  branch={branchName}
                  components={components}
                  rootComponent={component}
                />
              </div>
            )}

          {sourceViewer === undefined &&
            components !== undefined && (
              <ListFooter count={components.length} total={total} loadMore={this.handleLoadMore} />
            )}

          {sourceViewer !== undefined && (
            <div className="spacer-top">
              <SourceViewer branch={branchName} component={sourceViewer.key} />
            </div>
          )}
        </div>
      </div>
    );
  }
}
