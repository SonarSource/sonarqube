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
import { sortBy } from 'lodash';
import { connect } from 'react-redux';
import Tooltip from '../../../components/controls/Tooltip';
import { getLanguages } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';

class ProjectCardLanguages extends React.PureComponent {
  getLanguageName(key) {
    if (key === '<null>') {
      return translate('unknown');
    }
    const language = this.props.languages[key];
    return language != null ? language.name : key;
  }

  render() {
    const { distribution } = this.props;

    if (distribution == null) {
      return null;
    }
    const parsedLanguages = distribution.split(';').map(item => item.split('='));
    const finalLanguages = sortBy(parsedLanguages, l => -1 * Number(l[1])).map(l =>
      this.getLanguageName(l[0])
    );

    const tooltip = (
      <span>
        {finalLanguages.map(language => <span key={language}>{language}<br /></span>)}
      </span>
    );

    const languagesText =
      finalLanguages.slice(0, 2).join(', ') + (finalLanguages.length > 2 ? ', ...' : '');

    return (
      <div className="project-card-languages">
        <Tooltip placement="bottom" overlay={tooltip}>
          <span>{languagesText}</span>
        </Tooltip>
      </div>
    );
  }
}

const mapStateToProps = state => ({
  languages: getLanguages(state)
});

export default connect(mapStateToProps)(ProjectCardLanguages);
