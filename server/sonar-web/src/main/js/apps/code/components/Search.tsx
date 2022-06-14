/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { isEmpty, omit } from 'lodash';
import * as React from 'react';
import { getTree } from '../../../api/components';
import SearchBox from '../../../components/controls/SearchBox';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { ComponentMeasure } from '../../../types/types';
import PortfolioNewCodeToggle from './PortfolioNewCodeToggle';

interface Props {
  branchLike?: BranchLike;
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

export class Search extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    query: '',
    loading: false
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
        loading: false
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
      router.replace({
        pathname: location.pathname,
        query: { ...location.query, search: query }
      });

      const isPortfolio = ['VW', 'SVW', 'APP'].includes(component.qualifier);
      const qualifiers = isPortfolio ? 'SVW,TRK' : 'UTS,FIL';

      getTree({
        component: component.key,
        q: query,
        s: 'qualifier,name',
        qualifiers,
        ...getBranchLikeQuery(branchLike)
      })
        .then(r => {
          if (this.mounted) {
            this.setState({
              loading: false
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
    const { component, newCodeSelected } = this.props;
    const { loading, query } = this.state;
    const isPortfolio = ['VW', 'SVW', 'APP'].includes(component.qualifier);

    return (
      <div className="code-search" id="code-search">
        {isPortfolio && (
          <PortfolioNewCodeToggle
            enabled={isEmpty(query)}
            onNewCodeToggle={this.props.onNewCodeToggle}
            showNewCode={newCodeSelected}
          />
        )}
        <SearchBox
          minLength={3}
          onChange={this.handleQueryChange}
          onKeyDown={this.handleKeyDown}
          placeholder={translate(
            isPortfolio ? 'code.search_placeholder.portfolio' : 'code.search_placeholder'
          )}
          value={this.state.query}
        />
        <DeferredSpinner className="spacer-left" loading={loading} />
      </div>
    );
  }
}

export default withRouter(Search);
