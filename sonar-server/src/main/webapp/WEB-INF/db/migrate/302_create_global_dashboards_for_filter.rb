#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

#
# Sonar 3.1
#
class CreateGlobalDashboardsForFilter < ActiveRecord::Migration
  class ActiveFilter < ActiveRecord::Base
  end

  class Dashboard < ActiveRecord::Base
  end

  class ActiveDashboard < ActiveRecord::Base
  end

  class Widget < ActiveRecord::Base
  end

  class WidgetProperty < ActiveRecord::Base
  end

  class Filter < ActiveRecord::Base
  end

  def self.up
    dashboard_per_filter = create_global_dashboards()

    activate_dashboards(dashboard_per_filter)

    drop_table('active_filters')
  end

  def self.create_global_dashboards
    dashboards = {}

    Filter.find(:all).each do |filter|
      dashboard = Dashboard.create(:user_id => filter.user_id,
                                   :name => filter.name,
                                   :description => '',
                                   :column_layout => '100%',
                                   :shared => filter.shared,
                                   :is_global => true)

      widget = Widget.create(:dashboard_id => dashboard.id,
                             :widget_key => 'filter',
                             :name => 'Filter',
                             :column_index => 1,
                             :row_index => 1,
                             :configured => true)

      WidgetProperty.create(:widget_id => widget.id,
                            :kee => 'filter',
                            :text_value => filter.id)

      dashboards[filter.id] = dashboard
    end

    dashboards
  end

  def self.activate_dashboards(dashboard_per_filter)
    ActiveFilter.find(:all).each do |activeFilter|
      filter = Filter.find_by_id(activeFilter.filter_id)
      if filter
        dashboard = dashboard_per_filter[filter.id]

        if !filter.favourites || activeFilter.user_id
          ActiveDashboard.create(:dashboard_id => dashboard.id,
                                 :user_id => activeFilter.user_id,
                                 :order_index => activeFilter.order_index)
        end
      end
    end
  end

end

