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
// @flow
import React from 'react';
import { pickBy, sortBy } from 'lodash';
import SearchSelect from '../../../components/controls/SearchSelect';
import Checkbox from '../../../components/controls/Checkbox';
import Modal from '../../../components/controls/Modal';
import Select, { Creatable } from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import MarkdownTips from '../../../components/common/MarkdownTips';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import Avatar from '../../../components/ui/Avatar';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { searchIssueTags, bulkChangeIssues } from '../../../api/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { searchAssignees } from '../utils';
/*:: import type { Paging, Component, CurrentUser } from '../utils'; */
/*:: import type { Issue } from '../../../components/issue/types'; */

/*::
type Props = {|
  component?: Component,
  currentUser: CurrentUser,
  fetchIssues: ({}) => Promise<*>,
  onClose: () => void,
  onDone: () => void,
  organization?: { key: string }
|};
*/

/*::
type State = {|
  issues: Array<Issue>,
  // used for initial loading of issues
  loading: boolean,
  paging?: Paging,
  // used when submitting a form
  submitting: boolean,
  tags?: Array<string>,

  // form fields
  addTags?: Array<string>,
  assignee?: string,
  comment?: string,
  notifications?: boolean,
  organization?: string,
  removeTags?: Array<string>,
  severity?: string,
  transition?: string,
  type?: string
|};
*/

const hasAction = (action /*: string */) => (issue /*: Issue */) =>
  issue.actions && issue.actions.includes(action);

