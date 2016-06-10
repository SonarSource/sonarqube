/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import SourceViewer from './../../../components/source-viewer/SourceViewer';
import Search from './Search';
import ListFooter from '../../../components/controls/ListFooter';
import { retrieveComponentBase, retrieveComponent, loadMoreChildren, parseError } from '../utils';
import { addComponentBreadcrumbs } from '../bucket';
import { selectCoverageMetric } from '../../../helpers/measures';

import '../code.css';

export default class App extends React.Component {
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

  componentDidMount () {
    this.mounted = true;
    this.handleComponentChange();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.component !== this.props.component) {
      this.handleComponentChange();
    } else if (prevProps.location !== this.props.location) {
      this.handleUpdate();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  handleComponentChange () {
    const { component } = this.props;

    // we already know component's breadcrumbs,
    addComponentBreadcrumbs(component.key, component.breadcrumbs);

    this.setState({ loading: true });
    retrieveComponentBase(component.key).then(component => {
      const prefix = selectCoverageMetric(component.measures);
      this.coverageMetric = `${prefix}coverage`;
      this.handleUpdate();
    }).catch(e => {
      if (this.mounted) {
        this.setState({ loading: false });
        parseError(e).then(this.handleError.bind(this));
      }
    });
  }

  loadComponent (componentKey) {
    this.setState({ loading: true });

    retrieveComponent(componentKey).then(r => {
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
    }).catch(e => {
      if (this.mounted) {
        this.setState({ loading: false });
        parseError(e).then(this.handleError.bind(this));
      }
    });
  }

  handleUpdate () {
    const { component, location } = this.props;
    const { selected } = location.query;
    const finalKey = selected || component.key;

    this.loadComponent(finalKey);
  }

  handleLoadMore () {
    const { baseComponent, page } = this.state;
    loadMoreChildren(baseComponent.key, page + 1).then(r => {
      if (this.mounted) {
        this.setState({
          components: [...this.state.components, ...r.components],
          page: r.page,
          total: r.total
        });
      }
    }).catch(e => {
      if (this.mounted) {
        this.setState({ loading: false });
        parseError(e).then(this.handleError.bind(this));
      }
    });
  }

  handleError (error) {
    if (this.mounted) {
      this.setState({ error });
    }
  }

  render () {
    const { component, location } = this.props;
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
          {error && (
              <div className="alert alert-danger">
                {error}
              </div>
          )}

          <Search
              location={location}
              component={component}
              onError={this.handleError.bind(this)}/>


          <div className="code-components">
            {shouldShowBreadcrumbs && (
                <Breadcrumbs
                    rootComponent={component}
                    breadcrumbs={breadcrumbs}/>
            )}

            {shouldShowComponents && (
                <div className={componentsClassName}>
                  <Components
                      rootComponent={component}
                      baseComponent={baseComponent}
                      components={components}
                      coverageMetric={this.coverageMetric}/>
                </div>
            )}

            {shouldShowComponents && (
                <ListFooter
                    count={components.length}
                    total={total}
                    loadMore={this.handleLoadMore.bind(this)}/>
            )}

            {shouldShowSourceViewer && (
                <div className="spacer-top">
                  <SourceViewer component={sourceViewer}/>
                </div>
            )}
          </div>
        </div>
    );
  }
}
