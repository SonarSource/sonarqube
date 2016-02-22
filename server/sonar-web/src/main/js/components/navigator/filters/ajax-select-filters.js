/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import Backbone from 'backbone';
import BaseFilters from './base-filters';
import ChoiceFilters from './choice-filters';
import Template from '../templates/ajax-select-filter.hbs';
import ListTemplate from '../templates/choice-filter-item.hbs';

const PAGE_SIZE = 100;


const Suggestions = Backbone.Collection.extend({
  comparator: 'text',

  initialize () {
    this.more = false;
    this.page = 0;
  },


  parse (r) {
    this.more = r.more;
    return r.results;
  },


  fetch (options) {
    this.data = _.extend({
      p: 1,
      ps: PAGE_SIZE
    }, options.data || {});

    const settings = _.extend({}, options, { data: this.data });
    return Backbone.Collection.prototype.fetch.call(this, settings);
  },


  fetchNextPage (options) {
    if (this.more) {
      this.data.p += 1;
      const settings = _.extend({ remove: false }, options, { data: this.data });
      return this.fetch(settings);
    }
    return false;
  }

});


const UserSuggestions = Suggestions.extend({

  url () {
    return '/api/users/search';
  },

  parse (response) {
    const parsedResponse = window.usersToSelect2(response);
    this.more = parsedResponse.more;
    this.results = parsedResponse.results;
  }

});


const ProjectSuggestions = Suggestions.extend({

  url () {
    return '/api/resources/search?f=s2&q=TRK&display_key=true';
  }

});


const ComponentSuggestions = Suggestions.extend({

  url () {
    return '/api/resources/search?f=s2&qp=supportsGlobalDashboards&display_key=true';
  },

  parse (r) {
    this.more = r.more;

    // If results are divided into categories
    if (r.results.length > 0 && r.results[0].children) {
      const results = [];
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


const AjaxSelectDetailsFilterView = ChoiceFilters.DetailsChoiceFilterView.extend({
  template: Template,
  listTemplate: ListTemplate,
  searchKey: 's',


  render () {
    ChoiceFilters.DetailsChoiceFilterView.prototype.render.apply(this, arguments);

    const that = this;
    const keyup = function (e) {
      if (e.keyCode !== 37 && e.keyCode !== 38 && e.keyCode !== 39 && e.keyCode !== 40) {
        that.search();
      }
    };
    const debouncedKeyup = _.debounce(keyup, 250);
    const scroll = function () {
      that.scroll();
    };
    const throttledScroll = _.throttle(scroll, 1000);

    this.$('.navigator-filter-search input')
        .off('keyup keydown')
        .on('keyup', debouncedKeyup)
        .on('keydown', this.keydown);

    this.$('.choices')
        .off('scroll')
        .on('scroll', throttledScroll);
  },


  search () {
    const that = this;
    this.query = this.$('.navigator-filter-search input').val();
    if (this.query.length > 1) {
      this.$el.addClass('fetching');
      const selected = that.options.filterView.getSelected();
      const data = { ps: PAGE_SIZE };
      data[this.searchKey] = this.query;
      this.options.filterView.choices.fetch({
        data,
        success () {
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
        error () {
          that.showSearchError();
        }
      });
    } else {
      this.resetChoices();
      this.updateLists();
    }
  },


  showSearchError () {
    this.$el.removeClass('fetching');
    this.$('.navigator-filter-search').addClass('fetching-error');
  },


  scroll () {
    const that = this;
    const el = this.$('.choices');
    const scrollBottom = el.scrollTop() >= el[0].scrollHeight - el.outerHeight();

    if (scrollBottom) {
      this.options.filterView.choices.fetchNextPage().done(function () {
        that.updateLists();
      });
    }
  },


  keydown (e) {
    if (_([38, 40, 13]).indexOf(e.keyCode) !== -1) {
      e.preventDefault();
    }
  },


  resetChoices () {
    const that = this;
    this.options.filterView.choices.reset(this.options.filterView.choices.filter(function (item) {
      return item.get('checked');
    }));
    _.each(this.model.get('choices'), function (v, k) {
      that.options.filterView.choices.add(new Backbone.Model({ id: k, text: v }));
    });
  },


  onShow () {
    ChoiceFilters.DetailsChoiceFilterView.prototype.onShow.apply(this, arguments);
    this.resetChoices();
    this.render();
    this.$('.navigator-filter-search input').focus();
  }

});


const AjaxSelectFilterView = ChoiceFilters.ChoiceFilterView.extend({

  initialize (options) {
    ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
      projectsView: (options && options.projectsView) ? options.projectsView : AjaxSelectDetailsFilterView
    });
  },


  isDefaultValue () {
    return this.getSelected().length === 0;
  },


  renderInput () {
    const value = this.model.get('value') || [];
    const input = $('<input>')
        .prop('name', this.model.get('property'))
        .prop('type', 'hidden')
        .css('display', 'none')
        .val(value.join());
    input.appendTo(this.$el);
  },


  restoreFromQuery (q) {
    let param = _.findWhere(q, { key: this.model.get('property') });

    if (this.model.get('choices')) {
      _.each(this.model.get('choices'), function (v, k) {
        if (k[0] === '!') {
          const x = _.findWhere(q, { key: k.substr(1) });
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


  restore (value, param) {
    const that = this;
    if (_.isString(value)) {
      value = value.split(',');
    }

    if (this.choices && value.length > 0) {
      this.model.set({ value, enabled: true });

      const opposite = _.filter(value, function (item) {
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


  restoreFromText (value, text) {
    const that = this;
    _.each(value, function (v, i) {
      that.choices.add(new Backbone.Model({
        id: v,
        text: text[i],
        checked: true
      }));
    });
    this.onRestore(value);
  },


  restoreByRequests (value) {
    const that = this;
    const requests = _.map(value, function (v) {
      return that.createRequest(v);
    });

    $.when.apply($, requests).done(function () {
      that.onRestore(value);
    });
  },


  onRestore () {
    this.projectsView.updateLists();
    this.renderBase();
  },


  clear () {
    this.model.unset('value');
    if (this.choices) {
      this.choices.reset([]);
    }
    this.render();
  },


  createRequest () {
  }

});


const ComponentFilterView = AjaxSelectFilterView.extend({

  initialize () {
    AjaxSelectFilterView.prototype.initialize.call(this, {
      projectsView: AjaxSelectDetailsFilterView
    });
    this.choices = new ComponentSuggestions();
  },


  createRequest (v) {
    const that = this;
    return $
        .ajax({
          url: '/api/resources',
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


const ProjectFilterView = AjaxSelectFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      projectsView: AjaxSelectDetailsFilterView
    });

    this.choices = new ProjectSuggestions();
  },


  createRequest (v) {
    const that = this;
    return $
        .ajax({
          url: '/api/resources',
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


const AssigneeFilterView = AjaxSelectFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      projectsView: AjaxSelectDetailsFilterView
    });

    this.choices = new UserSuggestions();
  },

  createRequest (v) {
    const that = this;
    return $
        .ajax({
          url: '/api/users/search',
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


const ReporterFilterView = AjaxSelectFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      projectsView: AjaxSelectDetailsFilterView
    });

    this.selection = new UserSuggestions();
    this.choices = new UserSuggestions();
  },


  createRequest (v) {
    const that = this;
    return $
        .ajax({
          url: '/api/users/search',
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
  Suggestions,
  AjaxSelectDetailsFilterView,
  AjaxSelectFilterView,
  ProjectFilterView,
  ComponentFilterView,
  AssigneeFilterView,
  ReporterFilterView
};


