/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { debounce, throttle } from 'lodash';
import escapeHtml from 'escape-html';
import ItemTemplate from './templates/item.hbs';
import ListTemplate from './templates/list.hbs';
import { translate } from '../../helpers/l10n';
import './styles.css';
import '../controls/SearchBox.css';

let showError = null;

/*
 * SelectList Collection
 */

const SelectListCollection = Backbone.Collection.extend({
  initialize(options) {
    this.options = options;
  },

  parse(r) {
    return this.options.parse.call(this, r);
  },

  fetch(options) {
    const data = $.extend(
      {
        page: 1,
        pageSize: 100
      },
      options.data || {}
    );
    const settings = $.extend({}, options, { data });

    this.settings = {
      url: settings.url,
      data
    };

    Backbone.Collection.prototype.fetch.call(this, settings);
  },

  fetchNextPage(options) {
    if (this.more) {
      const nextPage = this.settings.data.page + 1;
      const settings = $.extend(this.settings, options);

      settings.data.page = nextPage;
      settings.remove = false;
      this.fetch(settings);
    } else {
      options.error();
    }
  }
});

/*
 * SelectList Item View
 */

const SelectListItemView = Backbone.View.extend({
  tagName: 'li',
  template: ItemTemplate,

  events: {
    'change .select-list-list-checkbox': 'toggle'
  },

  initialize(options) {
    this.listenTo(this.model, 'change', this.render);
    this.settings = options.settings;
  },

  render() {
    this.$el.html(this.template(this.settings.dangerouslyUnescapedHtmlFormat(this.model.toJSON())));
    this.$('input').prop('name', this.model.get('name'));
    this.$el.toggleClass('selected', this.model.get('selected'));
    this.$('.select-list-list-checkbox')
      .prop(
        'title',
        this.model.get('selected') ? this.settings.tooltips.deselect : this.settings.tooltips.select
      )
      .prop('checked', this.model.get('selected'));

    if (this.settings.readOnly) {
      this.$('.select-list-list-checkbox').prop('disabled', true);
    }
  },

  remove(postpone) {
    if (postpone) {
      this.$el.addClass(this.model.get('selected') ? 'added' : 'removed');
      setTimeout(() => {
        Backbone.View.prototype.remove.call(this, arguments);
      }, 500);
    } else {
      Backbone.View.prototype.remove.call(this, arguments);
    }
  },

  toggle() {
    const selected = this.model.get('selected');
    const that = this;
    const url = selected ? this.settings.deselectUrl : this.settings.selectUrl;
    const data = $.extend({}, this.settings.extra || {});

    data[this.settings.selectParameter] = this.model.get(this.settings.selectParameterValue);

    that.$el.addClass('progress');
    $.ajax({
      url,
      data,
      type: 'POST',
      statusCode: {
        // do not show global error
        400: null,
        403: null,
        500: null
      }
    })
      .done(() => {
        that.model.set('selected', !selected);
      })
      .fail(jqXHR => {
        that.render();
        showError(jqXHR);
      })
      .always(() => {
        that.$el.removeClass('progress');
      });
  }
});

/*
 * SelectList View
 */

