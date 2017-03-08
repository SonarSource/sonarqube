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
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import Filter from './Filter';
import LanguageFilterFooter from './LanguageFilterFooter';
import LanguageFilterOption from './LanguageFilterOption';
import {
  getProjectsAppFacetByProperty,
  getProjectsAppMaxFacetValue,
  getLanguages,
  getLanguageByKey
} from '../../../store/rootReducer';

export const FilterContainer = (function () {
  const mapStateToProps = (state, ownProps) => ({
    value: ownProps.query[ownProps.property],
    facet: getProjectsAppFacetByProperty(state, ownProps.property),
    maxFacetValue: getProjectsAppMaxFacetValue(state)
  });
  return connect(mapStateToProps)(withRouter(Filter));
})();

export const LanguageFilterFooterContainer = (function () {
  const mapStateToProps = (state, ownProps) => ({
    languages: getLanguages(state),
    value: ownProps.query[ownProps.property],
    facet: getProjectsAppFacetByProperty(state, ownProps.property)
  });
  return connect(mapStateToProps)(withRouter(LanguageFilterFooter));
})();

export const LanguageFilterOptionContainer = (function () {
  const mapStateToProps = (state, ownProps) => ({
    language: getLanguageByKey(state, ownProps.languageKey)
  });
  return connect(mapStateToProps)(LanguageFilterOption);
})();
