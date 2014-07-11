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
# License along with {library}; if not, write to the Free Software
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
 class ActiveRuleParameter < ActiveRecord::Base
   belongs_to :active_rule
   belongs_to :rules_parameter

   validates_presence_of :rules_parameter_key

   def name
    rules_parameter.name
   end

   def parameter
     rules_parameter
   end

 end
