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
import IssueView from './issue-view';
import IssueModel from './models/issue';
import { receiveIssues } from '../../store/issues/duck';
import type { Issue as IssueType } from './types';

type Model = { toJSON: () => {} };

type Props = {
  checked?: boolean,
  issue: IssueType | Model,
  onCheck?: () => void,
  onClick: () => void,
  onFilterClick?: () => void,
  onIssueChange: ({}) => void,
  selected: boolean
};

class Issue extends React.PureComponent {
  issueView: Object;
  node: HTMLElement;
  props: Props;

  static defaultProps = {
    selected: false
  };

  componentDidMount() {
    this.renderIssueView();
    if (this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUpdate(nextProps: Props) {
    if (!nextProps.selected && this.props.selected) {
      this.unbindShortcuts();
    }
    this.destroyIssueView();
  }

  componentDidUpdate(prevProps: Props) {
    this.renderIssueView();
    if (!prevProps.selected && this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUnmount() {
    if (this.props.selected) {
      this.unbindShortcuts();
    }
    this.destroyIssueView();
  }

  bindShortcuts() {
    document.addEventListener('keypress', this.handleKeyPress);
  }

  unbindShortcuts() {
    document.removeEventListener('keypress', this.handleKeyPress);
  }

  doIssueAction(action: string) {
    this.issueView.$('.js-issue-' + action).click();
  }

  handleKeyPress = (e: Object) => {
    const tagName = e.target.tagName.toUpperCase();
    const shouldHandle = tagName !== 'INPUT' && tagName !== 'TEXTAREA' && tagName !== 'BUTTON';

    if (shouldHandle) {
      switch (e.key) {
        case 'f':
          return this.doIssueAction('transition');
        case 'a':
          return this.doIssueAction('assign');
        case 'm':
          return this.doIssueAction('assign-to-me');
        case 'p':
          return this.doIssueAction('plan');
        case 'i':
          return this.doIssueAction('set-severity');
        case 'c':
          return this.doIssueAction('comment');
        case 't':
          return this.doIssueAction('edit-tags');
      }
    }
  };

  renderIssueView() {
    const model = this.props.issue.toJSON ? this.props.issue : new IssueModel(this.props.issue);
    this.issueView = new IssueView({
      model,
      checked: this.props.checked,
      onCheck: this.props.onCheck,
      onClick: this.props.onClick,
      onFilterClick: this.props.onFilterClick,
      onIssueChange: this.props.onIssueChange
    });
    this.issueView.render().$el.appendTo(this.node);
    if (this.props.selected) {
      this.issueView.select();
    }
  }

  destroyIssueView() {
    this.issueView.destroy();
  }

  render() {
    return <div className="issue-container" ref={node => this.node = node} />;
  }
}

const onIssueChange = issue =>
  dispatch => {
    dispatch(receiveIssues([issue]));
  };

const mapDispatchToProps = { onIssueChange };

export default connect(null, mapDispatchToProps)(Issue);
