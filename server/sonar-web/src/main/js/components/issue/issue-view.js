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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import ChangeLog from './models/changelog';
import ChangeLogView from './views/changelog-view';
import TransitionsFormView from './views/transitions-form-view';
import AssignFormView from './views/assign-form-view';
import CommentFormView from './views/comment-form-view';
import DeleteCommentView from './views/DeleteCommentView';
import SetSeverityFormView from './views/set-severity-form-view';
import SetTypeFormView from './views/set-type-form-view';
import TagsFormView from './views/tags-form-view';
import Template from './templates/issue.hbs';
import getCurrentUserFromStore from '../../app/utils/getCurrentUserFromStore';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    change: 'notifyAndRender',
    transition: 'onTransition'
  },

  className() {
    const hasCheckbox = this.options.onCheck != null;
    return hasCheckbox ? 'issue issue-with-checkbox' : 'issue';
  },

  events() {
    return {
      'click': 'handleClick',
      'click .js-issue-comment': 'onComment',
      'click .js-issue-comment-edit': 'editComment',
      'click .js-issue-comment-delete': 'deleteComment',
      'click .js-issue-transition': 'transition',
      'click .js-issue-set-severity': 'setSeverity',
      'click .js-issue-set-type': 'setType',
      'click .js-issue-assign': 'assign',
      'click .js-issue-assign-to-me': 'assignToMe',
      'click .js-issue-plan': 'plan',
      'click .js-issue-show-changelog': 'showChangeLog',
      'click .js-issue-rule': 'showRule',
      'click .js-issue-edit-tags': 'editTags',
      'click .js-issue-locations': 'showLocations',
      'click .js-issue-filter': 'filterSimilarIssues',
      'click .js-toggle': 'onIssueCheck',
      'click .js-issue-permalink': 'onPermalinkClick'
    };
  },

  notifyAndRender() {
    const { onIssueChange } = this.options;
    if (onIssueChange) {
      onIssueChange(this.model.toJSON());
    }

    // if ConnectedIssue is used, this view can be destroyed just after onIssueChange()
    if (!this.isDestroyed) {
      this.render();
    }
  },

  onRender() {
    this.$el.attr('data-key', this.model.get('key'));
  },

  disableControls() {
    this.$(':input').prop('disabled', true);
  },

  enableControls() {
    this.$(':input').prop('disabled', false);
  },

  resetIssue(options) {
    const that = this;
    const key = this.model.get('key');
    const componentUuid = this.model.get('componentUuid');
    this.model.reset({ key, componentUuid }, { silent: true });
    return this.model.fetch(options).done(() => that.trigger('reset'));
  },

  showChangeLog(e) {
    e.preventDefault();
    e.stopPropagation();
    const that = this;
    const t = $(e.currentTarget);
    const changeLog = new ChangeLog();
    return changeLog
      .fetch({
        data: { issue: this.model.get('key') }
      })
      .done(() => {
        if (that.popup) {
          that.popup.destroy();
        }
        that.popup = new ChangeLogView({
          triggerEl: t,
          bottomRight: true,
          collection: changeLog,
          issue: that.model
        });
        that.popup.render();
      });
  },

  updateAfterAction(response) {
    if (this.popup) {
      this.popup.destroy();
    }
    if (response) {
      this.model.set(this.model.parse(response));
    }
  },

  onComment(e) {
    e.stopPropagation();
    this.comment();
  },

  comment(options) {
    $('body').click();
    this.popup = new CommentFormView({
      triggerEl: this.$('.js-issue-comment'),
      bottom: true,
      issue: this.model,
      detailView: this,
      additionalOptions: options
    });
    this.popup.render();
  },

  editComment(e) {
    e.stopPropagation();
    $('body').click();
    const commentEl = $(e.currentTarget).closest('.issue-comment');
    const commentKey = commentEl.data('comment-key');
    const comment = this.model.get('comments').find(comment => comment.key === commentKey);
    this.popup = new CommentFormView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: new Backbone.Model(comment),
      issue: this.model,
      detailView: this
    });
    this.popup.render();
  },

  deleteComment(e) {
    e.stopPropagation();
    $('body').click();
    const commentEl = $(e.currentTarget).closest('.issue-comment');
    const commentKey = commentEl.data('comment-key');
    this.popup = new DeleteCommentView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      onDelete: () => {
        this.disableControls();
        $.ajax({
          type: 'POST',
          url: window.baseUrl + '/api/issues/delete_comment?key=' + commentKey
        }).done(r => this.updateAfterAction(r));
      }
    });
    this.popup.render();
  },

  transition(e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new TransitionsFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model,
      view: this
    });
    this.popup.render();
  },

  setSeverity(e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new SetSeverityFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model
    });
    this.popup.render();
  },

  setType(e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new SetTypeFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model
    });
    this.popup.render();
  },

  assign(e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new AssignFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model
    });
    this.popup.render();
  },

  assignToMe() {
    const view = new AssignFormView({
      model: this.model,
      triggerEl: $('body')
    });
    const currentUser = getCurrentUserFromStore();
    view.submit(currentUser.login, currentUser.name);
    view.destroy();
  },

  showRule(e) {
    e.preventDefault();
    e.stopPropagation();
    const ruleKey = this.model.get('rule');
    // lazy load Workspace
    const Workspace = require('../workspace/main').default;
    Workspace.openRule({ key: ruleKey });
  },

  action(action) {
    this.disableControls();
    return this.model
      .customAction(action)
      .done(r => this.updateAfterAction(r))
      .fail(() => this.enableControls());
  },

  editTags(e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new TagsFormView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: this.model
    });
    this.popup.render();
  },

  showLocations() {
    this.model.trigger('locations', this.model);
  },

  select() {
    this.$el.addClass('selected');
  },

  unselect() {
    this.$el.removeClass('selected');
  },

  onTransition(transition) {
    if (transition === 'falsepositive' || transition === 'wontfix') {
      this.comment({ fromTransition: true });
    }
  },

  handleClick(e) {
    e.preventDefault();
    const { onClick } = this.options;
    if (onClick) {
      onClick(this.model.get('key'));
    }
  },

  filterSimilarIssues(e) {
    this.options.onFilterClick(e);
  },

  onIssueCheck(e) {
    this.options.onCheck(e);
  },

  onPermalinkClick(e) {
    e.stopPropagation();
  },

  serializeData() {
    const issueKey = encodeURIComponent(this.model.get('key'));
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      permalink: window.baseUrl + '/issues/search#issues=' + issueKey,
      hasSecondaryLocations: this.model.get('flows').length,
      hasSimilarIssuesFilter: this.options.onFilterClick != null,
      hasCheckbox: this.options.onCheck != null,
      checked: this.options.checked
    };
  }
});
