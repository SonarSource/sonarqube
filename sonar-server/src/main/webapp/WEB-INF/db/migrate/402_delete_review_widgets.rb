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
# Sonar 3.6
#
class DeleteReviewWidgets < ActiveRecord::Migration

  class WidgetProperty < ActiveRecord::Base
  end

  class Widget < ActiveRecord::Base
  end

  def self.up
    delete_widget 'reviews_metrics'
    delete_widget 'planned_reviews'
    delete_widget 'unplanned_reviews'
    delete_widget 'project_reviews'
  end

  def self.delete_widget(widget_key)
    Widget.find(:all, :conditions => {:widget_key => widget_key}).each do |widget|
      WidgetProperty.delete_all(:widget_id => widget.id)
      Widget.delete(widget.id)
    end
  end
end