var lib = require('../lib');


lib.initMessages();
lib.changeWorkingDirectory('coding-rules-spec');


casper.test.begin('Coding Rules - Readonly Tests', function suite(test) {

  var appId = null;
  var showId = null;

  casper.start(lib.buildUrl('coding-rules'), function() {
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_initial.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkInitialPageLoad() {

    casper.waitForSelector('.navigator-header', function checkHeader() {
      test.assertExists('.navigator-header h1');
      test.assertExists('button#coding-rules-new-search');
      test.assertDoesntExist('button#coding-rules-create-rule');
    });


    casper.waitForSelector('.navigator-filters', function checkDefaultFilters() {
      test.assertVisible('input[type="text"].query-filter-input');
      test.assertElementCount('.navigator-filter', 15);
    test.assertElementCount('.navigator-filter-optional', 12 /* Only query, qProfile and 'More' are visible by default */);
      test.assertVisible('button.navigator-filter-submit');
    });


    casper.waitForSelector('li.active', function checkResultsList() {
      test.assertElementCount('ol.navigator-results-list li', 10);
      test.assertElementCount('li.active', 1);
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'Xoo');
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'No empty line');
      test.assertSelectorHasText('ol.navigator-results-list li.active', 'BETA');
    });

    casper.waitForSelector('h3.coding-rules-detail-header', function showFirstRule() {
      test.assertSelectorHasText('h3.coding-rules-detail-header', 'No empty line');
      test.assertSelectorHasText('.navigator-details .subtitle', 'squid-xoo:x1');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(1)', 'severity.MINOR');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(2)', 'BETA');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(3)', 'convention, pitfall');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(4)', 'Testability > Integration level testability');
      test.assertSelectorHasText('.coding-rules-detail-property:nth-child(6)', 'SonarQube (Xoo)');

      test.assertDoesntExist('button#coding-rules-detail-extend-description');
    });
  });

  casper.then(function showDeprecated() {
    lib.clearRequestMock(showId);
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_deprecated.json');

    casper.click('div[name="squid:S0001"]');

    casper.waitWhileSelector('.navigator-details i.spinner');
    casper.waitForSelector('h3.coding-rules-detail-header', function() {
      test.assertSelectorHasText('h3.coding-rules-detail-header', 'Deprecated rule');
    });

  });

  casper.run(function() {
    test.done();
  });
});

casper.test.begin('Coding Rules - Admin Tests', function suite(test) {

  var showId = null;
  var updateId = null;

  casper.start(lib.buildUrl('coding-rules'), function() {
    lib.mockRequest('/api/l10n/index', '{}');
    lib.mockRequestFromFile('/api/rules/app', 'app_admin.json');
    lib.mockRequestFromFile('/api/rules/tags', 'tags.json');
    lib.mockRequestFromFile('/api/rules/search', 'search_initial.json');
    showId = lib.mockRequestFromFile('/api/rules/show', 'show_x1.json');
  });


  casper.waitWhileSelector("div#coding-rules-page-loader", function checkInitialPageLoad() {

    casper.waitForSelector('.navigator-header', function checkAdminHeader() {
      test.assertExist('button#coding-rules-create-rule');
    });


    casper.waitForSelector('h3.coding-rules-detail-header', function showFirstRule() {
      test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo shall not pass');

      test.assertExists('button#coding-rules-detail-extend-description');
    });


    casper.then(function editNote() {
      casper.click('button#coding-rules-detail-extend-description');
      test.assertSelectorHasText('textarea#coding-rules-detail-extend-description-text', 'As per the [Book of Xoo](http://xoo.sonarsource.com/book):\n> Xoo shall not pass!');

      casper.sendKeys('textarea#coding-rules-detail-extend-description-text', 'Xoo must pass');

      updateId = lib.mockRequestFromFile('/api/rules/update', 'edit_note_x1.json');
      casper.click('button#coding-rules-detail-extend-description-submit');

      casper.wait(500, function showUpdatedRule() {
        test.assertSelectorHasText('.coding-rules-detail-description-extra', 'Xoo must pass');
      });

    });
  });


  casper.run(function() {
    test.done();
  });
});
