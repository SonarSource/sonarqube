define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Computation Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/computation/queue', 'computation-spec/queue.json')
          .mockFromFile('/api/computation/history', 'computation-spec/history.json')
          .startApp('computation/app')
          .checkElementCount('#computation-list li[data-id]', 1)
          .checkElementInclude('#computation-list', 'SonarQube')
          .checkElementInclude('#computation-list-footer', '1')
          .checkElementExist('.js-queue.selected')
          .clickElement('.js-history')
          .checkElementCount('#computation-list li[data-id]', 3)
          .checkElementInclude('#computation-list', 'Duration')
          .checkElementExist('.js-history.selected')
          .checkElementExist('.panel-danger[data-id="3"]')
          .clickElement('.js-queue')
          .checkElementCount('#computation-list li[data-id]', 1);
    });

    bdd.it('should show more', function () {
      return this.remote
          .open('#past')
          .mockFromFile('/api/computation/queue', 'computation-spec/queue.json')
          .mockFromFile('/api/computation/history', 'computation-spec/history-big-1.json')
          .startApp('computation/app')
          .checkElementCount('#computation-list li[data-id]', 2)
          .clearMocks()
          .mockFromFile('/api/computation/history', 'computation-spec/history-big-2.json', { data: { p: 2 } })
          .clickElement('#computation-fetch-more')
          .checkElementCount('#computation-list li[data-id]', 3);
    });
  });
});
