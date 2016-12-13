/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import Select from 'react-select';
import { translate } from '../../../helpers/l10n';
import { changeFilter } from '../actions';
import { getProjectActivity } from '../../../store/rootReducer';
import { getFilter } from '../../../store/projectActivity/duck';

type Props = {
  changeFilter: (project: string, filter: ?string) => void,
  filter: ?string,
  project: string
};

class ProjectActivityPageHeader extends React.Component {
  props: Props;

  handleChange = (option?: { value: ?string }) => {
    this.props.changeFilter(this.props.project, option && option.value);
  }

  render () {
    const selectOptions = ['VERSION', 'QUALITY_GATE', 'QUALITY_PROFILE', 'OTHER'].map(category => ({
      label: translate('event.category', category),
      value: category
    }));

    return (
        <header className="page-header">
          <h1 className="page-title">
            {translate('project_activity.page')}
          </h1>

          <div className="page-actions">
            <Select
                className="input-medium"
                placeholder={translate('filter_verb') + '...'}
                clearable={true}
                searchable={false}
                value={this.props.filter}
                options={selectOptions}
                onChange={this.handleChange}/>
          </div>
        </header>
    );
  }
}

const mapStateToProps = (state, ownProps: Props) => ({
  filter: getFilter(getProjectActivity(state), ownProps.project)
});

const mapDispatchToProps = { changeFilter };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectActivityPageHeader);
