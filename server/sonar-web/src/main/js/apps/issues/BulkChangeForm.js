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
import { sortBy } from 'lodash';
import ModalForm from '../../components/common/modal-form';
import Template from './templates/BulkChangeForm.hbs';
import getCurrentUserFromStore from '../../app/utils/getCurrentUserFromStore';
import { searchIssues, searchIssueTags, bulkChangeIssues } from '../../api/issues';
import { searchUsers } from '../../api/users';
import { translate, translateWithParameters } from '../../helpers/l10n';

const LIMIT = 500;
const INPUT_WIDTH = '250px';
const MINIMUM_QUERY_LENGTH = 2;
const UNASSIGNED = '<UNASSIGNED>';

type Issue = {
  actions?: Array<string>,
  assignee: string | null,
  transitions?: Array<string>
};

const hasAction = (action: string) =>
  (issue: Issue) => issue.actions && issue.actions.includes(action);

export default ModalForm.extend({
  template: Template,

  initialize() {
    this.issues = null;
    this.paging = null;
    this.tags = null;
    this.loadIssues();
    this.loadTags();
  },

  loadIssues() {
    const { query } = this.options;
    searchIssues({
      ...query,
      additionalFields: 'actions,transitions',
      ps: LIMIT
    }).then(r => {
      this.issues = r.issues;
      this.paging = r.paging;
      this.render();
    });
  },

  loadTags() {
    searchIssueTags().then(r => {
      this.tags = r.tags;
      this.render();
    });
  },

  prepareAssigneeSelect() {
    const input = this.$('#assignee');
    if (input.length) {
      const canBeAssignedToMe = this.issues && this.canBeAssignedToMe(this.issues);
      const currentUser = getCurrentUserFromStore();
      const canBeUnassigned = this.issues && this.canBeUnassigned(this.issues);
      const defaultOptions = [];
      if (canBeAssignedToMe && currentUser.isLoggedIn) {
        defaultOptions.push({
          id: currentUser.login,
          text: `${currentUser.name} (${currentUser.login})`
        });
      }
      if (canBeUnassigned) {
        defaultOptions.push({ id: UNASSIGNED, text: translate('unassigned') });
      }

      input.select2({
        allowClear: false,
        placeholder: translate('search_verb'),
        width: INPUT_WIDTH,
        formatNoMatches: () => translate('select2.noMatches'),
        formatSearching: () => translate('select2.searching'),
        formatInputTooShort: () =>
          translateWithParameters('select2.tooShort', MINIMUM_QUERY_LENGTH),
        query: query => {
          if (query.term.length === 0) {
            query.callback({ results: defaultOptions });
          } else if (query.term.length >= MINIMUM_QUERY_LENGTH) {
            searchUsers(query.term).then(r => {
              query.callback({
                results: r.users.map(user => ({
                  id: user.login,
                  text: `${user.name} (${user.login})`
                }))
              });
            });
          }
        }
      });

      input.on('change', () => this.$('#assign-action').prop('checked', true));
    }
  },

  prepareTypeSelect() {
    this.$('#type')
      .select2({
        minimumResultsForSearch: 999,
        width: INPUT_WIDTH
      })
      .on('change', () => this.$('#set-type-action').prop('checked', true));
  },

  prepareSeveritySelect() {
    const format = state =>
      state.id
        ? `<i class="icon-severity-${state.id.toLowerCase()}"></i> ${state.text}`
        : state.text;
    this.$('#severity')
      .select2({
        minimumResultsForSearch: 999,
        width: INPUT_WIDTH,
        formatResult: format,
        formatSelection: format
      })
      .on('change', () => this.$('#set-severity-action').prop('checked', true));
  },

  prepareTagsInput() {
    this.$('#add_tags')
      .select2({
        width: INPUT_WIDTH,
        tags: this.tags
      })
      .on('change', () => this.$('#add-tags-action').prop('checked', true));

    this.$('#remove_tags')
      .select2({
        width: INPUT_WIDTH,
        tags: this.tags
      })
      .on('change', () => this.$('#remove-tags-action').prop('checked', true));
  },

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.prepareAssigneeSelect();
    this.prepareTypeSelect();
    this.prepareSeveritySelect();
    this.prepareTagsInput();
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    const query = {};

    const assignee = this.$('#assignee').val();
    if (this.$('#assign-action').is(':checked') && assignee != null) {
      query['assign'] = assignee === UNASSIGNED ? '' : assignee;
    }

    const type = this.$('#type').val();
    if (this.$('#set-type-action').is(':checked') && type) {
      query['set_type'] = type;
    }

    const severity = this.$('#severity').val();
    if (this.$('#set-severity-action').is(':checked') && severity) {
      query['set_severity'] = severity;
    }

    const addedTags = this.$('#add_tags').val();
    if (this.$('#add-tags-action').is(':checked') && addedTags) {
      query['add_tags'] = addedTags;
    }

    const removedTags = this.$('#remove_tags').val();
    if (this.$('#remove-tags-action').is(':checked') && removedTags) {
      query['remove_tags'] = removedTags;
    }

    const transition = this.$('[name="do_transition.transition"]:checked').val();
    if (transition) {
      query['do_transition'] = transition;
    }

    const comment = this.$('#comment').val();
    if (comment) {
      query['comment'] = comment;
    }

    const sendNotifications = this.$('#send-notifications').is(':checked');
    if (sendNotifications) {
      query['sendNotifications'] = sendNotifications;
    }

    this.disableForm();
    this.showSpinner();

    const issueKeys = this.issues.map(issue => issue.key);
    bulkChangeIssues(issueKeys, query).then(
      () => {
        this.destroy();
        this.options.onChange();
      },
      (e: Object) => {
        this.enableForm();
        this.hideSpinner();
        e.response.json().then(r => this.showErrors(r.errors, r.warnings));
      }
    );
  },

  canBeAssigned(issues: Array<Issue>) {
    return issues.filter(hasAction('assign')).length;
  },

  canBeAssignedToMe(issues: Array<Issue>) {
    return issues.filter(hasAction('assign_to_me')).length;
  },

  canBeUnassigned(issues: Array<Issue>) {
    return issues.filter(issue => issue.assignee).length;
  },

  canChangeType(issues: Array<Issue>) {
    return issues.filter(hasAction('set_type')).length;
  },

  canChangeSeverity(issues: Array<Issue>) {
    return issues.filter(hasAction('set_severity')).length;
  },

  canChangeTags(issues: Array<Issue>) {
    return issues.filter(hasAction('set_tags')).length;
  },

  canBeCommented(issues: Array<Issue>) {
    return issues.filter(hasAction('comment')).length;
  },

  availableTransitions(issues: Array<Issue>) {
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
  },

  serializeData() {
    return {
      ...ModalForm.prototype.serializeData.apply(this, arguments),
      isLoaded: this.issues != null && this.tags != null,
      issues: this.issues,
      limitReached: this.paging && this.paging.total > LIMIT,
      canBeAssigned: this.issues && this.canBeAssigned(this.issues),
      canChangeType: this.issues && this.canChangeType(this.issues),
      canChangeSeverity: this.issues && this.canChangeSeverity(this.issues),
      canChangeTags: this.issues && this.canChangeTags(this.issues),
      canBeCommented: this.issues && this.canBeCommented(this.issues),
      availableTransitions: this.issues && this.availableTransitions(this.issues)
    };
  }
});
