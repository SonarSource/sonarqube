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
class Review < ActiveRecord::Base
  belongs_to :user
  belongs_to :rule_failure
  belongs_to :resource, :class_name => "Project", :foreign_key => "resource_id"
  has_many :review_comments, :order => "created_at", :dependent => :destroy
  validates_presence_of :user, :message => "can't be empty"
  validates_presence_of :review_type, :message => "can't be empty"
  validates_presence_of :status, :message => "can't be empty"

  INFO = "info"
  MINOR = "minor"
  MAJOR = "major"
  CRITICAL = "critical"
  BLOCKER = "blocker"


  def self.default_severity
    return MAJOR
  end
  
  def self.severity_options
    severity_ops = []
    severity_ops << ["Info", INFO]
    severity_ops << ["Minor", MINOR]
    severity_ops << ["Major", MAJOR]
    severity_ops << ["Critical", CRITICAL]
    severity_ops << ["Blocker", BLOCKER]
    return severity_ops
  end

end
