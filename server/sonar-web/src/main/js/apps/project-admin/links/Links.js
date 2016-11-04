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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import { connect } from 'react-redux';
import Header from './Header';
import Table from './Table';
import DeletionModal from './views/DeletionModal';
import {
    fetchProjectLinks,
    deleteProjectLink,
    createProjectLink
} from '../store/actions';
import { getProjectAdminProjectLinks } from '../../../app/store/rootReducer';

class Links extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired,
    links: React.PropTypes.array
  };

  componentWillMount () {
    this.handleCreateLink = this.handleCreateLink.bind(this);
    this.handleDeleteLink = this.handleDeleteLink.bind(this);
  }

  componentDidMount () {
    this.props.fetchProjectLinks(this.props.component.key);
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleCreateLink (name, url) {
    return this.props.createProjectLink(this.props.component.key, name, url);
  }

  handleDeleteLink (link) {
    new DeletionModal({ link }).on('done', () => {
      this.props.deleteProjectLink(this.props.component.key, link.id);
    }).render();
  }

  render () {
    return (
        <div className="page page-limited">
          <Header
              onCreate={this.handleCreateLink}/>
          <Table
              links={this.props.links}
              onDelete={this.handleDeleteLink}/>
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  links: getProjectAdminProjectLinks(state, ownProps.component.key)
});

export default connect(
    mapStateToProps,
    { fetchProjectLinks, createProjectLink, deleteProjectLink }
)(Links);
