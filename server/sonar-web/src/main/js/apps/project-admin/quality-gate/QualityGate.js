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
import { connect } from 'react-redux';
import shallowCompare from 'react-addons-shallow-compare';
import Header from './Header';
import Form from './Form';
import GlobalMessagesContainer from '../components/GlobalMessagesContainer';
import { fetchProjectGate, setProjectGate } from '../store/actions';
import { getProjectAdminAllGates, getProjectAdminProjectGate } from '../../../app/store/rootReducer';

class QualityGate extends React.Component {
  static propTypes = {
    component: React.PropTypes.object.isRequired,
    allGates: React.PropTypes.array,
    gate: React.PropTypes.object
  };

  componentDidMount () {
    this.props.fetchProjectGate(this.props.component.key);
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleChangeGate (oldId, newId) {
    this.props.setProjectGate(this.props.component.key, oldId, newId);
  }

  render () {
    return (
        <div id="project-quality-gate" className="page page-limited">
          <Header/>
          <GlobalMessagesContainer/>
          <Form
              allGates={this.props.allGates}
              gate={this.props.gate}
              onChange={this.handleChangeGate.bind(this)}/>
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  allGates: getProjectAdminAllGates(state),
  gate: getProjectAdminProjectGate(state, ownProps.component.key)
});

export default connect(
    mapStateToProps,
    { fetchProjectGate, setProjectGate }
)(QualityGate);
