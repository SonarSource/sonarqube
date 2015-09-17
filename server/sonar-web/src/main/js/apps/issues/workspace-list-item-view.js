import $ from 'jquery';
import _ from 'underscore';
import IssueView from 'components/issue/issue-view';
import IssueFilterView from './issue-filter-view';
import './templates';

var SHOULD_NULL = {
      any: ['issues'],
      resolutions: ['resolved'],
      resolved: ['resolutions'],
      assignees: ['assigned'],
      assigned: ['assignees'],
      actionPlans: ['planned'],
      planned: ['actionPlans']
    };

export default IssueView.extend({
  checkboxTemplate: Templates['issues-issue-checkbox'],
  filterTemplate: Templates['issues-issue-filter'],

  events: function () {
    return _.extend(IssueView.prototype.events.apply(this, arguments), {
      'click': 'selectCurrent',
      'dblclick': 'openComponentViewer',
      'click .js-issue-navigate': 'openComponentViewer',
      'click .js-issue-filter': 'onIssueFilterClick',
      'click .js-toggle': 'onIssueToggle'
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
    this.addCheckbox();
    this.$el.addClass('issue-navigate-right');
    if (this.options.app.state.get('canBulkChange')) {
      this.$el.addClass('issue-with-checkbox');
    }
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
      that.popup.destroy();
    });
    this.popup.render();
  },

  onIssueToggle: function (e) {
    e.preventDefault();
    this.model.set({ selected: !this.model.get('selected') });
    var selected = this.model.collection.where({ selected: true }).length;
    this.options.app.state.set({ selected: selected });
  },

  addFilterSelect: function () {
    this.$('.issue-table-meta-cell-first')
        .find('.issue-meta-list')
        .append(this.filterTemplate(this.model.toJSON()));
  },

  addCheckbox: function () {
    this.$el.append(this.checkboxTemplate(this.model.toJSON()));
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
        index = this.model.get('index'),
        selected = this.model.get('selected');
    this.model.reset({
      key: key,
      componentUuid: componentUuid,
      index: index,
      selected: selected
    }, { silent: true });
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


