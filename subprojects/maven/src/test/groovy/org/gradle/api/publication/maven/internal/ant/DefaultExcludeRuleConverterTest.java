/*
 * Copyright 2007-2008 the original author or authors.
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
package org.gradle.api.publication.maven.internal.ant;

import org.apache.maven.model.Exclusion;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.internal.artifacts.DefaultExcludeRule;
import org.gradle.util.WrapUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Hans Dockter
 */
public class DefaultExcludeRuleConverterTest {
    private static final String TEST_ORG = "org";
    private static final String TEST_MODULE = "module";

    private DefaultExcludeRuleConverter excludeRuleConverter;

    @Before
    public void setUp() {
        excludeRuleConverter = new DefaultExcludeRuleConverter();   
    }
    
    @Test
    public void convertableRule() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(ExcludeRule.GROUP_KEY, TEST_ORG);
        params.put(ExcludeRule.MODULE_KEY, TEST_MODULE);
        DefaultExcludeRule excludeRule = new DefaultExcludeRule(params);
        Exclusion mavenExclude = excludeRuleConverter.convert(excludeRule);
        assertEquals(TEST_ORG, mavenExclude.getGroupId());
        assertEquals(TEST_MODULE, mavenExclude.getArtifactId());
    }
    
    @Test
    public void unconvertableRules() {
        checkForNull(new DefaultExcludeRule(WrapUtil.toMap(ExcludeRule.GROUP_KEY, TEST_ORG)));
        checkForNull(new DefaultExcludeRule(WrapUtil.toMap(ExcludeRule.MODULE_KEY, TEST_MODULE)));
    }

    private void checkForNull(DefaultExcludeRule excludeRule) {
        assertNull(excludeRuleConverter.convert(excludeRule));
    }
}
