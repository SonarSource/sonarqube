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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import ListFooter from '../../../components/controls/ListFooter';
import { getProjectActivity } from '../../../store/rootReducer';
import { getAnalyses, getPaging } from '../../../store/projectActivity/duck';
import { fetchMoreProjectActivity } from '../actions';
import type { Paging } from '../../../store/projectActivity/duck';

class ProjectActivityPageFooter extends React.Component {
  props: {
    analyses: Array<*>,
    paging: ?Paging,
    project: string,
    fetchMoreProjectActivity: (project: string) => void
  };

  handleLoadMore = () => {
    this.props.fetchMoreProjectActivity(this.props.project);
  };

  render () {
    const { analyses, paging } = this.props;

    if (!paging || analyses.length === 0) {
      return null;
    }

    return (
        <ListFooter count={analyses.length} total={paging.total} loadMore={this.handleLoadMore}/>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  analyses: getAnalyses(getProjectActivity(state), ownProps.project),
  paging: getPaging(getProjectActivity(state), ownProps.project)
});

const mapDispatchToProps = { fetchMoreProjectActivity };

export default connect(mapStateToProps, mapDispatchToProps)(ProjectActivityPageFooter);
