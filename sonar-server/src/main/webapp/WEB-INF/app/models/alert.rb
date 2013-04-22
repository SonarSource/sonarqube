#
# SonarQube, open source software quality management tool.
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
class Alert < ActiveRecord::Base
  belongs_to :profile

  validates_presence_of :operator
  validates_uniqueness_of :metric_id, :scope => :profile_id


  def metric=(m)
    metric_id=m.id
  end

  def metric
    Metric.by_id(metric_id)
  end

  def name
    metric ? metric.short_name : ''
  end

  def <=>(other)
    name<=>other.name
  end

  protected

  def validate
    errors.add('Can not set alerts on data metrics.') if metric && metric.val_type==Metric::VALUE_TYPE_DATA
    errors.add_to_base('Can not set alerts on alerts.') if metric==Metric.by_key(Metric::ALERT_STATUS)
    errors.add_to_base('At least one threshold (warning or error) is required.') if value_error.blank? && value_warning.blank?
  end

end
