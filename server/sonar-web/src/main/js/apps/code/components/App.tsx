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
import { Location } from 'history';
import Components from './Components';
import Breadcrumbs from './Breadcrumbs';
import Search from './Search';
import SourceViewerWrapper from './SourceViewerWrapper';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import { retrieveComponentChildren, retrieveComponent, loadMoreChildren } from '../utils';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics } from '../../../store/rootReducer';
import { isSameBranchLike } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import '../code.css';

interface StateToProps {
  metrics: { [metric: string]: T.Metric };
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

interface OwnProps {
  branchLike?: T.BranchLike;
  component: T.Component;
  location: Pick<Location, 'query'>;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  baseComponent?: T.ComponentMeasure;
  breadcrumbs: T.Breadcrumb[];
  components?: T.ComponentMeasure[];
  loading: boolean;
  page: number;
  searchResults?: T.ComponentMeasure[];
  sourceViewer?: T.ComponentMeasure;
  total: number;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    breadcrumbs: [],
    total: 0,
    page: 0
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

  handleComponentChange() {
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
  }

  loadComponent(componentKey: string) {
    this.setState({ loading: true });

    retrieveComponent(componentKey, this.props.component.qualifier, this.props.branchLike).then(
      r => {
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
      },
      this.stopLoading
    );
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
    loadMoreChildren(
      baseComponent.key,
      page + 1,
      this.props.component.qualifier,
      this.props.branchLike
    ).then(r => {
      if (this.mounted) {
        this.setState({
          components: [...components, ...r.components],
          page: r.page,
          total: r.total
        });
      }
    }, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { branchLike, component, location } = this.props;
    const { loading, baseComponent, components, breadcrumbs, total, sourceViewer } = this.state;
    const shouldShowBreadcrumbs = breadcrumbs.length > 1;

    const componentsClassName = classNames('boxed-group', 'spacer-top', {
      'new-loading': loading
    });

    const defaultTitle =
      baseComponent && ['APP', 'VW', 'SVW'].includes(baseComponent.qualifier)
        ? translate('projects.page')
        : translate('code.page');

    return (
      <div className="page page-limited">
        <Suggestions suggestions="code" />
        <Helmet title={sourceViewer !== undefined ? sourceViewer.name : defaultTitle} />

        <Search branchLike={branchLike} component={component} />

        <div className="code-components">
          {shouldShowBreadcrumbs && (
            <Breadcrumbs
              branchLike={branchLike}
              breadcrumbs={breadcrumbs}
              rootComponent={component}
            />
          )}

          {sourceViewer === undefined &&
            components !== undefined && (
              <div className={componentsClassName}>
                <Components
                  baseComponent={baseComponent}
                  branchLike={branchLike}
                  components={components}
                  metrics={this.props.metrics}
                  rootComponent={component}
                />
              </div>
            )}

          {sourceViewer === undefined &&
            components !== undefined && (
              <ListFooter count={components.length} loadMore={this.handleLoadMore} total={total} />
            )}

          {sourceViewer !== undefined && (
            <div className="spacer-top">
              <SourceViewerWrapper
                branchLike={branchLike}
                component={sourceViewer.key}
                location={location}
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
