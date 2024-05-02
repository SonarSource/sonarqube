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
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { Location, Router } from '~sonar-aligned/types/router';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { CodeScope, getCodeUrl, getProjectUrl } from '../../../helpers/urls';
import { WithBranchLikesProps, useBranchesQuery } from '../../../queries/branch';
import { Breadcrumb, Component, ComponentMeasure, Dict, Metric } from '../../../types/types';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import { loadMoreChildren, retrieveComponent, retrieveComponentChildren } from '../utils';
import CodeAppRenderer from './CodeAppRenderer';

interface Props extends WithBranchLikesProps {
  component: Component;
  location: Location;
  metrics: Dict<Metric>;
  router: Router;
}

interface State {
  baseComponent?: ComponentMeasure;
  breadcrumbs: Breadcrumb[];
  components?: ComponentMeasure[];
  highlighted?: ComponentMeasure;
  loading: boolean;
  newCodeSelected: boolean;
  page: number;
  searchResults?: ComponentMeasure[];
  sourceViewer?: ComponentMeasure;
  total: number;
}

class CodeApp extends React.Component<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      breadcrumbs: [],
      loading: true,
      newCodeSelected: true,
      page: 0,
      total: 0,
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.handleComponentChange();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query.selected !== this.props.location.query.selected) {
      this.handleUpdate();
    }
  }

  componentWillUnmount() {
    clearBucket();
    this.mounted = false;
  }

  loadComponent = (componentKey: string) => {
    this.setState({ loading: true });
    retrieveComponent(
      componentKey,
      this.props.component.qualifier,
      this,
      this.props.branchLike,
    ).then((r) => {
      if (
        [ComponentQualifier.File, ComponentQualifier.TestFile].includes(
          r.component.qualifier as ComponentQualifier,
        )
      ) {
        this.setState({
          breadcrumbs: r.breadcrumbs,
          components: r.components,
          loading: false,
          page: 0,
          searchResults: undefined,
          sourceViewer: r.component,
          total: 0,
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
          total: r.total,
        });
      }
    }, this.stopLoading);
  };

  stopLoading = () => {
    this.setState({ loading: false });
  };

  handleComponentChange = () => {
    const { branchLike, component } = this.props;

    // we already know component's breadcrumbs,
    addComponentBreadcrumbs(component.key, component.breadcrumbs);

    this.setState({ loading: true });
    retrieveComponentChildren(component.key, component.qualifier, this, branchLike).then(() => {
      addComponent(component);
      this.handleUpdate();
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
      this,
      this.props.branchLike,
    ).then((r) => {
      if (r.components.length) {
        this.setState({
          components: [...components, ...r.components],
          page: r.page,
          total: r.total,
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

  handleHighlight = (highlighted: ComponentMeasure) => {
    this.setState({ highlighted });
  };

  handleSearchClear = () => {
    this.setState({ searchResults: undefined });
  };

  handleSearchResults = (searchResults: ComponentMeasure[] = []) => {
    this.setState({ searchResults });
  };

  handleSelect = (component: ComponentMeasure) => {
    const { branchLike, component: rootComponent } = this.props;
    const { newCodeSelected } = this.state;

    if (component.refKey) {
      const codeType = newCodeSelected ? CodeScope.New : CodeScope.Overall;
      const url = getProjectUrl(component.refKey, component.branch, codeType);
      this.props.router.push(url);
    } else {
      this.props.router.push(getCodeUrl(rootComponent.key, branchLike, component.key));
    }

    this.setState({ highlighted: undefined });
  };

  handleSelectNewCode = (newCodeSelected: boolean) => {
    this.setState({ newCodeSelected });
  };

  handleUpdate = () => {
    const { component, location } = this.props;
    const { selected } = location.query;
    const finalKey = selected || component.key;

    this.loadComponent(finalKey);
  };

  render() {
    return (
      <CodeAppRenderer
        {...this.props}
        {...this.state}
        handleGoToParent={this.handleGoToParent}
        handleHighlight={this.handleHighlight}
        handleLoadMore={this.handleLoadMore}
        handleSearchClear={this.handleSearchClear}
        handleSearchResults={this.handleSearchResults}
        handleSelect={this.handleSelect}
        handleSelectNewCode={this.handleSelectNewCode}
      />
    );
  }
}

function withBranchLikes<P extends { component?: Component }>(
  WrappedComponent: React.ComponentType<React.PropsWithChildren<P & WithBranchLikesProps>>,
): React.ComponentType<React.PropsWithChildren<Omit<P, 'branchLike' | 'branchLikes'>>> {
  return function WithBranchLike(p: P) {
    const { data, isFetching } = useBranchesQuery(p.component);

    return (
      (isPortfolioLike(p.component?.qualifier) || !isFetching) && (
        <WrappedComponent
          branchLikes={data?.branchLikes ?? []}
          branchLike={data?.branchLike}
          {...p}
        />
      )
    );
  };
}

export default withRouter(withComponentContext(withMetricsContext(withBranchLikes(CodeApp))));
