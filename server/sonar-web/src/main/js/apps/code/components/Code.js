import classNames from 'classnames';
import React, { Component } from 'react';
import { connect } from 'react-redux';

import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import SourceViewer from './SourceViewer';
import Search from './Search';
import { initComponent, browse } from '../actions';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';


class Code extends Component {
  componentDidMount () {
    const { dispatch, component, routing } = this.props;
    const selectedKey = (routing.path && decodeURIComponent(routing.path.substr(1))) || component.key;
    dispatch(initComponent(component.key, component.breadcrumbs))
        .then(() => dispatch(browse(selectedKey)));
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

  handleBrowse (component) {
    const { dispatch } = this.props;
    dispatch(browse(component.key));
  }

  render () {
    const { fetching, baseComponent, components, breadcrumbs, sourceViewer, coverageMetric, searchResults } = this.props;
    const shouldShowBreadcrumbs = Array.isArray(breadcrumbs) && breadcrumbs.length > 1;
    const shouldShowSearchResults = !!searchResults;
    const shouldShowSourceViewer = !!sourceViewer;
    const shouldShowComponents = !shouldShowSearchResults && !shouldShowSourceViewer && components;

    const componentsClassName = classNames('spacer-top', { 'new-loading': fetching });

    return (
        <TooltipsContainer options={{ delay: { show: 500, hide: 0 } }}>
          <div className="page">
            <header className="page-header">
              <h1 className="page-title">{window.t('code.page')}</h1>

              <div
                  className="pull-left"
                  style={{ visibility: fetching ? 'visible' : 'hidden' }}>
                <i className="spinner"/>
              </div>

              <Search component={this.props.component}/>
            </header>

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
        </TooltipsContainer>
    );
  }
}

export default connect(state => {
  return Object.assign({ routing: state.routing }, state.current);
})(Code);
