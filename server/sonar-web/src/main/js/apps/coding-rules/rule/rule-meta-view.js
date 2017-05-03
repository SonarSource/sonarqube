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
import { difference, union } from 'lodash';
import Marionette from 'backbone.marionette';
import RuleFilterMixin from './rule-filter-mixin';
import Template from '../templates/rule/coding-rules-rule-meta.hbs';

export default Marionette.ItemView.extend(RuleFilterMixin).extend({
  template: Template,

  modelEvents: {
    change: 'render'
  },

  ui: {
    tagsChange: '.coding-rules-detail-tags-change',
    tagInput: '.coding-rules-detail-tag-input',
    tagsEdit: '.coding-rules-detail-tag-edit',
    tagsEditDone: '.coding-rules-detail-tag-edit-done',
    tagsEditCancel: '.coding-rules-details-tag-edit-cancel',
    tagsList: '.coding-rules-detail-tag-list'
  },

  events: {
    'click @ui.tagsChange': 'changeTags',
    'click @ui.tagsEditDone': 'editDone',
    'click @ui.tagsEditCancel': 'cancelEdit',
    'click .js-rule-filter': 'onRuleFilterClick'
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({
      container: 'body'
    });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  requestTags() {
    const url = window.baseUrl + '/api/rules/tags';
    const data = this.options.app.organization
      ? { organization: this.options.app.organization }
      : undefined;
    return $.get(url, data);
  },

  changeTags() {
    const that = this;
    this.requestTags().done(r => {
      that.ui.tagInput.select2({
        tags: difference(difference(r.tags, that.model.get('tags')), that.model.get('sysTags')),
        width: '300px'
      });

      that.ui.tagsEdit.removeClass('hidden');
      that.ui.tagsList.addClass('hidden');
      that.tagsBuffer = that.ui.tagInput.select2('val');
      that.ui.tagInput.select2('open');
    });
  },

  cancelEdit() {
    this.ui.tagsList.removeClass('hidden');
    this.ui.tagsEdit.addClass('hidden');
    if (this.ui.tagInput.select2) {
      this.ui.tagInput.select2('val', this.tagsBuffer);
      this.ui.tagInput.select2('close');
    }
  },

  editDone() {
    const that = this;
    const tags = this.ui.tagInput.val();
    const data = { key: this.model.get('key'), tags };
    if (this.options.app.organization) {
      data.organization = this.options.app.organization;
    }
    return $.ajax({
      type: 'POST',
      url: window.baseUrl + '/api/rules/update',
      data
    })
      .done(r => {
        that.model.set('tags', r.rule.tags);
        that.cancelEdit();
      })
      .always(() => {
        that.cancelEdit();
      });
  },

  serializeData() {
    const permalinkPath = this.options.app.organization
      ? `/organizations/${this.options.app.organization}/rules`
      : '/coding_rules';

    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      canCustomizeRule: this.options.app.canWrite,
      allTags: union(this.model.get('sysTags'), this.model.get('tags')),
      permalink: window.baseUrl + permalinkPath + '#rule_key=' + encodeURIComponent(this.model.id)
    };
  }
});
