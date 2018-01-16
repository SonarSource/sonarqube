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
import { connect } from 'react-redux';
import { uniq } from 'lodash';
import Facet, { BasicProps } from './Facet';
import LanguageFacetFooter from './LanguageFacetFooter';
import { getLanguages } from '../../../store/rootReducer';

interface StateProps {
  referencedLanguages: { [language: string]: { key: string; name: string } };
}

interface Props extends BasicProps, StateProps {}

class LanguageFacet extends React.PureComponent<Props> {
  getLanguageName = (language: string) => {
    const { referencedLanguages } = this.props;
    return referencedLanguages[language] ? referencedLanguages[language].name : language;
  };

  handleSelect = (language: string) => {
    const { values } = this.props;
    this.props.onChange({ languages: uniq([...values, language]) });
  };

  renderFooter = () => {
    if (!this.props.stats) {
      return null;
    }

    return (
      <LanguageFacetFooter
        onSelect={this.handleSelect}
        referencedLanguages={this.props.referencedLanguages}
      />
    );
  };

  render() {
    const { referencedLanguages, ...facetProps } = this.props;
    return (
      <Facet
        {...facetProps}
        property="languages"
        renderFooter={this.renderFooter}
        renderName={this.getLanguageName}
        renderTextName={this.getLanguageName}
      />
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({
  referencedLanguages: getLanguages(state)
});

export default connect(mapStateToProps)(LanguageFacet);
