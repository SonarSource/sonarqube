import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import ChangeLog from './models/changelog';
import ChangeLogView from './views/changelog-view';
import ActionPlans from './collections/action-plans';
import TransitionsFormView from './views/transitions-form-view';
import AssignFormView from './views/assign-form-view';
import CommentFormView from './views/comment-form-view';
import PlanFormView from './views/plan-form-view';
import SetSeverityFormView from './views/set-severity-form-view';
import MoreActionsView from './views/more-actions-view';
import TagsFormView from './views/tags-form-view';
import Workspace from '../workspace/main';
import Template from './templates/issue.hbs';

export default Marionette.ItemView.extend({
  className: 'issue',
  template: Template,

  modelEvents: {
    'change': 'render',
    'transition': 'onTransition'
  },

  events: function () {
    return {
      'click .js-issue-comment': 'onComment',
      'click .js-issue-comment-edit': 'editComment',
      'click .js-issue-comment-delete': 'deleteComment',
      'click .js-issue-transition': 'transition',
      'click .js-issue-set-severity': 'setSeverity',
      'click .js-issue-assign': 'assign',
      'click .js-issue-assign-to-me': 'assignToMe',
      'click .js-issue-plan': 'plan',
      'click .js-issue-show-changelog': 'showChangeLog',
      'click .js-issue-more': 'showMoreActions',
      'click .js-issue-rule': 'showRule',
      'click .js-issue-edit-tags': 'editTags',
      'click .js-issue-locations': 'showLocations'
    };
  },

  onRender: function () {
    this.$el.attr('data-key', this.model.get('key'));
  },

  disableControls: function () {
    this.$(':input').prop('disabled', true);
  },

  enableControls: function () {
    this.$(':input').prop('disabled', false);
  },

  resetIssue: function (options) {
    var that = this;
    var key = this.model.get('key'),
        componentUuid = this.model.get('componentUuid');
    this.model.reset({ key: key, componentUuid: componentUuid }, { silent: true });
    return this.model.fetch(options).done(function () {
      return that.trigger('reset');
    });
  },

  showChangeLog: function (e) {
    var that = this;
    var t = $(e.currentTarget),
        changeLog = new ChangeLog();
    return changeLog.fetch({
      data: { issue: this.model.get('key') }
    }).done(function () {
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

  updateAfterAction: function (fetch) {
    if (this.popup) {
      this.popup.destroy();
    }
    if (fetch) {
      this.resetIssue();
    }
  },

  onComment: function (e) {
    e.stopPropagation();
    this.comment();
  },

  comment: function (options) {
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

  editComment: function (e) {
    e.stopPropagation();
    $('body').click();
    var commentEl = $(e.currentTarget).closest('.issue-comment'),
        commentKey = commentEl.data('comment-key'),
        comment = _.findWhere(this.model.get('comments'), { key: commentKey });
    this.popup = new CommentFormView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: new Backbone.Model(comment),
      issue: this.model,
      detailView: this
    });
    this.popup.render();
  },

  deleteComment: function (e) {
    var that = this;
    var commentKey = $(e.target).closest('[data-comment-key]').data('comment-key'),
        confirmMsg = $(e.target).data('confirm-msg');
    if (confirm(confirmMsg)) {
      this.disableControls();
      return $.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/delete_comment?key=' + commentKey
      }).done(function () {
        that.updateAfterAction(true);
      });
    }
  },

  transition: function (e) {
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

  setSeverity: function (e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new SetSeverityFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model
    });
    this.popup.render();
  },

  assign: function (e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new AssignFormView({
      triggerEl: $(e.currentTarget),
      bottom: true,
      model: this.model
    });
    this.popup.render();
  },

  assignToMe: function () {
    var view = new AssignFormView({
      model: this.model,
      triggerEl: $('body')
    });
    view.submit(window.SS.user, window.SS.userName);
    view.destroy();
  },

  plan: function (e) {
    var that = this;
    var t = $(e.currentTarget),
        actionPlans = new ActionPlans();
    return actionPlans.fetch({
      reset: true,
      data: { project: this.model.get('project') }
    }).done(function () {
      e.stopPropagation();
      $('body').click();
      that.popup = new PlanFormView({
        triggerEl: t,
        bottom: true,
        model: that.model,
        collection: actionPlans
      });
      that.popup.render();
    });
  },

  showRule: function () {
    var ruleKey = this.model.get('rule');
    Workspace.openRule({ key: ruleKey });
  },

  showMoreActions: function (e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new MoreActionsView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: this.model,
      detailView: this
    });
    this.popup.render();
  },

  action: function (action) {
    var that = this;
    this.disableControls();
    return this.model.customAction(action)
        .done(function () {
          that.updateAfterAction(true);
        })
        .fail(function () {
          that.enableControls();
        });
  },

  editTags: function (e) {
    e.stopPropagation();
    $('body').click();
    this.popup = new TagsFormView({
      triggerEl: $(e.currentTarget),
      bottomRight: true,
      model: this.model
    });
    this.popup.render();
  },

  showLocations: function () {
    this.model.trigger('locations', this.model);
  },

  onTransition: function (transition) {
    if (transition === 'falsepositive' || transition === 'wontfix') {
      this.comment({ fromTransition: true });
    }
  },

  serializeData: function () {
    var issueKey = encodeURIComponent(this.model.get('key'));
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      permalink: baseUrl + '/issues/search#issues=' + issueKey,
      hasSecondaryLocations: this.model.get('flows').length
    });
  }
});


