import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import BaseFilters from './base-filters';
import ChoiceFilters from './choice-filters';
import Template from '../templates/ajax-select-filter.hbs';
import ListTemplate from '../templates/choice-filter-item.hbs';

var PAGE_SIZE = 100;


var Suggestions = Backbone.Collection.extend({
  comparator: 'text',

  initialize: function () {
    this.more = false;
    this.page = 0;
  },


  parse: function (r) {
    this.more = r.more;
    return r.results;
  },


  fetch: function (options) {
    this.data = _.extend({
      p: 1,
      ps: PAGE_SIZE
    }, options.data || {});

    var settings = _.extend({}, options, { data: this.data });
    return Backbone.Collection.prototype.fetch.call(this, settings);
  },


  fetchNextPage: function (options) {
    if (this.more) {
      this.data.p += 1;
      var settings = _.extend({ remove: false }, options, { data: this.data });
      return this.fetch(settings);
    }
    return false;
  }

});


var UserSuggestions = Suggestions.extend({

  url: function () {
    return baseUrl + '/api/users/search';
  },

  parse: function (response) {
    var parsedResponse = window.usersToSelect2(response);
    this.more = parsedResponse.more;
    this.results = parsedResponse.results;
  }

});


var ProjectSuggestions = Suggestions.extend({

  url: function () {
    return baseUrl + '/api/resources/search?f=s2&q=TRK&display_key=true';
  }

});


var ComponentSuggestions = Suggestions.extend({

  url: function () {
    return baseUrl + '/api/resources/search?f=s2&qp=supportsGlobalDashboards&display_key=true';
  },

  parse: function (r) {
    this.more = r.more;

    // If results are divided into categories
    if (r.results.length > 0 && r.results[0].children) {
      var results = [];
      _.each(r.results, function (category) {
        _.each(category.children, function (child) {
          child.category = category.text;
          results.push(child);
        });
      });
      return results;
    } else {
      return r.results;
    }
  }

});


var AjaxSelectDetailsFilterView = ChoiceFilters.DetailsChoiceFilterView.extend({
  template: Template,
  listTemplate: ListTemplate,
  searchKey: 's',


  render: function () {
    ChoiceFilters.DetailsChoiceFilterView.prototype.render.apply(this, arguments);

    var that = this,
        keyup = function (e) {
          if (e.keyCode !== 37 && e.keyCode !== 38 && e.keyCode !== 39 && e.keyCode !== 40) {
            that.search();
          }
        },
        debouncedKeyup = _.debounce(keyup, 250),
        scroll = function () {
          that.scroll();
        },
        throttledScroll = _.throttle(scroll, 1000);

    this.$('.navigator-filter-search input')
        .off('keyup keydown')
        .on('keyup', debouncedKeyup)
        .on('keydown', this.keydown);

    this.$('.choices')
        .off('scroll')
        .on('scroll', throttledScroll);
  },


  search: function () {
    var that = this;
    this.query = this.$('.navigator-filter-search input').val();
    if (this.query.length > 1) {
      this.$el.addClass('fetching');
      var selected = that.options.filterView.getSelected(),
          data = { ps: PAGE_SIZE };
      data[this.searchKey] = this.query;
      this.options.filterView.choices.fetch({
        data: data,
        success: function () {
          selected.forEach(function (item) {
            that.options.filterView.choices.unshift(item);
          });
          _.each(that.model.get('choices'), function (v, k) {
            if (k[0] === '!') {
              that.options.filterView.choices.add(new Backbone.Model({ id: k, text: v }));
            }
          });
          that.updateLists();
          that.$el.removeClass('fetching');
          that.$('.navigator-filter-search').removeClass('fetching-error');
        },
        error: function () {
          that.showSearchError();
        }
      });
    } else {
      this.resetChoices();
      this.updateLists();
    }
  },


  showSearchError: function () {
    this.$el.removeClass('fetching');
    this.$('.navigator-filter-search').addClass('fetching-error');
  },


  scroll: function () {
    var that = this,
        el = this.$('.choices'),
        scrollBottom = el.scrollTop() >= el[0].scrollHeight - el.outerHeight();

    if (scrollBottom) {
      this.options.filterView.choices.fetchNextPage().done(function () {
        that.updateLists();
      });
    }
  },


  keydown: function (e) {
    if (_([38, 40, 13]).indexOf(e.keyCode) !== -1) {
      e.preventDefault();
    }
  },


  resetChoices: function () {
    var that = this;
    this.options.filterView.choices.reset(this.options.filterView.choices.filter(function (item) {
      return item.get('checked');
    }));
    _.each(this.model.get('choices'), function (v, k) {
      that.options.filterView.choices.add(new Backbone.Model({ id: k, text: v }));
    });
  },


  onShow: function () {
    ChoiceFilters.DetailsChoiceFilterView.prototype.onShow.apply(this, arguments);
    this.resetChoices();
    this.render();
    this.$('.navigator-filter-search input').focus();
  }

});


