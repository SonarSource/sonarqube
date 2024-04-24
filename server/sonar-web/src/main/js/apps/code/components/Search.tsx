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
import classNames from 'classnames';
import { InputSearch, Spinner, ToggleButton } from 'design-system';
import { isEmpty, omit } from 'lodash';
import * as React from 'react';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getTree } from '../../../api/components';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike, isView } from '../../../types/component';
import { ComponentMeasure } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  component: ComponentMeasure;
  location: Location;
  newCodeSelected: boolean;
  onSearchClear: () => void;
  onNewCodeToggle: (newCode: boolean) => void;
  onSearchResults: (results?: ComponentMeasure[]) => void;
  router: Router;
}

interface State {
  query: string;
  loading: boolean;
}

class Search extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    query: '',
    loading: false,
  };

  componentDidMount() {
    this.mounted = true;
    if (this.props.location.query.search) {
      this.handleQueryChange(this.props.location.query.search);
    }
  }

  componentDidUpdate(nextProps: Props) {
    // if the component has change, reset the current state
    if (nextProps.location.query.id !== this.props.location.query.id) {
      this.setState({
        query: '',
        loading: false,
      });
      this.props.onSearchClear();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    switch (event.nativeEvent.key) {
      case KeyboardKeys.Enter:
      case KeyboardKeys.UpArrow:
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        event.currentTarget.blur();
        break;
    }
  };

  handleSearch = (query: string) => {
    if (this.mounted) {
      const { branchLike, component, router, location } = this.props;
      this.setState({ loading: true });

      if (query !== location.query.search) {
        router.replace({
          pathname: location.pathname,
          query: { ...location.query, search: query },
        });
      }

      const qualifiers = isView(component.qualifier)
        ? [ComponentQualifier.SubPortfolio, ComponentQualifier.Project].join(',')
        : [ComponentQualifier.TestFile, ComponentQualifier.File].join(',');

      getTree({
        component: component.key,
        q: query,
        s: 'qualifier,name',
        qualifiers,
        ...getBranchLikeQuery(branchLike),
      })
        .then((r) => {
          if (this.mounted) {
            this.setState({
              loading: false,
            });
            this.props.onSearchResults(r.components);
          }
        })
        .catch(() => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        });
    }
  };

  handleQueryChange = (query: string) => {
    const { router, location } = this.props;
    this.setState({ query });
    if (query.length === 0) {
      router.replace({ pathname: location.pathname, query: omit(location.query, 'search') });
      this.props.onSearchClear();
    } else {
      this.handleSearch(query);
    }
  };

  render() {
    const { className, component, newCodeSelected } = this.props;
    const { loading, query } = this.state;
    const isPortfolio = isPortfolioLike(component.qualifier);

    const searchPlaceholder = getSearchPlaceholderLabel(component.qualifier as ComponentQualifier);

    return (
      <div className={classNames('sw-flex sw-items-center', className)} id="code-search">
        {isPortfolio && (
          <div className="sw-mr-4">
            <ToggleButton
              disabled={!isEmpty(query)}
              options={[
                {
                  value: true,
                  label: translate('projects.view.new_code'),
                },
                {
                  value: false,
                  label: translate('projects.view.overall_code'),
                },
              ]}
              value={newCodeSelected}
              onChange={this.props.onNewCodeToggle}
            />
          </div>
        )}
        <InputSearch
          searchInputAriaLabel={searchPlaceholder}
          minLength={3}
          onChange={this.handleQueryChange}
          onKeyDown={this.handleKeyDown}
          placeholder={searchPlaceholder}
          size="large"
          value={this.state.query}
        />
        <Spinner className="sw-ml-2" loading={loading} />
      </div>
    );
  }
}

export default withRouter(Search);

function getSearchPlaceholderLabel(qualifier: ComponentQualifier) {
  switch (qualifier) {
    case ComponentQualifier.Portfolio:
    case ComponentQualifier.SubPortfolio:
      return translate('code.search_placeholder.portfolio');

    case ComponentQualifier.Application:
      return translate('code.search_placeholder.application');

    default:
      return translate('code.search_placeholder');
  }
}
