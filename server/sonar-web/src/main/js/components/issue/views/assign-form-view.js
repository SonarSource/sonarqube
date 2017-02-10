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
import $ from 'jquery';
import debounce from 'lodash/debounce';
import uniqBy from 'lodash/uniqBy';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-assign-form.hbs';
import OptionTemplate from '../templates/issue-assign-form-option.hbs';
import { translate } from '../../../helpers/l10n';
import getCurrentUserFromStore from '../../../app/utils/getCurrentUserFromStore';

export default ActionOptionsView.extend({
  template: Template,
  optionTemplate: OptionTemplate,

  events () {
    return {
      ...ActionOptionsView.prototype.events.apply(this, arguments),
      'click input': 'onInputClick',
      'keydown input': 'onInputKeydown',
      'keyup input': 'onInputKeyup'
    };
  },

  initialize () {
    ActionOptionsView.prototype.initialize.apply(this, arguments);
    this.assignees = null;
    this.debouncedSearch = debounce(this.search, 250);
  },

  getAssignee () {
    return this.model.get('assignee');
  },

  getAssigneeName () {
    return this.model.get('assigneeName');
  },

  onRender () {
    const that = this;
    ActionOptionsView.prototype.onRender.apply(this, arguments);
    this.renderTags();
    setTimeout(() => {
      that.$('input').focus();
    }, 100);
  },

  renderTags () {
    this.$('.menu').empty();
    this.getAssignees().forEach(this.renderAssignee, this);
    this.bindUIElements();
    this.selectInitialOption();
  },

  renderAssignee (assignee) {
    const html = this.optionTemplate(assignee);
    this.$('.menu').append(html);
  },

  selectOption (e) {
    const assignee = $(e.currentTarget).data('value');
    const assigneeName = $(e.currentTarget).data('text');
    this.submit(assignee, assigneeName);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit (assignee) {
    return this.model.assign(assignee);
  },

  onInputClick (e) {
    e.stopPropagation();
  },

  onInputKeydown (e) {
    this.query = this.$('input').val();
    if (e.keyCode === 38) {
      this.selectPreviousOption();
    }
    if (e.keyCode === 40) {
      this.selectNextOption();
    }
    if (e.keyCode === 13) {
      this.selectActiveOption();
    }
    if (e.keyCode === 27) {
      this.destroy();
    }
    if ([9, 13, 27, 38, 40].indexOf(e.keyCode) !== -1) {
      return false;
    }
  },

  onInputKeyup () {
    let query = this.$('input').val();
    if (query !== this.query) {
      if (query.length < 2) {
        query = '';
      }
      this.query = query;
      this.debouncedSearch(query);
    }
  },

  search (query) {
    const that = this;
    if (query.length > 1) {
      $.get(window.baseUrl + '/api/users/search', { q: query }).done(data => {
        that.resetAssignees(data.users);
      });
    } else {
      this.resetAssignees();
    }
  },

  resetAssignees (users) {
    if (users) {
      this.assignees = users.map(user => {
        return { id: user.login, text: user.name };
      });
    } else {
      this.assignees = null;
    }
    this.renderTags();
  },

  getAssignees () {
    if (this.assignees) {
      return this.assignees;
    }
    const currentUser = getCurrentUserFromStore();
    const assignees = [
      { id: currentUser.login, text: currentUser.name },
      { id: '', text: translate('unassigned') }
    ];
    return this.makeUnique(assignees);
  },

  makeUnique (assignees) {
    return uniqBy(assignees, assignee => assignee.id);
  }
});