var AjaxSelectFilterView = ChoiceFilters.ChoiceFilterView.extend({

  initialize: function (options) {
    ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
      detailsView: (options && options.detailsView) ? options.detailsView : AjaxSelectDetailsFilterView
    });
  },


  isDefaultValue: function () {
    return this.getSelected().length === 0;
  },


  renderInput: function () {
    var value = this.model.get('value') || [],
        input = $('<input>')
            .prop('name', this.model.get('property'))
            .prop('type', 'hidden')
            .css('display', 'none')
            .val(value.join());
    input.appendTo(this.$el);
  },


  restoreFromQuery: function (q) {
    var param = _.findWhere(q, { key: this.model.get('property') });

    if (this.model.get('choices')) {
      _.each(this.model.get('choices'), function (v, k) {
        if (k[0] === '!') {
          var x = _.findWhere(q, { key: k.substr(1) });
          if (x == null) {
            return;
          }
          if (!param) {
            param = { value: k };
          } else {
            param.value += ',' + k;
          }
        }
      });
    }

    if (param && param.value) {
      this.model.set('enabled', true);
      this.restore(param.value, param);
    } else {
      this.clear();
    }
  },


  restore: function (value, param) {
    var that = this;
    if (_.isString(value)) {
      value = value.split(',');
    }

    if (this.choices && value.length > 0) {
      this.model.set({ value: value, enabled: true });

      var opposite = _.filter(value, function (item) {
        return item[0] === '!';
      });
      opposite.forEach(function (item) {
        that.choices.add(new Backbone.Model({
          id: item,
          text: that.model.get('choices')[item],
          checked: true
        }));
      });

      value = _.reject(value, function (item) {
        return item[0] === '!';
      });
      if (_.isArray(param.text) && param.text.length === value.length) {
        this.restoreFromText(value, param.text);
      } else {
        this.restoreByRequests(value);
      }
    } else {
      this.clear();
    }
  },


  restoreFromText: function (value, text) {
    var that = this;
    _.each(value, function (v, i) {
      that.choices.add(new Backbone.Model({
        id: v,
        text: text[i],
        checked: true
      }));
    });
    this.onRestore(value);
  },


  restoreByRequests: function (value) {
    var that = this,
        requests = _.map(value, function (v) {
          return that.createRequest(v);
        });

    $.when.apply($, requests).done(function () {
      that.onRestore(value);
    });
  },


  onRestore: function () {
    this.detailsView.updateLists();
    this.renderBase();
  },


  clear: function () {
    this.model.unset('value');
    if (this.choices) {
      this.choices.reset([]);
    }
    this.render();
  },


  createRequest: function () {
  }

});


var ComponentFilterView = AjaxSelectFilterView.extend({

  initialize: function () {
    AjaxSelectFilterView.prototype.initialize.call(this, {
      detailsView: AjaxSelectDetailsFilterView
    });
    this.choices = new ComponentSuggestions();
  },


  createRequest: function (v) {
    var that = this;
    return $
        .ajax({
          url: baseUrl + '/api/resources',
          type: 'GET',
          data: { resource: v }
        })
        .done(function (r) {
          that.selection.add(new Backbone.Model({
            id: r[0].key,
            text: r[0].name
          }));
        });
  }

});


var ProjectFilterView = AjaxSelectFilterView.extend({

  initialize: function () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: AjaxSelectDetailsFilterView
    });

    this.choices = new ProjectSuggestions();
  },


  createRequest: function (v) {
    var that = this;
    return $
        .ajax({
          url: baseUrl + '/api/resources',
          type: 'GET',
          data: { resource: v }
        })
        .done(function (r) {
          that.choices.add(new Backbone.Model({
            id: r[0].key,
            text: r[0].name,
            checked: true
          }));
        });
  }

});


var AssigneeFilterView = AjaxSelectFilterView.extend({

  initialize: function () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: AjaxSelectDetailsFilterView
    });

    this.choices = new UserSuggestions();
  },

  createRequest: function (v) {
    var that = this;
    return $
        .ajax({
          url: baseUrl + '/api/users/search',
          type: 'GET',
          data: { q: v }
        })
        .done(function (r) {
          that.choices.add(new Backbone.Model({
            id: r.users[0].login,
            text: r.users[0].name + ' (' + r.users[0].login + ')',
            checked: true
          }));
        });
  }

});


var ReporterFilterView = AjaxSelectFilterView.extend({

  initialize: function () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: AjaxSelectDetailsFilterView
    });

    this.selection = new UserSuggestions();
    this.choices = new UserSuggestions();
  },


  createRequest: function (v) {
    var that = this;
    return $
        .ajax({
          url: baseUrl + '/api/users/search',
          type: 'GET',
          data: { q: v }
        })
        .done(function (r) {
          that.choices.add(new Backbone.Model({
            id: r.users[0].login,
            text: r.users[0].name + ' (' + r.users[0].login + ')',
            checked: true
          }));
        });
  }

});


/*
 * Export public classes
 */

export default {
  Suggestions: Suggestions,
  AjaxSelectDetailsFilterView: AjaxSelectDetailsFilterView,
  AjaxSelectFilterView: AjaxSelectFilterView,
  ProjectFilterView: ProjectFilterView,
  ComponentFilterView: ComponentFilterView,
  AssigneeFilterView: AssigneeFilterView,
  ReporterFilterView: ReporterFilterView
};


