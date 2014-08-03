/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.gradle

import org.gradle.api.Project

class ModulePlugin extends BasePlugin {

	void apply(Project project) {

		super.apply(project)

		project.configure(project) {
			// add some common dependencies
			afterEvaluate {
				
				Object core = null
				Object test = null
				
				try {
					core = project.project(":axelor-core")
					test = project.project(":axelor-test")
				} catch (Exception e) {
					core = "com.axelor:axelor-core:${sdkVersion}"
					test = "com.axelor:axelor-test:${sdkVersion}"
				}
				
				dependencies {
					compile core
					testCompile test
				}
			}
        }
    }
}
