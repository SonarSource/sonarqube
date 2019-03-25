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
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import { InjectedRouter } from 'react-router';
import { Location } from 'history';
import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import Search from './Search';
import SourceViewerWrapper from './SourceViewerWrapper';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import { retrieveComponentChildren, retrieveComponent, loadMoreChildren } from '../utils';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics } from '../../../store/rootReducer';
import { isSameBranchLike } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import { getProjectUrl, getCodeUrl } from '../../../helpers/urls';
import '../code.css';

interface StateToProps {
  metrics: T.Dict<T.Metric>;
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

interface OwnProps {
  branchLike?: T.BranchLike;
  component: T.Component;
  location: Pick<Location, 'query'>;
  router: Pick<InjectedRouter, 'push'>;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  baseComponent?: T.ComponentMeasure;
  breadcrumbs: T.Breadcrumb[];
  components?: T.ComponentMeasure[];
  highlighted?: T.ComponentMeasure;
  loading: boolean;
  page: number;
  searchResults?: T.ComponentMeasure[];
  sourceViewer?: T.ComponentMeasure;
  total: number;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    breadcrumbs: [],
    loading: true,
    page: 0,
    total: 0
  };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
    this.handleComponentChange();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.component !== this.props.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike)
    ) {
      this.handleComponentChange();
    } else if (prevProps.location !== this.props.location) {
      this.handleUpdate();
    }
  }

  componentWillUnmount() {
    clearBucket();
    this.mounted = false;
  }

  loadComponent = (componentKey: string) => {
    this.setState({ loading: true });
    retrieveComponent(componentKey, this.props.component.qualifier, this.props.branchLike).then(
      r => {
        if (this.mounted) {
          if (['FIL', 'UTS'].includes(r.component.qualifier)) {
            this.setState({
              breadcrumbs: r.breadcrumbs,
              components: r.components,
              loading: false,
              page: 0,
              searchResults: undefined,
              sourceViewer: r.component,
              total: 0
            });
          } else {
            this.setState({
              baseComponent: r.component,
              breadcrumbs: r.breadcrumbs,
              components: r.components,
              loading: false,
              page: r.page,
              searchResults: undefined,
              sourceViewer: undefined,
              total: r.total
            });
          }
        }
      },
      this.stopLoading
    );
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleComponentChange = () => {
    const { branchLike, component } = this.props;

    // we already know component's breadcrumbs,
    addComponentBreadcrumbs(component.key, component.breadcrumbs);

    this.setState({ loading: true });
    retrieveComponentChildren(component.key, component.qualifier, branchLike).then(() => {
      addComponent(component);
      if (this.mounted) {
        this.handleUpdate();
      }
    }, this.stopLoading);
  };

  handleLoadMore = () => {
    const { baseComponent, components, page } = this.state;
    if (!baseComponent || !components) {
      return;
    }
    loadMoreChildren(
      baseComponent.key,
      page + 1,
      this.props.component.qualifier,
      this.props.branchLike
    ).then(r => {
      if (this.mounted && r.components.length) {
        this.setState({
          components: [...components, ...r.components],
          page: r.page,
          total: r.total
        });
      }
    }, this.stopLoading);
  };

  handleGoToParent = () => {
    const { branchLike, component } = this.props;
    const { breadcrumbs = [] } = this.state;

    if (breadcrumbs.length > 1) {
      const parentComponent = breadcrumbs[breadcrumbs.length - 2];
      this.props.router.push(getCodeUrl(component.key, branchLike, parentComponent.key));
      this.setState({ highlighted: breadcrumbs[breadcrumbs.length - 1] });
    }
  };

  handleHighlight = (highlighted: T.ComponentMeasure) => {
    this.setState({ highlighted });
  };

  handleSearchClear = () => {
    this.setState({ searchResults: undefined });
  };

  handleSearchResults = (searchResults: T.ComponentMeasure[] = []) => {
    this.setState({ searchResults });
  };

  handleSelect = (component: T.ComponentMeasure) => {
    const { branchLike, component: rootComponent } = this.props;

    if (component.refKey) {
      this.props.router.push(getProjectUrl(component.refKey));
    } else {
      this.props.router.push(getCodeUrl(rootComponent.key, branchLike, component.key));
    }

    this.setState({ highlighted: undefined });
  };

  handleUpdate = () => {
    const { component, location } = this.props;
    const { selected } = location.query;
    const finalKey = selected || component.key;

    this.loadComponent(finalKey);
  };

  render() {
    const { branchLike, component, location } = this.props;
    const {
      baseComponent,
      breadcrumbs,
      components = [],
      highlighted,
      loading,
      total,
      searchResults,
      sourceViewer
    } = this.state;

    const showSearch = searchResults !== undefined;

    const shouldShowBreadcrumbs = breadcrumbs.length > 1 && !showSearch;
    const shouldShowComponentList =
      sourceViewer === undefined && components.length > 0 && !showSearch;

    const componentsClassName = classNames('boxed-group', 'spacer-top', {
      'new-loading': loading,
      'search-results': showSearch
    });

    const defaultTitle =
      baseComponent && ['APP', 'VW', 'SVW'].includes(baseComponent.qualifier)
        ? translate('projects.page')
        : translate('code.page');

    return (
      <div className="page page-limited">
        <Suggestions suggestions="code" />
        <Helmet title={sourceViewer !== undefined ? sourceViewer.name : defaultTitle} />
        <A11ySkipTarget anchor="code_main" />

        <Search
          branchLike={branchLike}
          component={component}
          onSearchClear={this.handleSearchClear}
          onSearchResults={this.handleSearchResults}
        />

        <div className="code-components">
          {shouldShowBreadcrumbs && (
            <Breadcrumbs
              branchLike={branchLike}
              breadcrumbs={breadcrumbs}
              rootComponent={component}
            />
          )}

          {shouldShowComponentList && (
            <>
              <div className={componentsClassName}>
                <Components
                  baseComponent={baseComponent}
                  branchLike={branchLike}
                  components={components}
                  cycle={true}
                  metrics={this.props.metrics}
                  onEndOfList={this.handleLoadMore}
                  onGoToParent={this.handleGoToParent}
                  onHighlight={this.handleHighlight}
                  onSelect={this.handleSelect}
                  rootComponent={component}
                  selected={highlighted}
                />
              </div>
              <ListFooter count={components.length} loadMore={this.handleLoadMore} total={total} />
            </>
          )}

          {showSearch && searchResults && (
            <div className={componentsClassName}>
              <Components
                branchLike={this.props.branchLike}
                components={searchResults}
                metrics={{}}
                onHighlight={this.handleHighlight}
                onSelect={this.handleSelect}
                rootComponent={component}
                selected={highlighted}
              />
            </div>
          )}

          {sourceViewer !== undefined && !showSearch && (
            <div className="spacer-top">
              <SourceViewerWrapper
                branchLike={branchLike}
                component={sourceViewer.key}
                isFile={true}
                location={location}
                onGoToParent={this.handleGoToParent}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: any): StateToProps => ({
  metrics: getMetrics(state)
});

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

export default connect<StateToProps, DispatchToProps, Props>(
  mapStateToProps,
  mapDispatchToProps
)(App);
