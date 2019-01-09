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
import { pickBy, sortBy } from 'lodash';
import { searchAssignees } from '../utils';
import { searchIssueTags, bulkChangeIssues } from '../../../api/issues';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import MarkdownTips from '../../../components/common/MarkdownTips';
import SearchSelect from '../../../components/controls/SearchSelect';
import Checkbox from '../../../components/controls/Checkbox';
import Modal from '../../../components/controls/Modal';
import Select from '../../../components/controls/Select';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import Avatar from '../../../components/ui/Avatar';
import { SubmitButton } from '../../../components/ui/buttons';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Alert } from '../../../components/ui/Alert';
import { isLoggedIn } from '../../../helpers/users';

interface AssigneeOption {
  avatar?: string;
  email?: string;
  label: string;
  value: string;
}

interface TagOption {
  label: string;
  value: string;
}

interface Props {
  component: T.Component | undefined;
  currentUser: T.CurrentUser;
  fetchIssues: (x: {}) => Promise<{ issues: T.Issue[]; paging: T.Paging }>;
  onClose: () => void;
  onDone: () => void;
  organization: { key: string } | undefined;
}

interface FormFields {
  addTags?: Array<{ label: string; value: string }>;
  assignee?: AssigneeOption;
  comment?: string;
  notifications?: boolean;
  organization?: string;
  removeTags?: Array<{ label: string; value: string }>;
  severity?: string;
  transition?: string;
  type?: string;
}

interface State extends FormFields {
  initialTags: Array<{ label: string; value: string }>;
  issues: T.Issue[];
  // used for initial loading of issues
  loading: boolean;
  paging?: T.Paging;
  // used when submitting a form
  submitting: boolean;
}

type AssigneeSelectType = new () => SearchSelect<AssigneeOption>;
const AssigneeSelect = SearchSelect as AssigneeSelectType;

type TagSelectType = new () => SearchSelect<TagOption>;
const TagSelect = SearchSelect as TagSelectType;

