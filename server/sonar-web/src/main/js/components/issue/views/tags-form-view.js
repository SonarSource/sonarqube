import $ from 'jquery';
import _ from 'underscore';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-tags-form.hbs';
import OptionTemplate from '../templates/issue-tags-form-option.hbs';

export default ActionOptionsView.extend({
  template: Template,
  optionTemplate: OptionTemplate,

  modelEvents: {
    'change:tags': 'renderTags'
  },

  events: function () {
    return _.extend(ActionOptionsView.prototype.events.apply(this, arguments), {
      'click input': 'onInputClick',
      'keydown input': 'onInputKeydown',
      'keyup input': 'onInputKeyup'
    });
  },

  initialize: function () {
    ActionOptionsView.prototype.initialize.apply(this, arguments);
    this.query = '';
    this.tags = [];
    this.selected = 0;
    this.debouncedSearch = _.debounce(this.search, 250);
    this.requestTags();
  },

  requestTags: function (query) {
    var that = this;
    return $.get(baseUrl + '/api/issues/tags', { ps: 10, q: query }).done(function (data) {
      that.tags = data.tags;
      that.renderTags();
    });
  },

  onRender: function () {
    var that = this;
    ActionOptionsView.prototype.onRender.apply(this, arguments);
    this.renderTags();
    setTimeout(function () {
      that.$('input').focus();
    }, 100);
  },

  selectInitialOption: function () {
    this.selected = Math.max(Math.min(this.selected, this.getOptions().length - 1), 0);
    this.makeActive(this.getOptions().eq(this.selected));
  },

  filterTags: function (tags) {
    var that = this;
    return _.filter(tags, function (tag) {
      return tag.indexOf(that.query) !== -1;
    });
  },

  renderTags: function () {
    this.$('.menu').empty();
    this.filterTags(this.getTags()).forEach(this.renderSelectedTag, this);
    this.filterTags(_.difference(this.tags, this.getTags())).forEach(this.renderTag, this);
    if (this.query.length > 0 && this.tags.indexOf(this.query) === -1 && this.getTags().indexOf(this.query) === -1) {
      this.renderCustomTag(this.query);
    }
    this.selectInitialOption();
  },

  renderSelectedTag: function (tag) {
    var html = this.optionTemplate({
      tag: tag,
      selected: true,
      custom: false
    });
    return this.$('.menu').append(html);
  },

  renderTag: function (tag) {
    var html = this.optionTemplate({
      tag: tag,
      selected: false,
      custom: false
    });
    return this.$('.menu').append(html);
  },

  renderCustomTag: function (tag) {
    var html = this.optionTemplate({
      tag: tag,
      selected: false,
      custom: true
    });
    return this.$('.menu').append(html);
  },

  selectOption: function (e) {
    e.preventDefault();
    e.stopPropagation();
    var tags = this.getTags().slice(),
        tag = $(e.currentTarget).data('value');
    if ($(e.currentTarget).data('selected') != null) {
      tags = _.without(tags, tag);
    } else {
      tags.push(tag);
    }
    this.selected = this.getOptions().index($(e.currentTarget));
    return this.submit(tags);
  },

  submit: function (tags) {
    var that = this;
    var _tags = this.getTags();
    this.model.set({ tags: tags });
    return $.ajax({
      type: 'POST',
      url: baseUrl + '/api/issues/set_tags',
      data: {
        key: this.model.id,
        tags: tags.join()
      }
    }).fail(function () {
      return that.model.set({ tags: _tags });
    });
  },

  onInputClick: function (e) {
    e.stopPropagation();
  },

  onInputKeydown: function (e) {
    this.query = this.$('input').val();
    if (e.keyCode === 38) {
      this.selectPreviousOption();
    }
    if (e.keyCode === 40) {
      this.selectNextOption();
    }
    if (e.keyCode === 13) {
      this.selectActiveOption();
    }
    if (e.keyCode === 27) {
      this.destroy();
    }
    if ([9, 13, 27, 38, 40].indexOf(e.keyCode) !== -1) {
      return false;
    }
  },

  onInputKeyup: function () {
    var query = this.$('input').val();
    if (query !== this.query) {
      this.query = query;
      this.debouncedSearch(query);
    }
  },

  search: function (query) {
    this.query = query;
    return this.requestTags(query);
  },

  resetAssignees: function (users) {
    this.assignees = users.map(function (user) {
      return { id: user.login, text: user.name };
    });
    this.renderTags();
  },

  getTags: function () {
    return this.model.get('tags') || [];
  }
});


