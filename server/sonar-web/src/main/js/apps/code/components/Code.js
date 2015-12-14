import classNames from 'classnames';
import React, { Component } from 'react';
import { connect } from 'react-redux';

import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import SourceViewer from './SourceViewer';
import { initComponent, fetchComponents, showSource } from '../actions';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';


class Code extends Component {
  componentDidMount () {
    const { dispatch, component } = this.props;
    dispatch(initComponent(component));
  }

  componentWillReceiveProps (nextProps) {
    if (nextProps.component !== this.props.component) {
      const { dispatch, component } = this.props;
      dispatch(initComponent(component));
    }
  }

  hasSourceCode (component) {
    return component.qualifier === 'FIL' || component.qualifier === 'UTS';
  }

  handleBrowse (component) {
    const { dispatch } = this.props;
    if (this.hasSourceCode(component)) {
      dispatch(showSource(component));
    } else {
      dispatch(fetchComponents(component));
    }
  }

  render () {
    const { fetching, baseComponent, components, breadcrumbs, sourceViewer } = this.props;
    const shouldShowBreadcrumbs = Array.isArray(breadcrumbs) && breadcrumbs.length > 1;
    const shouldShowComponents = !sourceViewer && components;
    const shouldShowSourceViewer = sourceViewer;

    const componentsClassName = classNames('spacer-top', { 'new-loading': fetching });

    return (
        <TooltipsContainer options={{ delay: { show: 500, hide: 0 } }}>
          <div className="page">
            <header className="page-header">
              <h1 className="page-title">{window.t('code.page')}</h1>

              {fetching && (
                  <i className="spinner"/>
              )}

              {shouldShowBreadcrumbs && (
                  <Breadcrumbs
                      breadcrumbs={breadcrumbs}
                      onBrowse={this.handleBrowse.bind(this)}/>
              )}
            </header>

            {shouldShowComponents && (
                <div className={componentsClassName}>
                  <Components
                      baseComponent={baseComponent}
                      components={components}
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


export default connect(state => state)(Code);
