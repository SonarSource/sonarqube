/*global Backbone:false*/

(function ($) {

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

    fetchNextPage: function() {
      if (this.more) {
        var nextPage = this.settings.data.page + 1,
            settings = this.settings;

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

    events: {
      'click': 'toggle'
    },

    initialize: function(options) {
      this.listenTo(this.model, 'change', this.render);
      this.settings = options.settings;
    },

    render: function() {
      this.$el.html(this.settings.format(this.model.toJSON()));
      this.$el.toggleClass('selected', this.model.get('selected'));
    },

    toggle: function() {
      var selected = this.model.get('selected'),
          model = this.model;

      var url = selected ? this.settings.deselectUrl : this.settings.selectUrl;
      $.ajax({
          url: url,
          type: 'POST',
          data: { user: this.model.id }
      }).done(function() {
            model.set('selected', !selected);
          });
    }
  });



  /*
   * SelectList View
   */

  var SelectListView = Backbone.View.extend({
    template:
        '<div class="select-list-container">' +
          '<div class="select-list-control">' +
            '<div class="select-list-check-control">' +
              '<a class="select-list-control-button" name="selected">Selected</a>' +
              '<a class="select-list-control-button" name="deselected">Deselected</a>' +
              '<a class="select-list-control-button" name="all">All</a>' +
            '</div>' +
            '<div class="select-list-search-control">' +
              '<input type="text" placeholder="Search">' +
              '<a class="select-list-search-control-clear">&times;</a>' +
            '</div>' +
          '</div>' +
          '<div class="select-list-list-container">' +
            '<ul class="select-list-list"></ul>' +
          '</div>' +
        '</div>',

    events: {
      'click .select-list-control-button[name=selected]': 'showSelected',
      'click .select-list-control-button[name=deselected]': 'showDeselected',
      'click .select-list-control-button[name=all]': 'showAll',

      'keyup .select-list-search-control input': 'search',
      'click .select-list-search-control-clear': 'clearSearch'
    },

    initialize: function(options) {
      this.listenTo(this.collection, 'add', this.renderListItem);
      this.listenTo(this.collection, 'reset', this.renderList);
      this.settings = options.settings;
    },

    render: function() {
      var that = this;

      this.$el.html(this.template)
          .width(this.settings.width);

      this.$listContainer = this.$('.select-list-list-container')
          .height(this.settings.height)
          .css('overflow', 'scroll')
          .on('scroll', function() { that.scroll(); });

      this.$list = this.$('.select-list-list');

      this.listItemViews = [];
    },

    renderList: function() {
      this.listItemViews.forEach(function(view) { view.remove(); });
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

    filterBySelection: function(filter) {
      filter = this.currentFilter = filter || this.currentFilter;

      if (filter != null) {
        this.$('.select-list-check-control').toggleClass('disabled', false);
        this.$('.select-list-search-control').toggleClass('disabled', true);
        this.$('.select-list-search-control input').val('');

        this.$('.select-list-control-button').removeClass('active')
            .filter('[name=' + filter + ']').addClass('active');

        this.collection.fetch({
          url: this.settings.searchUrl,
          reset: true,
          data: { selected: filter }
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
          hasQuery = query.length > 0;

      this.$('.select-list-check-control').toggleClass('disabled', hasQuery);
      this.$('.select-list-search-control').toggleClass('disabled', !hasQuery);

      if (hasQuery > 0) {
        this.collection.fetch({
          url: this.settings.searchUrl,
          reset: true,
          data: { query: query }
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

    scroll: function() {
      var scrollBottom = this.$listContainer.scrollTop() >=
          this.$list[0].scrollHeight - this.$listContainer.outerHeight(),
          that = this;

      if (scrollBottom) {
        $.throttle(250, function() {
          that.collection.fetchNextPage();
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
    format: function (item) { return item.value; }
  };

})(jQuery);