const SelectListView = Backbone.View.extend({
  template: ListTemplate,

  events: {
    'click .select-list-control-button[name=selected]': 'showSelected',
    'click .select-list-control-button[name=deselected]': 'showDeselected',
    'click .select-list-control-button[name=all]': 'showAll',
    'click .js-reset': 'onResetClick'
  },

  initialize(options) {
    this.listenTo(this.collection, 'add', this.renderListItem);
    this.listenTo(this.collection, 'reset', this.renderList);
    this.listenTo(this.collection, 'remove', this.removeModel);
    this.listenTo(this.collection, 'change:selected', this.confirmFilter);
    this.settings = options.settings;

    const that = this;
    this.showFetchSpinner = function() {
      that.$listContainer.addClass('loading');
    };
    this.hideFetchSpinner = function() {
      that.$listContainer.removeClass('loading');
    };

    const onScroll = function() {
      that.showFetchSpinner();

      that.collection.fetchNextPage({
        success() {
          that.hideFetchSpinner();
        },
        error() {
          that.hideFetchSpinner();
        }
      });
    };
    this.onScroll = throttle(onScroll, 1000);
  },

  render() {
    const that = this;
    const keyup = function() {
      that.search();
    };

    this.$el.html(this.template(this.settings.labels)).width(this.settings.width);

    this.$listContainer = this.$('.select-list-list-container');
    if (!this.settings.readOnly) {
      this.$listContainer
        .height(this.settings.height)
        .css('overflow', 'auto')
        .on('scroll', () => {
          that.scroll();
        });
    } else {
      this.$listContainer.addClass('select-list-list-container-readonly');
    }

    this.$list = this.$('.select-list-list');

    const searchInput = this.$('.select-list-search-control input')
      .on('keyup', debounce(keyup, 250))
      .on('search', debounce(keyup, 250));

    if (this.settings.focusSearch) {
      setTimeout(() => {
        searchInput.focus();
      }, 250);
    }

    this.listItemViews = [];

    showError = function(jqXHR) {
      let message = translate('default_error_message');
      if (jqXHR != null && jqXHR.responseJSON != null && jqXHR.responseJSON.errors != null) {
        message = jqXHR.responseJSON.errors.map(e => e.msg).join('. ');
      }

      that.$el.prevAll('.alert').remove();
      $('<div>')
        .addClass('alert alert-danger')
        .text(message)
        .insertBefore(that.$el);
    };

    if (this.settings.readOnly) {
      this.$('.select-list-control').remove();
    }
  },

  renderList() {
    this.listItemViews.forEach(view => {
      view.remove();
    });
    this.listItemViews = [];
    if (this.collection.length > 0) {
      this.collection.each(this.renderListItem, this);
    } else if (this.settings.readOnly) {
      this.renderEmpty();
    }
    this.$listContainer.scrollTop(0);
  },

  renderListItem(item) {
    const itemView = new SelectListItemView({
      model: item,
      settings: this.settings
    });
    this.listItemViews.push(itemView);
    this.$list.append(itemView.el);
    itemView.render();
  },

  renderEmpty() {
    this.$list.append(`<li class="empty-message">${this.settings.labels.noResults}</li>`);
  },

  confirmFilter(model) {
    if (this.currentFilter !== 'all') {
      this.collection.remove(model);
    }
  },

  removeModel(model, collection, options) {
    this.listItemViews[options.index].remove(true);
    this.listItemViews.splice(options.index, 1);
  },

  filterBySelection(filter) {
    const that = this;
    filter = this.currentFilter = filter || this.currentFilter;

    if (filter != null) {
      this.$('.select-list-check-control').toggleClass('disabled', false);
      this.$('.select-list-search-control').toggleClass('disabled', true);
      this.$('.select-list-search-control input').val('');

      this.$('.select-list-control-button')
        .removeClass('active')
        .filter(`[name=${filter}]`)
        .addClass('active');

      this.showFetchSpinner();

      this.collection.fetch({
        url: this.settings.searchUrl,
        reset: true,
        data: { selected: filter },
        success() {
          that.hideFetchSpinner();
        },
        error: showError
      });
    }
  },

  showSelected() {
    this.filterBySelection('selected');
  },

  showDeselected() {
    this.filterBySelection('deselected');
  },

  showAll() {
    this.filterBySelection('all');
  },

  search() {
    const query = this.$('.select-list-search-control input').val();
    const hasQuery = query.length > 0;
    const that = this;
    const data = {};

    this.$('.select-list-check-control').toggleClass('disabled', hasQuery);
    this.$('.select-list-search-control').toggleClass('disabled', !hasQuery);
    this.$('.js-reset').toggleClass('hidden', !hasQuery);

    if (hasQuery) {
      this.showFetchSpinner();
      this.currentFilter = 'all';

      data[this.settings.queryParam] = query;
      data.selected = 'all';
      this.collection.fetch({
        data,
        url: this.settings.searchUrl,
        reset: true,
        success() {
          that.hideFetchSpinner();
        },
        error: showError
      });
    } else {
      this.filterBySelection();
    }
  },

  onResetClick(e) {
    e.preventDefault();
    e.currentTarget.blur();
    this.$('.select-list-search-control input')
      .val('')
      .focus()
      .trigger('search');
  },

  searchByQuery(query) {
    this.$('.select-list-search-control input').val(query);
    this.search();
  },

  clearSearch() {
    this.filterBySelection();
  },

  scroll() {
    const scrollBottom =
      this.$listContainer.scrollTop() >=
      this.$list[0].scrollHeight - this.$listContainer.outerHeight();

    if (scrollBottom && this.collection.more) {
      this.onScroll();
    }
  }
});

/*
 * SelectList Entry Point
 */

const SelectList = function(options) {
  this.settings = $.extend(this.defaults, options);

  this.collection = new SelectListCollection({
    parse: this.settings.parse
  });

  this.view = new SelectListView({
    el: this.settings.el,
    collection: this.collection,
    settings: this.settings
  });

  this.view.render();
  this.filter('selected');
  return this;
};

/*
 * SelectList API Methods
 */

SelectList.prototype.filter = function(filter) {
  this.view.filterBySelection(filter);
  return this;
};

SelectList.prototype.search = function(query) {
  this.view.searchByQuery(query);
  return this;
};

/*
 * SelectList Defaults
 */

SelectList.prototype.defaults = {
  width: '50%',
  height: 400,

  readOnly: false,
  focusSearch: true,

  dangerouslyUnescapedHtmlFormat(item) {
    return escapeHtml(item.value);
  },

  parse(r) {
    this.more = r.more;
    return r.results;
  },

  queryParam: 'query',

  labels: {
    selected: 'Selected',
    deselected: 'Deselected',
    all: 'All',
    noResults: ''
  },

  tooltips: {
    select: 'Click this to select item',
    deselect: 'Click this to deselect item'
  },

  errorMessage: 'Something gone wrong, try to reload the page and try again.'
};

export default SelectList;
