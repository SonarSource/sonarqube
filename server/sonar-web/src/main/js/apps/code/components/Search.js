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
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { debounce } from 'lodash';
import Components from './Components';
import { getTree } from '../../../api/components';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../utils';
import { getComponentUrl } from '../../../helpers/urls';

export default class Search extends React.PureComponent {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  static propTypes = {
    component: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
    onError: PropTypes.func.isRequired
  };

  state = {
    query: '',
    loading: false,
    results: null,
    selectedIndex: null
  };

  componentWillMount() {
    this.handleSearch = debounce(this.handleSearch.bind(this), 250);
  }

  componentDidMount() {
    this.mounted = true;
    this.refs.input.focus();
  }

  componentWillReceiveProps(nextProps) {
    // if the url has change, reset the current state
    if (nextProps.location !== this.props.location) {
      this.setState({
        query: '',
        loading: false,
        results: null,
        selectedIndex: null
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkInputValue(query) {
    return this.refs.input.value === query;
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
    const { component } = this.props;
    const { results, selectedIndex } = this.state;
    if (results != null && selectedIndex != null) {
      const selected = results[selectedIndex];

      if (selected.refKey) {
        window.location = getComponentUrl(selected.refKey);
      } else {
        this.context.router.push({
          pathname: '/code',
          query: {
            id: component.key,
            selected: selected.key
          }
        });
      }
    }
  }

  handleKeyDown(e) {
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

  handleSearch(query) {
    // first time check if value has changed due to debounce
    if (this.mounted && this.checkInputValue(query)) {
      const { component, onError } = this.props;
      this.setState({ loading: true });

      const isView = component.qualifier === 'VW' || component.qualifier === 'SVW';
      const qualifiers = isView ? 'SVW,TRK' : 'BRC,UTS,FIL';

      getTree(component.key, { q: query, s: 'qualifier,name', qualifiers })
        .then(r => {
          // second time check if value has change due to api request
          if (this.mounted && this.checkInputValue(query)) {
            this.setState({
              results: r.components,
              selectedIndex: r.components.length > 0 ? 0 : null,
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
  }

  handleQueryChange(query) {
    this.setState({ query });
    if (query.length < 3) {
      this.setState({ results: null });
    } else {
      this.handleSearch(query);
    }
  }

  handleInputChange(e) {
    const query = e.target.value;
    this.handleQueryChange(query);
  }

  handleSubmit(e) {
    e.preventDefault();
    const query = this.refs.input.value;
    this.handleQueryChange(query);
  }

  render() {
    const { component } = this.props;
    const { query, loading, selectedIndex, results } = this.state;
    const selected = selectedIndex != null && results != null ? results[selectedIndex] : null;
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
            ref="input"
            onKeyDown={this.handleKeyDown.bind(this)}
            onChange={this.handleInputChange.bind(this)}
            value={query}
            className={inputClassName}
            type="search"
            name="q"
            placeholder={translate('search_verb')}
            maxLength="100"
            autoComplete="off"
          />

          {loading && <i className="spinner spacer-left" />}

          <span className="note spacer-left">
            {translateWithParameters('select2.tooShort', 3)}
          </span>
        </form>

        {results != null &&
          <Components rootComponent={component} components={results} selected={selected} />}
      </div>
    );
  }
}
