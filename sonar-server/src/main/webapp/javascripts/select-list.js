/* global Backbone:false, jQuery:false */
/* jshint eqnull:true */

(function ($) {

  var showError = null;

  /*
   * SelectList Collection
   */

  var SelectListCollection = Backbone.Collection.extend({

    parse: function(r) {
      this.more = r.more;
      return r.results;
    },

    fetch: function(options) {
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

    fetchNextPage: function(options) {
      if (this.more) {
        var nextPage = this.settings.data.page + 1,
            settings = $.extend(this.settings, options);

        settings.data.page = nextPage;

        this.fetch(settings);
      }
    }

  });



  /*
   * SelectList Item View
   */

  var SelectListItemView = Backbone.View.extend({
    tagName: 'li',

    template: function(d) {
      return  '<input class="select-list-list-checkbox" type="checkbox">' +
          '<div class="select-list-list-item">' + d + '</div>';
    },

    events: {
      'change .select-list-list-checkbox': 'toggle'
    },

    initialize: function(options) {
      this.listenTo(this.model, 'change', this.render);
      this.settings = options.settings;
    },

    render: function() {
      this.$el.html(this.template(this.settings.format(this.model.toJSON())));
      this.$('input').prop('name', this.model.get('name'));
      this.$el.toggleClass('selected', this.model.get('selected'));
      this.$('.select-list-list-checkbox')
          .prop('title',
              this.model.get('selected') ?
                  this.settings.tooltips.deselect :
                  this.settings.tooltips.select)
          .prop('checked', this.model.get('selected'));
    },

    remove: function(postpone) {
      if (postpone) {
        var that = this;
          that.$el.addClass(this.model.get('selected') ? 'added' : 'removed');
          setTimeout(function() {
            Backbone.View.prototype.remove.call(that, arguments);
          }, 500);
      } else {
        Backbone.View.prototype.remove.call(this, arguments);
      }
    },

    toggle: function() {
      var selected = this.model.get('selected'),
          that = this,
          url = selected ? this.settings.deselectUrl : this.settings.selectUrl,
          data = $.extend({}, this.settings.extra || {});

      data[this.settings.selectParameter] = this.model.get(this.settings.selectParameterValue);

      that.$el.addClass('progress');
      $.ajax({
          url: url,
          type: 'POST',
          data: data
      })
          .done(function() {
            that.model.set('selected', !selected);
          })
          .fail(showError)
          .always(function() {
            that.$el.removeClass('progress');
          });
    }
  });



  /*
   * SelectList View
   */

  var SelectListView = Backbone.View.extend({
    template: function(l) {
      return '<div class="select-list-container">' +
          '<div class="select-list-control">' +
            '<div class="select-list-check-control">' +
              '<a class="select-list-control-button" name="selected">' + l.selected + '</a>' +
              '<a class="select-list-control-button" name="deselected">' + l.deselected + '</a>' +
              '<a class="select-list-control-button" name="all">' + l.all + '</a>' +
            '</div>' +
            '<div class="select-list-search-control">' +
              '<input type="text" placeholder="Search">' +
              '<a class="select-list-search-control-clear">&times;</a>' +
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
      'click .select-list-control-button[name=all]': 'showAll',

      'click .select-list-search-control-clear': 'clearSearch'
    },

    initialize: function(options) {
      this.listenTo(this.collection, 'add', this.renderListItem);
      this.listenTo(this.collection, 'reset', this.renderList);
      this.listenTo(this.collection, 'remove', this.removeModel);
      this.listenTo(this.collection, 'change:selected', this.confirmFilter);
      this.settings = options.settings;
    },

    render: function() {
      var that = this,
          keyup = function() { that.search(); };

      this.$el.html(this.template(this.settings.labels))
          .width(this.settings.width);

      this.$listContainer = this.$('.select-list-list-container')
          .height(this.settings.height)
          .css('overflow', 'auto')
          .on('scroll', function() { that.scroll(); });

      this.$list = this.$('.select-list-list');

      var searchInput = this.$('.select-list-search-control input')
          .on('keyup', $.debounce(250, keyup));

      setTimeout(function() {
        searchInput.focus();
      }, 250);

      this.listItemViews = [];

      showError = function() {
        $('<div>')
            .addClass('error').text(that.settings.errorMessage)
            .insertBefore(that.$el);
      };
    },

    renderList: function() {
      this.listItemViews.forEach(function(view) { view.remove(); });
      this.listItemViews = [];
      this.collection.each(this.renderListItem, this);
      this.$listContainer.scrollTop(0);
    },

    renderListItem: function(item) {
      var itemView = new SelectListItemView({
        model: item,
        settings: this.settings
      });
      this.listItemViews.push(itemView);
      this.$list.append(itemView.el);
      itemView.render();
    },

    confirmFilter: function(model) {
      if (this.currentFilter !== 'all') {
        this.collection.remove(model);
      }
    },

    removeModel: function(model, collection, options) {
      this.listItemViews[options.index].remove(true);
      this.listItemViews.splice(options.index, 1);
    },

    filterBySelection: function(filter) {
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
          success: function() {
            that.hideFetchSpinner();
          },
          error: showError
        });
      }
    },

    showSelected: function() {
      this.filterBySelection('selected');
    },

    showDeselected: function() {
      this.filterBySelection('deselected');
    },

    showAll: function() {
      this.filterBySelection('all');
    },

    search: function() {
      var query = this.$('.select-list-search-control input').val(),
          hasQuery = query.length > 0,
          that = this;

      this.$('.select-list-check-control').toggleClass('disabled', hasQuery);
      this.$('.select-list-search-control').toggleClass('disabled', !hasQuery);

      if (hasQuery) {
        this.showFetchSpinner();
        this.currentFilter = 'all';

        this.collection.fetch({
          url: this.settings.searchUrl,
          reset: true,
          data: { query: query },
          success: function() {
            that.hideFetchSpinner();
          },
          error: showError
        });
      } else {
        this.filterBySelection();
      }
    },

    searchByQuery: function(query) {
      this.$('.select-list-search-control input').val(query);
      this.search();
    },

    clearSearch: function() {
      this.filterBySelection();
    },

    showFetchSpinner: function() {
      this.$listContainer.addClass('loading');
    },

    hideFetchSpinner: function() {
      this.$listContainer.removeClass('loading');
    },

    scroll: function() {
      var scrollBottom = this.$listContainer.scrollTop() >=
          this.$list[0].scrollHeight - this.$listContainer.outerHeight(),
          that = this;

      if (scrollBottom && this.collection.more) {
        $.throttle(250, function() {
            that.showFetchSpinner();

            that.collection.fetchNextPage({
              success: function() { that.hideFetchSpinner(); }
            });
        })();
      }
    }

  });



  /*
   * SelectList Entry Point
   */

  window.SelectList = function(options) {
    this.settings = $.extend(window.SelectList.defaults, options);

    this.collection = new SelectListCollection();

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

  window.SelectList.prototype.filter = function(filter) {
    this.view.filterBySelection(filter);
    return this;
  };

  window.SelectList.prototype.search = function(query) {
    this.view.searchByQuery(query);
    return this;
  };



  /*
   * SelectList Defaults
   */

  window.SelectList.defaults = {
    width: '50%',
    height: 400,

    format: function (item) { return item.value; },

    labels: {
      selected: 'Selected',
      deselected: 'Deselected',
      all: 'All'
    },

    tooltips: {
      select: 'Click this to select item',
      deselect: 'Click this to deselect item'
    },

    errorMessage: 'Something gone wrong, try to reload the page and try again.'
  };

})(jQuery);