export default class BulkChangeModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    let organization = props.component && props.component.organization;
    if (props.organization && !organization) {
      organization = props.organization.key;
    }
    this.state = { initialTags: [], issues: [], loading: true, submitting: false, organization };
  }

  componentDidMount() {
    this.mounted = true;

    Promise.all([
      this.loadIssues(),
      searchIssueTags({ organization: this.state.organization })
    ]).then(
      ([issues, tags]) => {
        if (this.mounted) {
          this.setState({
            initialTags: tags.map(tag => ({ label: tag, value: tag })),
            issues: issues.issues,
            loading: false,
            paging: issues.paging
          });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadIssues = () => this.props.fetchIssues({ additionalFields: 'actions,transitions', ps: 250 });

  getDefaultAssignee = () => {
    const { currentUser } = this.props;
    const { issues } = this.state;
    const options = [];

    if (isLoggedIn(currentUser)) {
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

  handleCloseClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  handleAssigneeSearch = (query: string) => {
    return searchAssignees(query, this.state.organization).then(({ results }) =>
      results.map(r => ({ avatar: r.avatar, label: r.name, value: r.login }))
    );
  };

  handleAssigneeSelect = (assignee: AssigneeOption) => {
    this.setState({ assignee });
  };

  handleTagsSearch = (query: string) => {
    return searchIssueTags({ organization: this.state.organization, q: query }).then(tags =>
      tags.map(tag => ({ label: tag, value: tag }))
    );
  };

  handleTagsSelect = (field: 'addTags' | 'removeTags') => (
    options: Array<{ label: string; value: string }>
  ) => {
    this.setState<keyof FormFields>({ [field]: options });
  };

  handleFieldCheck = (field: keyof FormFields) => (checked: boolean) => {
    if (!checked) {
      this.setState<keyof FormFields>({ [field]: undefined });
    } else if (field === 'notifications') {
      this.setState<keyof FormFields>({ [field]: true });
    }
  };

  handleFieldChange = (field: 'comment' | 'transition') => (
    event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    this.setState<keyof FormFields>({ [field]: event.currentTarget.value });
  };

  handleSelectFieldChange = (field: 'severity' | 'type') => (data: { value: string } | null) => {
    if (data) {
      this.setState<keyof FormFields>({ [field]: data.value });
    } else {
      this.setState<keyof FormFields>({ [field]: undefined });
    }
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const query = pickBy(
      {
        add_tags: this.state.addTags && this.state.addTags.map(t => t.value).join(),
        assign: this.state.assignee ? this.state.assignee.value : null,
        comment: this.state.comment,
        do_transition: this.state.transition,
        remove_tags: this.state.removeTags && this.state.removeTags.map(t => t.value).join(),
        sendNotifications: this.state.notifications,
        set_severity: this.state.severity,
        set_type: this.state.type
      },
      x => x !== undefined
    );

    const issueKeys = this.state.issues.map(issue => issue.key);

    this.setState({ submitting: true });
    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.setState({ submitting: false });
        this.props.onDone();
      },
      error => {
        this.setState({ submitting: false });
        throwGlobalError(error);
      }
    );
  };

  getAvailableTransitions(issues: T.Issue[]) {
    const transitions: { [x: string]: number } = {};
    issues.forEach(issue => {
      if (issue.transitions) {
        issue.transitions.forEach(t => {
          if (transitions[t] !== undefined) {
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
    <a href="#" id="bulk-change-cancel" onClick={this.handleCloseClick}>
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

  renderCheckbox = (field: keyof FormFields, id?: string) => (
    <Checkbox
      checked={this.state[field] !== undefined}
      id={id}
      onCheck={this.handleFieldCheck(field)}
    />
  );

  renderAffected = (affected: number) => (
    <div className="pull-right note">
      ({translateWithParameters('issue_bulk_change.x_issues', affected)})
    </div>
  );

  renderField = (
    field: 'addTags' | 'assignee' | 'removeTags' | 'severity' | 'type',
    label: string,
    affected: number | undefined,
    input: React.ReactNode
  ) => (
    <div className="modal-field" id={`issues-bulk-change-${field}`}>
      <label htmlFor={field}>{translate(label)}</label>
      {input}
      {affected !== undefined && this.renderAffected(affected)}
    </div>
  );

  renderAssigneeOption = (option: AssigneeOption) => {
    return (
      <span>
        {option.avatar !== undefined && (
          <Avatar className="spacer-right" hash={option.avatar} name={option.label} size={16} />
        )}
        {option.label}
      </span>
    );
  };

  renderAssigneeField = () => {
    const affected = this.state.issues.filter(hasAction('assign')).length;

    if (affected === 0) {
      return null;
    }

    const input = (
      <AssigneeSelect
        clearable={true}
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
    const affected = this.state.issues.filter(hasAction('set_type')).length;

    if (affected === 0) {
      return null;
    }

    const types: T.IssueType[] = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const options = types.map(type => ({ label: translate('issue.type', type), value: type }));

    const optionRenderer = (option: { label: string; value: string }) => (
      <span>
        <IssueTypeIcon className="little-spacer-right" query={option.value} />
        {option.label}
      </span>
    );

    const input = (
      <Select
        clearable={true}
        onChange={this.handleSelectFieldChange('type')}
        optionRenderer={optionRenderer}
        options={options}
        searchable={false}
        value={this.state.type}
        valueRenderer={optionRenderer}
      />
    );

    return this.renderField('type', 'issue.set_type', affected, input);
  };

  renderSeverityField = () => {
    const affected = this.state.issues.filter(hasAction('set_severity')).length;

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
        clearable={true}
        onChange={this.handleSelectFieldChange('severity')}
        optionRenderer={(option: { value: string }) => <SeverityHelper severity={option.value} />}
        options={options}
        searchable={false}
        value={this.state.severity}
        valueRenderer={(option: { value: string }) => <SeverityHelper severity={option.value} />}
      />
    );

    return this.renderField('severity', 'issue.set_severity', affected, input);
  };

  renderTagOption = (option: TagOption) => {
    return <span>{option.label}</span>;
  };

  renderTagsField = (field: 'addTags' | 'removeTags', label: string, allowCreate: boolean) => {
    const { initialTags } = this.state;
    const affected = this.state.issues.filter(hasAction('set_tags')).length;

    if (initialTags === undefined || affected === 0) {
      return null;
    }

    const input = (
      <TagSelect
        canCreate={allowCreate}
        clearable={true}
        defaultOptions={this.state.initialTags}
        minimumQueryLength={0}
        multi={true}
        onMultiSelect={this.handleTagsSelect(field)}
        onSearch={this.handleTagsSearch}
        promptTextCreator={promptCreateTag}
        renderOption={this.renderTagOption}
        resetOnBlur={false}
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
          <span className="clearfix" key={transition.transition}>
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
    const affected = this.state.issues.filter(hasAction('comment')).length;

    if (affected === 0) {
      return null;
    }

    return (
      <div className="modal-field">
        <label htmlFor="comment">
          <span className="text-middle">{translate('issue.comment.formlink')}</span>
          <HelpTooltip
            className="spacer-left"
            overlay={translate('issue_bulk_change.comment.help')}
          />
        </label>
        <div>
          <textarea
            id="comment"
            onChange={this.handleFieldChange('comment')}
            rows={4}
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
      {this.renderCheckbox('notifications', 'send-notifications')}
    </div>
  );

  renderForm = () => {
    const { issues, paging, submitting } = this.state;

    const limitReached = paging !== undefined && paging.total > paging.pageIndex * paging.pageSize;

    return (
      <form id="bulk-change-form" onSubmit={this.handleSubmit}>
        <div className="modal-head">
          <h2>{translateWithParameters('issue_bulk_change.form.title', issues.length)}</h2>
        </div>

        <div className="modal-body">
          {limitReached && (
            <Alert variant="warning">
              {translateWithParameters('issue_bulk_change.max_issues_reached', issues.length)}
            </Alert>
          )}

          {this.renderAssigneeField()}
          {this.renderTypeField()}
          {this.renderSeverityField()}
          {this.renderTagsField('addTags', 'issue.add_tags', true)}
          {this.renderTagsField('removeTags', 'issue.remove_tags', false)}
          {this.renderTransitionsField()}
          {this.renderCommentField()}
          {issues.length > 0 && this.renderNotificationsField()}
          {issues.length === 0 && (
            <Alert variant="warning">{translate('issue_bulk_change.no_match')}</Alert>
          )}
        </div>

        <div className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <SubmitButton disabled={submitting || issues.length === 0} id="bulk-change-submit">
            {translate('apply')}
          </SubmitButton>
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

function hasAction(action: string) {
  return (issue: T.Issue) => issue.actions && issue.actions.includes(action);
}

function promptCreateTag(label: string) {
  return `+ ${label}`;
}
