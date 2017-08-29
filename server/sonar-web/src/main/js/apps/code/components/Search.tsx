/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { debounce } from 'lodash';
import Components from './Components';
import { getTree } from '../../../api/components';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../utils';
import { getProjectUrl } from '../../../helpers/urls';
import { Component } from '../types';

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
  input: HTMLInputElement;
  mounted: boolean;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  state: State = {
    query: '',
    loading: false
  };

  componentWillMount() {
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
    this.input.focus();
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

  checkInputValue(query: string) {
    return this.input.value === query;
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

  handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    switch (e.keyCode) {
      case 13:
        e.preventDefault();
        this.handleSelectCurrent();
        break;
      case 38:
        e.preventDefault();
        this.handleSelectPrevious();
        break;
      case 40:
        e.preventDefault();
        this.handleSelectNext();
        break;
      default: // do nothing
    }
  }

  handleSearch = (query: string) => {
    // first time check if value has changed due to debounce
    if (this.mounted && this.checkInputValue(query)) {
      const { branch, component, onError } = this.props;
      this.setState({ loading: true });

      const isPortfolio = ['VW', 'SVW', 'APP'].includes(component.qualifier);
      const qualifiers = isPortfolio ? 'SVW,TRK' : 'BRC,UTS,FIL';

      getTree(component.key, { branch, q: query, s: 'qualifier,name', qualifiers })
        .then(r => {
          // second time check if value has change due to api request
          if (this.mounted && this.checkInputValue(query)) {
            this.setState({
              results: r.components,
              selectedIndex: r.components.length > 0 ? 0 : undefined,
              loading: false
            });
          }
        })
        .catch(e => {
          // second time check if value has change due to api request
          if (this.mounted && this.checkInputValue(query)) {
            this.setState({ loading: false });
            parseError(e).then(onError);
          }
        });
    }
  };

  handleQueryChange(query: string) {
    this.setState({ query });
    if (query.length < 3) {
      this.setState({ results: undefined });
    } else {
      this.handleSearch(query);
    }
  }

  handleInputChange(event: React.SyntheticEvent<HTMLInputElement>) {
    const query = event.currentTarget.value;
    this.handleQueryChange(query);
  }

  handleSubmit(event: React.SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    const query = this.input.value;
    this.handleQueryChange(query);
  }

  render() {
    const { component } = this.props;
    const { query, loading, selectedIndex, results } = this.state;
    const selected = selectedIndex != null && results != null ? results[selectedIndex] : undefined;
    const containerClassName = classNames('code-search', {
      'code-search-with-results': results != null
    });
    const inputClassName = classNames('search-box-input', {
      touched: query.length > 0 && query.length < 3
    });

    return (
      <div id="code-search" className={containerClassName}>
        <form className="search-box" onSubmit={this.handleSubmit.bind(this)}>
          <button className="search-box-submit button-clean">
            <i className="icon-search" />
          </button>

          <input
            ref={node => (this.input = node as HTMLInputElement)}
            onKeyDown={this.handleKeyDown.bind(this)}
            onChange={this.handleInputChange.bind(this)}
            value={query}
            className={inputClassName}
            type="search"
            name="q"
            placeholder={translate('search_verb')}
            maxLength={100}
            autoComplete="off"
          />

          {loading && <i className="spinner spacer-left" />}

          <span className="note spacer-left">{translateWithParameters('select2.tooShort', 3)}</span>
        </form>

        {results != null && (
          <Components
            branch={this.props.branch}
            components={results}
            rootComponent={component}
            selected={selected}
          />
        )}
      </div>
    );
  }
}
