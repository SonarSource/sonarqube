import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-creation-date-facet'],

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'change input': 'applyFacet',
      'click .js-select-period-start': 'selectPeriodStart',
      'click .js-select-period-end': 'selectPeriodEnd',
      'click .sonar-d3 rect': 'selectBar',
      'click .js-all': 'onAllClick',
      'click .js-last-week': 'onLastWeekClick',
      'click .js-last-month': 'onLastMonthClick',
      'click .js-last-year': 'onLastYearClick'
    });
  },

  onRender: function () {
    var that = this;
    this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
    this.$('input').datepicker({
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true
    });
    var props = ['createdAfter', 'createdBefore', 'createdAt'],
        query = this.options.app.state.get('query');
    props.forEach(function (prop) {
      var value = query[prop];
      if (value != null) {
        return that.$('input[name=' + prop + ']').val(value);
      }
    });
    var values = this.model.getValues();
    if (!(_.isArray(values) && values.length > 0)) {
      var date = moment();
      values = [];
      _.times(10, function () {
        values.push({ count: 0, val: date.toDate().toString() });
        date = date.subtract(1, 'days');
      });
      values.reverse();
    }
    values = values.map(function (v) {
      var format = that.options.app.state.getFacetMode() === 'count' ? 'SHORT_INT' : 'SHORT_WORK_DUR';
      var text = window.formatMeasure(v.count, format);
      return _.extend(v, { text: text });
    });
    return this.$('.js-barchart').barchart(values);
  },

  selectPeriodStart: function () {
    return this.$('.js-period-start').datepicker('show');
  },

  selectPeriodEnd: function () {
    return this.$('.js-period-end').datepicker('show');
  },

  applyFacet: function () {
    var obj = { createdAt: null, createdInLast: null };
    this.$('input').each(function () {
      var property, value;
      property = $(this).prop('name');
      value = $(this).val();
      obj[property] = value;
    });
    return this.options.app.state.updateFilter(obj);
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      createdAfter: null,
      createdBefore: null,
      createdAt: null,
      createdInLast: null
    });
  },

  selectBar: function (e) {
    var periodStart = $(e.currentTarget).data('period-start'),
        periodEnd = $(e.currentTarget).data('period-end');
    return this.options.app.state.updateFilter({
      createdAfter: periodStart,
      createdBefore: periodEnd,
      createdAt: null,
      createdInLast: null
    });
  },

  selectPeriod: function (period) {
    return this.options.app.state.updateFilter({
      createdAfter: null,
      createdBefore: null,
      createdAt: null,
      createdInLast: period
    });
  },

  onAllClick: function () {
    return this.disable();
  },

  onLastWeekClick: function (e) {
    e.preventDefault();
    return this.selectPeriod('1w');
  },

  onLastMonthClick: function (e) {
    e.preventDefault();
    return this.selectPeriod('1m');
  },

  onLastYearClick: function (e) {
    e.preventDefault();
    return this.selectPeriod('1y');
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      periodStart: this.options.app.state.get('query').createdAfter,
      periodEnd: this.options.app.state.get('query').createdBefore,
      createdAt: this.options.app.state.get('query').createdAt,
      createdInLast: this.options.app.state.get('query').createdInLast
    });
  }
});


