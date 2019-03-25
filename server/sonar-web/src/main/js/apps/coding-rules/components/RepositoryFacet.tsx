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
import { connect } from 'react-redux';
import Facet, { BasicProps } from './Facet';
import { getLanguages, Store } from '../../../store/rootReducer';

interface StateProps {
  referencedLanguages: T.Dict<{ key: string; name: string }>;
}

interface Props extends BasicProps, StateProps {
  referencedRepositories: T.Dict<{ key: string; language: string; name: string }>;
}

class RepositoryFacet extends React.PureComponent<Props> {
  getLanguageName = (languageKey: string) => {
    const { referencedLanguages } = this.props;
    const language = referencedLanguages[languageKey];
    return (language && language.name) || languageKey;
  };

  renderName = (repositoryKey: string) => {
    const { referencedRepositories } = this.props;
    const repository = referencedRepositories[repositoryKey];
    return repository ? (
      <>
        {repository.name}
        <span className="note little-spacer-left">{this.getLanguageName(repository.language)}</span>
      </>
    ) : (
      repositoryKey
    );
  };

  renderTextName = (repositoryKey: string) => {
    const { referencedRepositories } = this.props;
    const repository = referencedRepositories[repositoryKey];
    return (repository && repository.name) || repositoryKey;
  };

  render() {
    const { referencedLanguages, referencedRepositories, ...facetProps } = this.props;
    return (
      <Facet
        {...facetProps}
        property="repositories"
        renderName={this.renderName}
        renderTextName={this.renderTextName}
      />
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  referencedLanguages: getLanguages(state)
});

export default connect(mapStateToProps)(RepositoryFacet);
