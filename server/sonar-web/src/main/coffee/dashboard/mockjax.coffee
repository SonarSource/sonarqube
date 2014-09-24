define ['third-party/jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  jQuery.mockjax
    url: "#{baseUrl}/api/dashboards/details"
    responseText: JSON.stringify
      name: 'Helicopter View'
      description: ''
      shared: true
      layout: '50%-50%'

      canManageDashboards: true
      canManageWidgets: true

      widgets: [
        {
          key: 'measure_filter_list'
          props: [
            {
              key: 'filter'
              value: '48'
            }
          ]
          layout: {
            column: 1
            row: 1
          }
        }
        {
          key: 'my_reviews'
          props: []
          layout: {
            column: 1,
            row: 2
          }
        },
        {
          key: 'hotspot_most_violated_rules',
          props: [],
          layout: {
            column: 2
            row: 1
          }
        }
      ]

  jQuery.mockjax
    url: "#{baseUrl}/api/dashboards/available_widgets"
    responseText: JSON.stringify
      widgets: [
        {
          key: ''
          name: ''
          description: ''
          category: ''
          props: []
        }
      ]
