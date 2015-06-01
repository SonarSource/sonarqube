define([
  'components/issue/issue-view',
  './issue-filter-view',
  './templates'
], function (IssueView, IssueFilterView) {

  var $ = jQuery,
      SHOULD_NULL = {
        any: ['issues'],
        resolutions: ['resolved'],
        resolved: ['resolutions'],
        assignees: ['assigned'],
        assigned: ['assignees'],
        actionPlans: ['planned'],
        planned: ['actionPlans']
      };

  return IssueView.extend({
    filterTemplate: Templates['issues-issue-filter'],

    events: function () {
      return _.extend(IssueView.prototype.events.apply(this, arguments), {
        'click': 'selectCurrent',
        'dblclick': 'openComponentViewer',
        'click .js-issue-navigate': 'openComponentViewer',
        'click .js-issue-filter': 'onIssueFilterClick'
      });
    },

    initialize: function (options) {
      IssueView.prototype.initialize.apply(this, arguments);
      this.listenTo(options.app.state, 'change:selectedIndex', this.select);
    },

    onRender: function () {
      IssueView.prototype.onRender.apply(this, arguments);
      this.select();
      this.addFilterSelect();
      this.$el.addClass('issue-navigate-right');
    },

    onIssueFilterClick: function (e) {
      var that = this;
      e.preventDefault();
      e.stopPropagation();
      $('body').click();
      this.popup = new IssueFilterView({
        triggerEl: $(e.currentTarget),
        bottomRight: true,
        model: this.model
      });
      this.popup.on('select', function (property, value) {
        var obj;
        obj = {};
        obj[property] = '' + value;
        SHOULD_NULL.any.forEach(function (p) {
          obj[p] = null;
        });
        if (SHOULD_NULL[property] != null) {
          SHOULD_NULL[property].forEach(function (p) {
            obj[p] = null;
          });
        }
        that.options.app.state.updateFilter(obj);
        that.popup.close();
      });
      this.popup.render();
    },

    addFilterSelect: function () {
      this.$('.issue-table-meta-cell-first')
          .find('.issue-meta-list')
          .append(this.filterTemplate(this.model.toJSON()));
    },

    select: function () {
      var selected = this.model.get('index') === this.options.app.state.get('selectedIndex');
      this.$el.toggleClass('selected', selected);
    },

    selectCurrent: function () {
      this.options.app.state.set({ selectedIndex: this.model.get('index') });
    },

    resetIssue: function (options) {
      var that = this;
      var key = this.model.get('key'),
          componentUuid = this.model.get('componentUuid'),
          index = this.model.get('index');
      this.model.clear({ silent: true });
      this.model.set({ key: key, componentUuid: componentUuid, index: index }, { silent: true });
      return this.model.fetch(options).done(function () {
        return that.trigger('reset');
      });
    },

    openComponentViewer: function () {
      this.options.app.state.set({ selectedIndex: this.model.get('index') });
      if (this.options.app.state.has('component')) {
        return this.options.app.controller.closeComponentViewer();
      } else {
        return this.options.app.controller.showComponentViewer(this.model);
      }
    },

    serializeData: function () {
      return _.extend(IssueView.prototype.serializeData.apply(this, arguments), {
        showComponent: true
      });
    }
  });

});
