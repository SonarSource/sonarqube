/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import classNames from 'classnames';
import React from 'react';
import Helmet from 'react-helmet';
import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import SourceViewer from './../../../components/SourceViewer/SourceViewer';
import Search from './Search';
import ListFooter from '../../../components/controls/ListFooter';
import {
  retrieveComponentChildren,
  retrieveComponent,
  loadMoreChildren,
  parseError
} from '../utils';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import { getBranchName } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import '../code.css';

export default class App extends React.PureComponent {
  state = {
    loading: true,
    baseComponent: null,
    components: null,
    breadcrumbs: [],
    total: 0,
    page: 0,
    sourceViewer: null,
    error: null
  };

  componentDidMount() {
    this.mounted = true;
    this.handleComponentChange();
  }

  componentDidUpdate(prevProps) {
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
      .then(r => {
        addComponent(r.baseComponent);
        this.handleUpdate();
      })
      .catch(e => {
        if (this.mounted) {
          this.setState({ loading: false });
          parseError(e).then(this.handleError.bind(this));
        }
      });
  }

  loadComponent(componentKey) {
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
              searchResults: null
            });
          } else {
            this.setState({
              loading: false,
              baseComponent: r.component,
              components: r.components,
              breadcrumbs: r.breadcrumbs,
              total: r.total,
              page: r.page,
              sourceViewer: null,
              searchResults: null
            });
          }
        }
      })
      .catch(e => {
        if (this.mounted) {
          this.setState({ loading: false });
          parseError(e).then(this.handleError.bind(this));
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
    const { baseComponent, page } = this.state;
    const isPortfolio = ['VW', 'SVW'].includes(this.props.component.qualifier);
    loadMoreChildren(baseComponent.key, page + 1, isPortfolio, getBranchName(this.props.branch))
      .then(r => {
        if (this.mounted) {
          this.setState({
            components: [...this.state.components, ...r.components],
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

  handleError = error => {
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

    const shouldShowSourceViewer = !!sourceViewer;
    const shouldShowComponents = !shouldShowSourceViewer && components;
    const shouldShowBreadcrumbs = Array.isArray(breadcrumbs) && breadcrumbs.length > 1;

    const componentsClassName = classNames('spacer-top', { 'new-loading': loading });

    return (
      <div className="page page-limited">
        <Helmet title={translate('code.page')} />

        {error &&
          <div className="alert alert-danger">
            {error}
          </div>}

        <Search location={location} component={component} onError={this.handleError} />

        <div className="code-components">
          {shouldShowBreadcrumbs &&
            <Breadcrumbs rootComponent={component} breadcrumbs={breadcrumbs} />}

          {shouldShowComponents &&
            <div className={componentsClassName}>
              <Components
                rootComponent={component}
                baseComponent={baseComponent}
                components={components}
              />
            </div>}

          {shouldShowComponents &&
            <ListFooter count={components.length} total={total} loadMore={this.handleLoadMore} />}

          {shouldShowSourceViewer &&
            <div className="spacer-top">
              <SourceViewer branch={getBranchName(branch)} component={sourceViewer.key} />
            </div>}
        </div>
      </div>
    );
  }
}
