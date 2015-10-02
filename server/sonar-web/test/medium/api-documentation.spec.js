define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('API Documentation Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/webservices/list', 'api-documentation/list.json')
          .startAppBrowserify('api-documentation')
          .checkElementExist('.api-documentation-results .list-group-item')
          .checkElementCount('.api-documentation-results .list-group-item', 2)
          .checkElementInclude('.list-group-item[data-path="api/public"] .list-group-item-heading', 'api/public')
          .checkElementInclude('.list-group-item[data-path="api/public"] .list-group-item-text',
          'api/public description')
          .checkElementInclude('.list-group-item[data-path="api/internal"] .list-group-item-heading', 'api/internal')
          .checkElementInclude('.list-group-item[data-path="api/internal"] .list-group-item-text',
          'api/internal description')
          .checkElementInclude('.list-group-item[data-path="api/internal"]', 'internal')
          .checkElementExist('.list-group-item[data-path="api/public"]:not(.hidden)')
          .checkElementExist('.list-group-item.hidden[data-path="api/internal"]')
          .checkElementNotExist('#api-documentation-show-internal:checked')
          .clickElement('#api-documentation-show-internal')
          .checkElementExist('.list-group-item[data-path="api/public"]:not(.hidden)')
          .checkElementExist('.list-group-item[data-path="api/internal"]:not(.hidden)')
          .checkElementExist('#api-documentation-show-internal:checked');
    });

    bdd.it('should show actions', function () {
      return this.remote
          .open()
          .mockFromFile('/api/webservices/list', 'api-documentation/list.json')
          .startAppBrowserify('api-documentation')
          .checkElementExist('.api-documentation-results .list-group-item')
          .clickElement('.list-group-item[data-path="api/public"]')
          .checkElementCount('.search-navigator-workspace-details .panel', 2)
          .checkElementInclude('.panel[data-action="do"]', 'POST api/public/do')
          .checkElementInclude('.panel[data-action="do"]', 'api/public/do description')
          .checkElementInclude('.panel[data-action="do"]', 'Since 3.6')
          .checkElementCount('.panel[data-action="do"] table tr', 1)
          .checkElementInclude('.panel[data-action="do"] table tr', 'format')
          .checkElementInclude('.panel[data-action="do"] table tr', 'optional')
          .checkElementInclude('.panel[data-action="do"] table tr', 'api/public/do format description')
          .checkElementInclude('.panel[data-action="do"] table tr', 'json')
          .checkElementInclude('.panel[data-action="do"] table tr', 'xml');
    });

    bdd.it('should show example response', function () {
      return this.remote
          .open()
          .mockFromFile('/api/webservices/list', 'api-documentation/list.json')
          .mockFromFile('/api/webservices/response_example', 'api-documentation/response-example.json')
          .startAppBrowserify('api-documentation')
          .checkElementExist('.api-documentation-results .list-group-item')
          .clickElement('.list-group-item[data-path="api/public"]')
          .clickElement('.panel[data-action="undo"] .js-show-response-example')
          .checkElementExist('.panel[data-action="undo"] pre')
          .checkElementInclude('.panel[data-action="undo"] pre', 'leia.organa');
    });

    //bdd.it('should open WS permalink', function () {
    //  return this.remote
    //      .open('#api/public')
    //      .mockFromFile('/api/webservices/list', 'api-documentation/list.json')
    //      .startAppBrowserify('api-documentation')
    //      .checkElementExist('.api-documentation-results .list-group-item')
    //      .checkElementExist('.panel[data-web-service="api/public"]')
    //      .checkElementCount('.panel[data-web-service="api/public"]', 2);
    //});
    //
    //bdd.it('should open action permalink', function () {
    //  return this.remote
    //      .open('#api/internal/move')
    //      .mockFromFile('/api/webservices/list', 'api-documentation/list.json')
    //      .startAppBrowserify('api-documentation')
    //      .checkElementExist('.api-documentation-results .list-group-item')
    //      .checkElementExist('.panel[data-web-service="api/internal"]')
    //      .checkElementExist('.panel[data-web-service="api/internal"][data-action="move"]');
    //});
  });
});
