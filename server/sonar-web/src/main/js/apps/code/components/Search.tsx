/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as PropTypes from 'prop-types';
import * as classNames from 'classnames';
import Components from './Components';
import { getTree } from '../../../api/components';
import { parseError } from '../../../helpers/request';
import { getProjectUrl } from '../../../helpers/urls';
import { Component } from '../types';
import SearchBox from '../../../components/controls/SearchBox';
import { translate } from '../../../helpers/l10n';

interface Props {
  branch?: string;
  component: Component;
  location: {};
  onError: (error: string) => void;
}

interface State {
  query: string;
  loading: boolean;
  results?: Component[];
  selectedIndex?: number;
}

export default class Search extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  state: State = {
    query: '',
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    // if the url has change, reset the current state
    if (nextProps.location !== this.props.location) {
      this.setState({
        query: '',
        loading: false,
        results: undefined,
        selectedIndex: undefined
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSelectNext() {
    const { selectedIndex, results } = this.state;
    if (results != null && selectedIndex != null && selectedIndex < results.length - 1) {
      this.setState({ selectedIndex: selectedIndex + 1 });
    }
  }

  handleSelectPrevious() {
    const { selectedIndex, results } = this.state;
    if (results != null && selectedIndex != null && selectedIndex > 0) {
      this.setState({ selectedIndex: selectedIndex - 1 });
    }
  }

  handleSelectCurrent() {
    const { branch, component } = this.props;
    const { results, selectedIndex } = this.state;
    if (results != null && selectedIndex != null) {
      const selected = results[selectedIndex];

      if (selected.refKey) {
        this.context.router.push(getProjectUrl(selected.refKey));
      } else {
        this.context.router.push({
          pathname: '/code',
          query: { branch, id: component.key, selected: selected.key }
        });
      }
    }
  }

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.handleSelectCurrent();
        break;
      case 38:
        event.preventDefault();
        this.handleSelectPrevious();
        break;
      case 40:
        event.preventDefault();
        this.handleSelectNext();
        break;
      default: // do nothing
    }
  };

  handleSearch = (query: string) => {
    if (this.mounted) {
      const { branch, component, onError } = this.props;
      this.setState({ loading: true });

      const isPortfolio = ['VW', 'SVW', 'APP'].includes(component.qualifier);
      const qualifiers = isPortfolio ? 'SVW,TRK' : 'BRC,UTS,FIL';

      getTree(component.key, { branch, q: query, s: 'qualifier,name', qualifiers })
        .then(r => {
          if (this.mounted) {
            this.setState({
              results: r.components,
              selectedIndex: r.components.length > 0 ? 0 : undefined,
              loading: false
            });
          }
        })
        .catch(e => {
          if (this.mounted) {
            this.setState({ loading: false });
            parseError(e).then(onError);
          }
        });
    }
  };

  handleQueryChange = (query: string) => {
    this.setState({ query });
    if (query.length === 0) {
      this.setState({ results: undefined });
    } else {
      this.handleSearch(query);
    }
  };

  render() {
    const { component } = this.props;
    const { loading, selectedIndex, results } = this.state;
    const selected = selectedIndex != null && results != null ? results[selectedIndex] : undefined;
    const containerClassName = classNames('code-search', {
      'code-search-with-results': results != null
    });
    const isPortfolio = ['VW', 'SVW', 'APP'].includes(component.qualifier);

    return (
      <div id="code-search" className={containerClassName}>
        <SearchBox
          minLength={3}
          onChange={this.handleQueryChange}
          onKeyDown={this.handleKeyDown}
          placeholder={translate(
            isPortfolio ? 'code.search_placeholder.portfolio' : 'code.search_placeholder'
          )}
          value={this.state.query}
        />
        {loading && <i className="spinner spacer-left" />}

        {results != null && (
          <div className="boxed-group boxed-group-inner spacer-top">
            <Components
              branch={this.props.branch}
              components={results}
              rootComponent={component}
              selected={selected}
            />
          </div>
        )}
      </div>
    );
  }
}
