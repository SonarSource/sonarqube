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
import difference from 'lodash/difference';
import without from 'lodash/without';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-tags-form.hbs';
import OptionTemplate from '../templates/issue-tags-form-option.hbs';

export default ActionOptionsView.extend({
  template: Template,
  optionTemplate: OptionTemplate,

  modelEvents: {
    'change:tags': 'renderTags'
  },

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
    this.query = '';
    this.tags = [];
    this.selected = 0;
    this.debouncedSearch = debounce(this.search, 250);
    this.requestTags();
  },

  requestTags (query) {
    const that = this;
    return $.get(window.baseUrl + '/api/issues/tags', { ps: 10, q: query }).done(data => {
      that.tags = data.tags;
      that.renderTags();
    });
  },

  onRender () {
    const that = this;
    ActionOptionsView.prototype.onRender.apply(this, arguments);
    this.renderTags();
    setTimeout(() => {
      that.$('input').focus();
    }, 100);
  },

  selectInitialOption () {
    this.selected = Math.max(Math.min(this.selected, this.getOptions().length - 1), 0);
    this.makeActive(this.getOptions().eq(this.selected));
  },

  filterTags (tags) {
    return tags.filter(tag => tag.indexOf(this.query) !== -1);
  },

  renderTags () {
    this.$('.menu').empty();
    this.filterTags(this.getTags()).forEach(this.renderSelectedTag, this);
    this.filterTags(difference(this.tags, this.getTags())).forEach(this.renderTag, this);
    if (this.query.length > 0 && this.tags.indexOf(this.query) === -1 && this.getTags().indexOf(this.query) === -1) {
      this.renderCustomTag(this.query);
    }
    this.selectInitialOption();
  },

  renderSelectedTag (tag) {
    const html = this.optionTemplate({
      tag,
      selected: true,
      custom: false
    });
    return this.$('.menu').append(html);
  },

  renderTag (tag) {
    const html = this.optionTemplate({
      tag,
      selected: false,
      custom: false
    });
    return this.$('.menu').append(html);
  },

  renderCustomTag (tag) {
    const html = this.optionTemplate({
      tag,
      selected: false,
      custom: true
    });
    return this.$('.menu').append(html);
  },

  selectOption (e) {
    e.preventDefault();
    e.stopPropagation();
    let tags = this.getTags().slice();
    const tag = $(e.currentTarget).data('value');
    if ($(e.currentTarget).data('selected') != null) {
      tags = without(tags, tag);
    } else {
      tags.push(tag);
    }
    this.selected = this.getOptions().index($(e.currentTarget));
    return this.submit(tags);
  },

  submit (tags) {
    const that = this;
    const _tags = this.getTags();
    this.model.set({ tags });
    return $.ajax({
      type: 'POST',
      url: window.baseUrl + '/api/issues/set_tags',
      data: {
        key: this.model.id,
        tags: tags.join()
      }
    }).fail(() => that.model.set({ tags: _tags }));
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
    const query = this.$('input').val();
    if (query !== this.query) {
      this.query = query;
      this.debouncedSearch(query);
    }
  },

  search (query) {
    this.query = query;
    return this.requestTags(query);
  },

  resetAssignees (users) {
    this.assignees = users.map(user => {
      return { id: user.login, text: user.name };
    });
    this.renderTags();
  },

  getTags () {
    return this.model.get('tags') || [];
  }
});

