#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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
class Characteristic < ActiveRecord::Base

  NAME_MAX_SIZE=100

  FUNCTION_CONSTANT_ISSUE = "constant_issue";
  FUNCTION_LINEAR = "linear";
  FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";

  DAY = "d"
  HOUR = "h"
  MINUTE = "mn"

  belongs_to :parent, :class_name => 'Characteristic', :foreign_key => 'parent_id'

  # Needed for Views Plugin. Remove it when the plugin will not used it anymore
  belongs_to :rule

  validates_uniqueness_of :name, :scope => [:enabled], :case_sensitive => false, :if => Proc.new { |c| c.enabled }
  validates_length_of :name, :in => 1..NAME_MAX_SIZE, :allow_blank => false

  def root?
    parent_id.nil?
  end

  def requirement?
    rule_id.nil?
  end

  def key
    kee
  end

  def name(rule_name_if_empty=false)
    read_attribute(:name)
  end

end
