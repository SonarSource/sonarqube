define([
  './models/changelog',
  './views/changelog-view',
  './collections/action-plans',
  './views/issue-popup',
  './views/transitions-form-view',
  './views/assign-form-view',
  './views/comment-form-view',
  './views/plan-form-view',
  './views/set-severity-form-view',
  './views/more-actions-view',
  './views/tags-form-view',
  'components/workspace/main',
  './templates'
], function (ChangeLog, ChangeLogView, ActionPlans, IssuePopup, TransitionsFormView, AssignFormView, CommentFormView,
             PlanFormView, SetSeverityFormView, MoreActionsView, TagsFormView, Workspace) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'issue',
    template: Templates.issue,

    modelEvents: {
      'change': 'render'
    },

    ui: {
      tagsChange: '.js-issue-edit-tags',
      tagInput: '.issue-tag-input',
      tagsEdit: '.issue-tag-edit',
      tagsEditDone: '.issue-tag-edit-done',
      tagsEditCancel: '.issue-tag-edit-cancel',
      tagsList: '.issue-tag-list'
    },

    events: function () {
      return {
        'click .js-issue-comment': 'comment',
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
        'click .js-issue-edit-tags': 'editTags'
      };
    },

    onRender: function () {
      this.ui.tagsEdit.hide();
      this.$el.attr('data-key', this.model.get('key'));
    },

    resetIssue: function (options) {
      var that = this;
      var key = this.model.get('key'),
          componentUuid = this.model.get('componentUuid');
      this.model.clear({ silent: true });
      this.model.set({
        key: key,
        componentUuid: componentUuid
      }, { silent: true });
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
          that.popup.close();
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
        this.popup.close();
      }
      if (fetch) {
        this.resetIssue();
      }
    },

    comment: function (e) {
      e.stopPropagation();
      $('body').click();
      this.popup = new CommentFormView({
        triggerEl: $(e.currentTarget),
        bottom: true,
        issue: this.model,
        detailView: this
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
      view.close();
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
      return $.post(baseUrl + '/api/issues/do_action', {
        issue: this.model.id,
        actionKey: action
      }).done(function () {
        that.resetIssue();
      });
    },

    showRule: function () {
      if (Workspace == null) {
        Workspace = require('components/workspace/main');
      }
      var ruleKey = this.model.get('rule');
      Workspace.openRule({ key: ruleKey });
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

    changeTags: function () {
      var that = this;
      return jQuery.ajax({
        url: baseUrl + '/api/issues/tags?ps=0'
      }).done(function (r) {
        if (that.ui.tagInput.select2) {
          that.ui.tagInput.select2({
            tags: _.difference(r.tags, that.model.get('tags')),
            width: '300px'
          });
        }
        if (that.ui.tagsEdit.show) {
          that.ui.tagsEdit.show();
        }
        if (that.ui.tagsList.hide) {
          that.ui.tagsList.hide();
        }
        that.tagsBuffer = that.ui.tagInput.select2('val');
        var keyScope = key.getScope();
        if (keyScope !== 'tags') {
          that.previousKeyScope = keyScope;
        }
        key.setScope('tags');
        key('escape', 'tags', function () {
          return that.cancelEdit();
        });
        that.$('.select2-input').keyup(function (event) {
          if (event.which === 27) {
            return that.cancelEdit();
          }
        });
        that.ui.tagInput.select2('focus');
      });
    },

    cancelEdit: function () {
      this.resetKeyScope();
      if (this.ui.tagsList.show) {
        this.ui.tagsList.show();
      }
      if (this.ui.tagInput.select2) {
        this.ui.tagInput.select2('val', this.tagsBuffer);
        this.ui.tagInput.select2('close');
      }
      if (this.ui.tagsEdit.hide) {
        return this.ui.tagsEdit.hide();
      }
    },

    editDone: function () {
      var that = this;
      this.resetKeyScope();
      var _tags = this.model.get('tags'),
          tags = this.ui.tagInput.val(),
          splitTags = tags ? tags.split(',') : null;
      this.model.set('tags', splitTags);
      return $.post(baseUrl + '/api/issues/set_tags', {
        key: this.model.get('key'),
        tags: tags
      }).done(function () {
        that.cancelEdit();
      }).fail(function () {
        that.model.set('tags', _tags);
      }).always(function () {
        that.render();
      });
    },

    resetKeyScope: function () {
      key.unbind('escape', 'tags');
      if (this.previousKeyScope) {
        key.setScope(this.previousKeyScope);
        this.previousKeyScope = null;
      }
    },

    serializeData: function () {
      var issueKey = encodeURIComponent(this.model.get('key'));
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        permalink: baseUrl + '/issues/search#issues=' + issueKey
      });
    }
  });

});
