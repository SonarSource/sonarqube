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
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import Select from 'react-select';
import difference from 'lodash/difference';
import isNil from 'lodash/isNil';
import omitBy from 'lodash/omitBy';
import { getProjectsAppFacetByProperty, getLanguages } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';

class LanguageFilterFooter extends React.Component {
  static propTypes = {
    property: React.PropTypes.string.isRequired,
    query: React.PropTypes.object.isRequired,
    isFavorite: React.PropTypes.bool,
    organization: React.PropTypes.object,
    languages: React.PropTypes.object,
    value: React.PropTypes.any,
    facet: React.PropTypes.object,
    getFilterUrl: React.PropTypes.func.isRequired
  }

  handleLanguageChange = ({ value }) => {
    const urlOptions = (this.props.value || []).concat(value).join(',');
    const path = this.props.getFilterUrl({ [this.props.property]: urlOptions });
    this.props.router.push(path);
  }

  getOptions () {
    const { languages, facet } = this.props;
    let options = Object.keys(languages);
    if (facet) {
      options = difference(options, Object.keys(facet));
    }
    return options.map(key => ({ label: languages[key].name, value: key }));
  }

  render () {
    return (
      <Select
          onChange={this.handleLanguageChange}
          className="input-super-large"
          options={this.getOptions()}
          placeholder={translate('search_verb')}
          clearable={false}
          searchable={true}/>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  languages: getLanguages(state),
  value: ownProps.query[ownProps.property],
  facet: getProjectsAppFacetByProperty(state, ownProps.property),
  getFilterUrl: part => {
    const basePathName = ownProps.organization ?
        `/organizations/${ownProps.organization.key}/projects` :
        '/projects';
    const pathname = basePathName + (ownProps.isFavorite ? '/favorite' : '');
    const query = omitBy({ ...ownProps.query, ...part }, isNil);
    return { pathname, query };
  }
});

export default connect(mapStateToProps)(withRouter(LanguageFilterFooter));
