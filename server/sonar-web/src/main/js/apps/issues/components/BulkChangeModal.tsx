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
import { pickBy, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Modal from 'sonar-ui-common/components/controls/Modal';
import Radio from 'sonar-ui-common/components/controls/Radio';
import SearchSelect from 'sonar-ui-common/components/controls/SearchSelect';
import Select from 'sonar-ui-common/components/controls/Select';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { bulkChangeIssues, searchIssueTags } from '../../../api/issues';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import MarkdownTips from '../../../components/common/MarkdownTips';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import Avatar from '../../../components/ui/Avatar';
import { isLoggedIn, isUserActive } from '../../../helpers/users';
import { searchAssignees } from '../utils';

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

export const MAX_PAGE_SIZE = 500;

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
      ([{ issues, paging }, tags]) => {
        if (this.mounted) {
          if (issues.length > MAX_PAGE_SIZE) {
            issues = issues.slice(0, MAX_PAGE_SIZE);
          }

          this.setState({
            initialTags: tags.map(tag => ({ label: tag, value: tag })),
            issues,
            loading: false,
            paging
          });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadIssues = () => {
    return this.props.fetchIssues({ additionalFields: 'actions,transitions', ps: MAX_PAGE_SIZE });
  };

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

  handleAssigneeSearch = (query: string) => {
    return searchAssignees(query, this.state.organization).then(({ results }) =>
      results.map(r => ({
        avatar: r.avatar,
        label: isUserActive(r) ? r.name : translateWithParameters('user.x_deleted', r.login),
        value: r.login
      }))
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

  handleRadioTransitionChange = (transition: string) => {
    this.setState({ transition });
  };

  handleCommentChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    this.setState({ comment: event.currentTarget.value });
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
    const transitions: T.Dict<number> = {};
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

  renderLoading = () => (
    <div>
      <div className="modal-head">
        <h2>{translate('bulk_change')}</h2>
      </div>
      <div className="modal-body">
        <div className="text-center">
          <i className="spinner spacer" />
        </div>
      </div>
      <div className="modal-foot">
        <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
      </div>
    </div>
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
        className="input-super-large"
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
        className="input-super-large"
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
        className="input-super-large"
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
        className="input-super-large"
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
          <span
            className="bulk-change-radio-button display-flex-center display-flex-space-between"
            key={transition.transition}>
            <Radio
              checked={this.state.transition === transition.transition}
              onCheck={this.handleRadioTransitionChange}
              value={transition.transition}>
              {translate('issue.transition', transition.transition)}
            </Radio>
            {this.renderAffected(transition.count)}
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
        <textarea
          id="comment"
          onChange={this.handleCommentChange}
          rows={4}
          value={this.state.comment || ''}
        />
        <MarkdownTips className="modal-field-descriptor text-right" />
      </div>
    );
  };

  renderNotificationsField = () => (
    <Checkbox
      checked={this.state.notifications !== undefined}
      className="display-inline-block spacer-top"
      id="send-notifications"
      onCheck={this.handleFieldCheck('notifications')}
      right={true}>
      <strong className="little-spacer-right">{translate('issue.send_notifications')}</strong>
    </Checkbox>
  );

  renderForm = () => {
    const { issues, paging, submitting } = this.state;

    const limitReached = paging && paging.total > MAX_PAGE_SIZE;

    return (
      <form id="bulk-change-form" onSubmit={this.handleSubmit}>
        <div className="modal-head">
          <h2>{translateWithParameters('issue_bulk_change.form.title', issues.length)}</h2>
        </div>

        <div className="modal-body modal-container">
          {limitReached && (
            <Alert variant="warning">
              <FormattedMessage
                defaultMessage={translate('issue_bulk_change.max_issues_reached')}
                id="issue_bulk_change.max_issues_reached"
                values={{ max: <strong>{MAX_PAGE_SIZE}</strong> }}
              />
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
          <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    );
  };

  render() {
    return (
      <Modal contentLabel="modal" onRequestClose={this.props.onClose} size="small">
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
