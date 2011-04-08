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
module ReviewsHelper
  
  def self.severity_options_with_any
    severity_ops = []
    severity_ops << ["Any", ""]
    Review.severity_options.each do |severity|
      severity_ops << severity
    end
    return severity_ops
  end
  
  def self.status_options_with_any
    status_ops = []
    status_ops << ["Any", ""]
    Review.status_options.each do |status|
      status_ops << status
    end
    return status_ops
  end

end
