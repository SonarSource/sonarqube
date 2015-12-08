import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';

var showError = null;

/*
 * SelectList Collection
 */

var SelectListCollection = Backbone.Collection.extend({

  initialize: function (options) {
    this.options = options;
  },

  parse: function (r) {
    return this.options.parse.call(this, r);
  },

  fetch: function (options) {
    var data = $.extend({
          page: 1,
          pageSize: 100
        }, options.data || {}),
        settings = $.extend({}, options, { data: data });

    this.settings = {
      url: settings.url,
      data: data
    };

    Backbone.Collection.prototype.fetch.call(this, settings);
  },

  fetchNextPage: function (options) {
    if (this.more) {
      var nextPage = this.settings.data.page + 1,
          settings = $.extend(this.settings, options);

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

var SelectListItemView = Backbone.View.extend({
  tagName: 'li',

  template: function (d) {
    return '<input class="select-list-list-checkbox" type="checkbox">' +
        '<div class="select-list-list-item">' + d + '</div>';
  },

  events: {
    'change .select-list-list-checkbox': 'toggle'
  },

  initialize: function (options) {
    this.listenTo(this.model, 'change', this.render);
    this.settings = options.settings;
  },

  render: function () {
    this.$el.html(this.template(this.settings.format(this.model.toJSON())));
    this.$('input').prop('name', this.model.get('name'));
    this.$el.toggleClass('selected', this.model.get('selected'));
    this.$('.select-list-list-checkbox')
        .prop('title',
        this.model.get('selected') ?
            this.settings.tooltips.deselect :
            this.settings.tooltips.select)
        .prop('checked', this.model.get('selected'));

    if (this.settings.readOnly) {
      this.$('.select-list-list-checkbox').prop('disabled', true);
    }
  },

  remove: function (postpone) {
    if (postpone) {
      var that = this;
      that.$el.addClass(this.model.get('selected') ? 'added' : 'removed');
      setTimeout(function () {
        Backbone.View.prototype.remove.call(that, arguments);
      }, 500);
    } else {
      Backbone.View.prototype.remove.call(this, arguments);
    }
  },

  toggle: function () {
    var selected = this.model.get('selected'),
        that = this,
        url = selected ? this.settings.deselectUrl : this.settings.selectUrl,
        data = $.extend({}, this.settings.extra || {});

    data[this.settings.selectParameter] = this.model.get(this.settings.selectParameterValue);

    that.$el.addClass('progress');
    $.ajax({
      url: url,
      type: 'POST',
      data: data,
      statusCode: {
        // do not show global error
        400: null,
        401: null,
        403: null,
        500: null
      }
    })
        .done(function () {
          that.model.set('selected', !selected);
        })
        .fail(function (jqXHR) {
          that.render();
          showError(jqXHR);
        })
        .always(function () {
          that.$el.removeClass('progress');
        });
  }
});


/*
 * SelectList View
 */

var SelectListView = Backbone.View.extend({
  template: function (l) {
    /* eslint max-len: 0 */
    return '<div class="select-list-container">' +
        '<div class="select-list-control">' +
        '<div class="select-list-check-control">' +
        '<a class="select-list-control-button" name="selected">' + l.selected + '</a>' +
        '<a class="select-list-control-button" name="deselected">' + l.deselected + '</a>' +
        '<a class="select-list-control-button" name="all">' + l.all + '</a>' +
        '</div>' +
        '<div class="select-list-search-control">' +
        '<form class="search-box">' +
        '<span class="search-box-submit button-clean"><i class="icon-search"></i></span>' +
        '<input class="search-box-input" type="search" name="q" placeholder="Search" maxlength="100" autocomplete="off">' +
        '</form>' +
        '</div>' +
        '</div>' +
        '<div class="select-list-list-container">' +
        '<ul class="select-list-list"></ul>' +
        '</div>' +
        '</div>';
  },

  events: {
    'click .select-list-control-button[name=selected]': 'showSelected',
    'click .select-list-control-button[name=deselected]': 'showDeselected',
    'click .select-list-control-button[name=all]': 'showAll'
  },

  initialize: function (options) {
    this.listenTo(this.collection, 'add', this.renderListItem);
    this.listenTo(this.collection, 'reset', this.renderList);
    this.listenTo(this.collection, 'remove', this.removeModel);
    this.listenTo(this.collection, 'change:selected', this.confirmFilter);
    this.settings = options.settings;

    var that = this;
    this.showFetchSpinner = function () {
      that.$listContainer.addClass('loading');
    };
    this.hideFetchSpinner = function () {
      that.$listContainer.removeClass('loading');
    };

    var onScroll = function () {
      that.showFetchSpinner();

      that.collection.fetchNextPage({
        success: function () {
          that.hideFetchSpinner();
        },
        error: function () {
          that.hideFetchSpinner();
        }
      });
    };
    this.onScroll = _.throttle(onScroll, 1000);
  },

  render: function () {
    var that = this,
        keyup = function () {
          that.search();
        };

    this.$el.html(this.template(this.settings.labels))
        .width(this.settings.width);

    this.$listContainer = this.$('.select-list-list-container');
    if (!this.settings.readOnly) {
      this.$listContainer
          .height(this.settings.height)
          .css('overflow', 'auto')
          .on('scroll', function () {
            that.scroll();
          });
    } else {
      this.$listContainer.addClass('select-list-list-container-readonly');
    }

    this.$list = this.$('.select-list-list');

    var searchInput = this.$('.select-list-search-control input')
        .on('keyup', _.debounce(keyup, 250))
        .on('search', _.debounce(keyup, 250));

    if (this.settings.focusSearch) {
      setTimeout(function () {
        searchInput.focus();
      }, 250);
    }

    this.listItemViews = [];

    showError = function (jqXHR) {
      var message = window.t('default_error_message');
      if (jqXHR != null && jqXHR.responseJSON != null && jqXHR.responseJSON.errors != null) {
        message = _.pluck(jqXHR.responseJSON.errors, 'msg').join('. ');
      }

      that.$el.prevAll('.alert').remove();
      $('<div>')
          .addClass('alert alert-danger').text(message)
          .insertBefore(that.$el);
    };

    if (this.settings.readOnly) {
      this.$('.select-list-control').remove();
    }
  },

  renderList: function () {
    this.listItemViews.forEach(function (view) {
      view.remove();
    });
    this.listItemViews = [];
    if (this.collection.length > 0) {
      this.collection.each(this.renderListItem, this);
    } else {
      if (this.settings.readOnly) {
        this.renderEmpty();
      }
    }
    this.$listContainer.scrollTop(0);
  },

  renderListItem: function (item) {
    var itemView = new SelectListItemView({
      model: item,
      settings: this.settings
    });
    this.listItemViews.push(itemView);
    this.$list.append(itemView.el);
    itemView.render();
  },

  renderEmpty: function () {
    this.$list.append('<li class="empty-message">' + this.settings.labels.noResults + '</li>');
  },

  confirmFilter: function (model) {
    if (this.currentFilter !== 'all') {
      this.collection.remove(model);
    }
  },

  removeModel: function (model, collection, options) {
    this.listItemViews[options.index].remove(true);
    this.listItemViews.splice(options.index, 1);
  },

  filterBySelection: function (filter) {
    var that = this;
    filter = this.currentFilter = filter || this.currentFilter;

    if (filter != null) {
      this.$('.select-list-check-control').toggleClass('disabled', false);
      this.$('.select-list-search-control').toggleClass('disabled', true);
      this.$('.select-list-search-control input').val('');

      this.$('.select-list-control-button').removeClass('active')
          .filter('[name=' + filter + ']').addClass('active');

      this.showFetchSpinner();

      this.collection.fetch({
        url: this.settings.searchUrl,
        reset: true,
        data: { selected: filter },
        success: function () {
          that.hideFetchSpinner();
        },
        error: showError
      });
    }
  },

  showSelected: function () {
    this.filterBySelection('selected');
  },

  showDeselected: function () {
    this.filterBySelection('deselected');
  },

  showAll: function () {
    this.filterBySelection('all');
  },

  search: function () {
    var query = this.$('.select-list-search-control input').val(),
        hasQuery = query.length > 0,
        that = this,
        data = {};

    this.$('.select-list-check-control').toggleClass('disabled', hasQuery);
    this.$('.select-list-search-control').toggleClass('disabled', !hasQuery);

    if (hasQuery) {
      this.showFetchSpinner();
      this.currentFilter = 'all';

      data[this.settings.queryParam] = query;
      data.selected = 'all';
      this.collection.fetch({
        url: this.settings.searchUrl,
        reset: true,
        data: data,
        success: function () {
          that.hideFetchSpinner();
        },
        error: showError
      });
    } else {
      this.filterBySelection();
    }
  },

  searchByQuery: function (query) {
    this.$('.select-list-search-control input').val(query);
    this.search();
  },

  clearSearch: function () {
    this.filterBySelection();
  },

  scroll: function () {
    var scrollBottom = this.$listContainer.scrollTop() >=
        this.$list[0].scrollHeight - this.$listContainer.outerHeight();

    if (scrollBottom && this.collection.more) {
      this.onScroll();
    }
  }

});


/*
 * SelectList Entry Point
 */

window.SelectList = function (options) {
  this.settings = $.extend(window.SelectList.defaults, options);

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

window.SelectList.prototype.filter = function (filter) {
  this.view.filterBySelection(filter);
  return this;
};

window.SelectList.prototype.search = function (query) {
  this.view.searchByQuery(query);
  return this;
};


/*
 * SelectList Defaults
 */

window.SelectList.defaults = {
  width: '50%',
  height: 400,

  readOnly: false,
  focusSearch: true,

  format: function (item) {
    return item.value;
  },

  parse: function (r) {
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