export default class BulkChangeModal extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    let organization = props.component && props.component.organization;
    if (props.organization && !organization) {
      organization = props.organization.key;
    }
    this.state = { issues: [], loading: true, submitting: false, organization };
  }

  componentDidMount() {
    this.mounted = true;

    Promise.all([
      this.loadIssues(),
      searchIssueTags({ organization: this.state.organization })
    ]).then(([issues, tags]) => {
      if (this.mounted) {
        this.setState({
          issues: issues.issues,
          loading: false,
          paging: issues.paging,
          tags
        });
      }
    });
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadIssues = () => this.props.fetchIssues({ additionalFields: 'actions,transitions', ps: 250 });

  getDefaultAssignee = () => {
    const { currentUser } = this.props;
    const { issues } = this.state;
    const options = [];

    if (currentUser.isLoggedIn) {
      const canBeAssignedToMe =
        issues.filter(issue => issue.assignee !== currentUser.login).length > 0;
      if (canBeAssignedToMe) {
        options.push({
          avatar: currentUser.avatar,
          label: currentUser.name,
          value: currentUser.login
        });
      }
    }

    const canBeUnassigned = issues.filter(issue => issue.assignee).length > 0;
    if (canBeUnassigned) {
      options.push({ label: translate('unassigned'), value: '' });
    }

    return options;
  };

  handleCloseClick = (e /*: Event & { target: HTMLElement } */) => {
    e.preventDefault();
    e.target.blur();
    this.props.onClose();
  };

  handleAssigneeSearch = (query /*: string */) => searchAssignees(query, this.state.organization);

  handleAssigneeSelect = (assignee /*: string */) => {
    this.setState({ assignee });
  };

  handleFieldCheck = (field /*: string */) => (checked /*: boolean */) => {
    if (!checked) {
      this.setState({ [field]: undefined });
    } else if (field === 'notifications') {
      this.setState({ [field]: true });
    }
  };

  handleFieldChange = (field /*: string */) => (event /*: { target: HTMLInputElement } */) => {
    this.setState({ [field]: event.target.value });
  };

  handleSelectFieldChange = (field /*: string */) => ({ value } /*: { value: string } */) => {
    this.setState({ [field]: value });
  };

  handleMultiSelectFieldChange = (field /*: string */) => (
    options /*: Array<{ value: string }> */
  ) => {
    this.setState({ [field]: options.map(option => option.value) });
  };

  handleSubmit = (e /*: Event */) => {
    e.preventDefault();
    const query = pickBy(
      {
        assign: this.state.assignee,
        set_type: this.state.type,
        set_severity: this.state.severity,
        add_tags: this.state.addTags && this.state.addTags.join(),
        remove_tags: this.state.removeTags && this.state.removeTags.join(),
        do_transition: this.state.transition,
        comment: this.state.comment,
        sendNotifications: this.state.notifications
      },
      // remove null, but keep empty string
      x => x != null
    );
    const issueKeys = this.state.issues.map(issue => issue.key);

    this.setState({ submitting: true });
    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.setState({ submitting: false });
        this.props.onDone();
      },
      (error /*: Error */) => {
        this.setState({ submitting: false });
        throwGlobalError(error);
      }
    );
  };

  getAvailableTransitions(
    issues /*: Array<Issue> */
  ) /*: Array<{ transition: string, count: number }> */ {
    const transitions = {};
    issues.forEach(issue => {
      if (issue.transitions) {
        issue.transitions.forEach(t => {
          if (transitions[t] != null) {
            transitions[t]++;
          } else {
            transitions[t] = 1;
          }
        });
      }
    });
    return sortBy(Object.keys(transitions)).map(transition => ({
      transition,
      count: transitions[transition]
    }));
  }

  renderCancelButton = () => (
    <a id="bulk-change-cancel" href="#" onClick={this.handleCloseClick}>
      {translate('cancel')}
    </a>
  );

  renderLoading = () => (
    <div>
      <div className="modal-head">
        <h2>{translate('bulk_change')}</h2>
      </div>
      <div className="modal-body">
        <div className="text-center">
          <i className="spinner spinner-margin" />
        </div>
      </div>
      <div className="modal-foot">{this.renderCancelButton()}</div>
    </div>
  );

  renderCheckbox = (field /*: string */) => (
    <Checkbox checked={this.state[field] != null} onCheck={this.handleFieldCheck(field)} />
  );

  renderAffected = (affected /*: number */) => (
    <div className="pull-right note">
      ({translateWithParameters('issue_bulk_change.x_issues', affected)})
    </div>
  );

  renderField = (
    field /*: string */,
    label /*: string */,
    affected /*: ?number */,
    input /*: Object */
  ) => (
    <div className="modal-field" id={`issues-bulk-change-${field}`}>
      <label htmlFor={field}>{translate(label)}</label>
      {this.renderCheckbox(field)}
      {input}
      {affected != null && this.renderAffected(affected)}
    </div>
  );

  renderAssigneeOption = (option /*: { avatar?: string, email?: string, label: string } */) => {
    return (
      <span>
        {option.avatar != null && (
          <Avatar className="spacer-right" hash={option.avatar} name={option.label} size={16} />
        )}
        {option.label}
      </span>
    );
  };

  renderAssigneeField = () => {
    const affected /*: number */ = this.state.issues.filter(hasAction('assign')).length;

    if (affected === 0) {
      return null;
    }

    const input = (
      <SearchSelect
        defaultOptions={this.getDefaultAssignee()}
        onSearch={this.handleAssigneeSearch}
        onSelect={this.handleAssigneeSelect}
        renderOption={this.renderAssigneeOption}
        resetOnBlur={false}
        value={this.state.assignee}
      />
    );

    return this.renderField('assignee', 'issue.assign.formlink', affected, input);
  };

  renderTypeField = () => {
    const affected /*: number */ = this.state.issues.filter(hasAction('set_type')).length;

    if (affected === 0) {
      return null;
    }

    const types = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const options = types.map(type => ({ label: translate('issue.type', type), value: type }));

    const optionRenderer = (option /*: { label: string, value: string } */) => (
      <span>
        <IssueTypeIcon className="little-spacer-right" query={option.value} />
        {option.label}
      </span>
    );

    const input = (
      <Select
        clearable={false}
        id="type"
        onChange={this.handleSelectFieldChange('type')}
        options={options}
        optionRenderer={optionRenderer}
        searchable={false}
        value={this.state.type}
        valueRenderer={optionRenderer}
      />
    );

    return this.renderField('type', 'issue.set_type', affected, input);
  };

  renderSeverityField = () => {
    const affected /*: number */ = this.state.issues.filter(hasAction('set_severity')).length;

    if (affected === 0) {
      return null;
    }

    const severities = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];
    const options = severities.map(severity => ({
      label: translate('severity', severity),
      value: severity
    }));

    const input = (
      <Select
        clearable={false}
        id="severity"
        onChange={this.handleSelectFieldChange('severity')}
        options={options}
        optionRenderer={option => <SeverityHelper severity={option.value} />}
        searchable={false}
        value={this.state.severity}
        valueRenderer={option => <SeverityHelper severity={option.value} />}
      />
    );

    return this.renderField('severity', 'issue.set_severity', affected, input);
  };

  renderTagsField = (field /*: string */, label /*: string */, allowCreate /*: boolean */) => {
    const affected /*: number */ = this.state.issues.filter(hasAction('set_tags')).length;

    if (this.state.tags == null || affected === 0) {
      return null;
    }

    const Component = allowCreate ? Creatable : Select;

    const options = [...this.state.tags, ...(this.state[field] || [])].map(tag => ({
      label: tag,
      value: tag
    }));

    const input = (
      <Component
        clearable={false}
        id={field}
        multi={true}
        onChange={this.handleMultiSelectFieldChange(field)}
        options={options}
        promptTextCreator={promptCreateTag}
        searchable={true}
        value={this.state[field]}
      />
    );

    return this.renderField(field, label, affected, input);
  };

  renderTransitionsField = () => {
    const transitions = this.getAvailableTransitions(this.state.issues);

    if (transitions.length === 0) {
      return null;
    }

    return (
      <div className="modal-field">
        <label>{translate('issue.transition')}</label>
        {transitions.map(transition => (
          <span key={transition.transition}>
            <input
              checked={this.state.transition === transition.transition}
              id={`transition-${transition.transition}`}
              name="do_transition.transition"
              onChange={this.handleFieldChange('transition')}
              type="radio"
              value={transition.transition}
            />
            <label
              htmlFor={`transition-${transition.transition}`}
              style={{ float: 'none', display: 'inline', left: 0, cursor: 'pointer' }}>
              {translate('issue.transition', transition.transition)}
            </label>
            {this.renderAffected(transition.count)}
            <br />
          </span>
        ))}
      </div>
    );
  };

  renderCommentField = () => {
    const affected /*: number */ = this.state.issues.filter(hasAction('comment')).length;

    if (affected === 0) {
      return null;
    }

    return (
      <div className="modal-field">
        <label htmlFor="comment">
          {translate('issue.comment.formlink')}
          <Tooltip overlay={translate('issue_bulk_change.comment.help')}>
            <i className="icon-help little-spacer-left" />
          </Tooltip>
        </label>
        <div>
          <textarea
            id="comment"
            onChange={this.handleFieldChange('comment')}
            rows="4"
            style={{ width: '100%' }}
            value={this.state.comment || ''}
          />
        </div>
        <div className="pull-right">
          <MarkdownTips />
        </div>
      </div>
    );
  };

  renderNotificationsField = () => (
    <div className="modal-field">
      <label htmlFor="send-notifications">{translate('issue.send_notifications')}</label>
      {this.renderCheckbox('notifications')}
    </div>
  );

  renderForm = () => {
    const { issues, paging, submitting } = this.state;

    const limitReached /*: boolean */ =
      paging != null && paging.total > paging.pageIndex * paging.pageSize;

    return (
      <form id="bulk-change-form" onSubmit={this.handleSubmit}>
        <div className="modal-head">
          <h2>{translateWithParameters('issue_bulk_change.form.title', issues.length)}</h2>
        </div>

        <div className="modal-body">
          {limitReached && (
            <div className="alert alert-warning">
              {translateWithParameters('issue_bulk_change.max_issues_reached', issues.length)}
            </div>
          )}

          {this.renderAssigneeField()}
          {this.renderTypeField()}
          {this.renderSeverityField()}
          {this.renderTagsField('addTags', 'issue.add_tags', true)}
          {this.renderTagsField('removeTags', 'issue.remove_tags', false)}
          {this.renderTransitionsField()}
          {this.renderCommentField()}
          {this.renderNotificationsField()}
        </div>

        <div className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <button disabled={submitting} id="bulk-change-submit">
            {translate('apply')}
          </button>
          {this.renderCancelButton()}
        </div>
      </form>
    );
  };

  render() {
    return (
      <Modal contentLabel="modal" onRequestClose={this.props.onClose}>
        {this.state.loading ? this.renderLoading() : this.renderForm()}
      </Modal>
    );
  }
}

function promptCreateTag(label /*: string */) {
  return `+ ${label}`;
}
