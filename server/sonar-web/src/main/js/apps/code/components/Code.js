/*
 * SonarQube :: Web
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
import React, { Component } from 'react';
import { connect } from 'react-redux';

import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import SourceViewer from './SourceViewer';
import Search from './Search';
import { initComponent, browse, search } from '../actions';
import { translate } from '../../../helpers/l10n';


class Code extends Component {
  componentDidMount () {
    const { dispatch, component, routing } = this.props;
    const selectedKey = (routing.path && decodeURIComponent(routing.path.substr(1))) || component.key;

    dispatch(initComponent(component.key, component.breadcrumbs))
        .then(() => dispatch(browse(selectedKey)));

    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.attachShortcuts();
  }

  componentWillReceiveProps (nextProps) {
    if (nextProps.routing !== this.props.routing) {
      const { dispatch, routing, component, fetching } = nextProps;
      if (!fetching) {
        const selectedKey = (routing.path && decodeURIComponent(routing.path.substr(1))) || component.key;
        dispatch(browse(selectedKey));
      }
    }
  }

  componentWillUnmount () {
    this.removeShortcuts();
  }

  attachShortcuts () {
    window.addEventListener('keyup', this.handleKeyDown);
  }

  removeShortcuts () {
    window.removeEventListener('keyup', this.handleKeyDown);
  }

  handleBrowse (component) {
    const { dispatch } = this.props;
    dispatch(browse(component.key));
  }

  handleKeyDown (e) {
    const { dispatch, component, searchQuery } = this.props;

    // "t" key
    if (e.keyCode === 84 && searchQuery == null) {
      dispatch(search('', component));
    }
  }

  render () {
    const {
        fetching,
        baseComponent,
        components,
        breadcrumbs,
        sourceViewer,
        coverageMetric,
        searchResults,
        errorMessage } = this.props;
    const shouldShowSearchResults = !!searchResults;
    const shouldShowSourceViewer = !!sourceViewer;
    const shouldShowComponents = !shouldShowSearchResults && !shouldShowSourceViewer && components;
    const shouldShowBreadcrumbs = !shouldShowSearchResults && Array.isArray(breadcrumbs) && breadcrumbs.length > 1;

    const componentsClassName = classNames('spacer-top', { 'new-loading': fetching });

    return (
        <div className="page">
          <header className="page-header">
            <h1 className="page-title">{translate('code.page')}</h1>

            <div
                className="pull-left"
                style={{ visibility: fetching ? 'visible' : 'hidden' }}>
              <i className="spinner"/>
            </div>

            <div className="page-actions">
              <Search component={this.props.component}/>
            </div>
          </header>

          {errorMessage && (
              <div className="alert alert-danger">
                {errorMessage}
              </div>
          )}

          {shouldShowBreadcrumbs && (
              <Breadcrumbs
                  breadcrumbs={breadcrumbs}
                  onBrowse={this.handleBrowse.bind(this)}/>
          )}

          {shouldShowSearchResults && (
              <div className={componentsClassName}>
                <Components
                    components={searchResults}
                    onBrowse={this.handleBrowse.bind(this)}/>
              </div>
          )}

          {shouldShowComponents && (
              <div className={componentsClassName}>
                <Components
                    baseComponent={baseComponent}
                    components={components}
                    coverageMetric={coverageMetric}
                    onBrowse={this.handleBrowse.bind(this)}/>
              </div>
          )}

          {shouldShowSourceViewer && (
              <div className="spacer-top">
                <SourceViewer component={sourceViewer}/>
              </div>
          )}
        </div>
    );
  }
}

export default connect(state => {
  return Object.assign({ routing: state.routing }, state.current);
})(Code);
