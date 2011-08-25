/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests

import org.gradle.util.DistributionLocator
import org.gradle.util.GradleVersion
import spock.lang.Specification

/**
 * @author: Szczepan Faber, created at: 8/20/11
 */
class DistributionLocatorIntegrationTest extends Specification {

    def locator = new DistributionLocator()

    def "locates release versions"() {
        expect:
        urlExist(locator.getDistributionFor(GradleVersion.version("0.8")))
        urlExist(locator.getDistributionFor(GradleVersion.version("0.9.1")))
        urlExist(locator.getDistributionFor(GradleVersion.version("1.0-milestone-3")))
    }

    def "locates snapshot versions"() {
        expect:
        urlExist(locator.getDistributionFor(GradleVersion.version("1.0-milestone-4-20110623211531+0200")))
        urlExist(locator.getDistributionFor(GradleVersion.version("1.0-milestone-4-20110725000027+0200")))
    }

    void urlExist(String url) {
        new URL(url).openStream().close()
    }
}