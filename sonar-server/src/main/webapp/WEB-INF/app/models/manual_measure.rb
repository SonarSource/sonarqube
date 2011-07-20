#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class ManualMeasure < ActiveRecord::Base
  belongs_to :resource, :class_name => 'Project'
  validates_uniqueness_of :metric_id, :scope => :resource_id
  validates_length_of :text_value, :maximum => 4000, :allow_nil => true, :allow_blank => true
  validates_length_of :url, :maximum => 4000, :allow_nil => true, :allow_blank => true
  validates_length_of :description, :maximum => 4000, :allow_nil => true, :allow_blank => true
  validate :validate_metric

  def metric
    @metric ||=
        begin
          Metric.by_id(metric_id)
        end
  end

  def user
    @user ||=
        begin
          user_login ? User.find(:first, :conditions => ['login=?', user_login]) : nil
        end
  end

  def username
    user ? user.name : user_login
  end

  def metric=(m)
    @metric = m
    write_attribute(:metric_id, m.id) if m.id
  end

  def validate_metric
    errors.add_to_base("Not a valid metric") unless metric && metric.enabled?
  end
end