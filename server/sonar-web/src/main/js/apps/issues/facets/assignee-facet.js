import $ from 'jquery';
import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';
import '../templates';

export default CustomValuesFacet.extend({
  template: Templates['issues-assignee-facet'],

  getUrl: function () {
    return baseUrl + '/api/users/search';
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term, page) {
        return { q: term, p: page };
      },
      results: window.usersToSelect2
    };
  },

  onRender: function () {
    CustomValuesFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').assigned;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unassigned]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unassigned = $(e.currentTarget).is('[data-unassigned]');
    $(e.currentTarget).toggleClass('active');
    if (unassigned) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        assigned: value,
        assignees: null
      });
    } else {
      return this.options.app.state.updateFilter({
        assigned: null,
        assignees: this.getValue()
      });
    }
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        users = this.options.app.facets.users;
    values.forEach(function (v) {
      var login = v.val,
          name = '';
      if (login) {
        var user = _.findWhere(users, { login: login });
        if (user != null) {
          name = user.name;
        }
      }
      v.label = name;
    });
    return values;
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      assigned: null,
      assignees: null
    });
  },

  addCustomValue: function () {
    var property = this.model.get('property'),
        customValue = this.$('.js-custom-value').select2('val'),
        value = this.getValue();
    if (value.length > 0) {
      value += ',';
    }
    value += customValue;
    var obj = {};
    obj[property] = value;
    obj.assigned = null;
    return this.options.app.state.updateFilter(obj);
  },

  sortValues: function (values) {
    return _.sortBy(values, function (v) {
      return v.val === '' ? -999999 : -v.count;
    });
  },

  getNumberOfMyIssues: function () {
    return this.options.app.state.get('myIssues');
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      myIssues: this.getNumberOfMyIssues(),
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


