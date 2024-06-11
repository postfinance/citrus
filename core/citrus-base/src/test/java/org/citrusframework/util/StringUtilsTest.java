/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class StringUtilsTest {

    @Test
    public void appendSegmentToPath() {
        Assert.assertEquals(StringUtils.appendSegmentToPath("s1","s2"), "s1/s2");
        Assert.assertEquals(StringUtils.appendSegmentToPath("s1/","s2"), "s1/s2");
        Assert.assertEquals(StringUtils.appendSegmentToPath("s1/","/s2"), "s1/s2");
        Assert.assertEquals(StringUtils.appendSegmentToPath("/s1","/s2"), "/s1/s2");
        Assert.assertEquals(StringUtils.appendSegmentToPath("/s1/","/s2"), "/s1/s2");
        Assert.assertEquals(StringUtils.appendSegmentToPath("/s1/","/s2/"), "/s1/s2/");
        Assert.assertEquals(StringUtils.appendSegmentToPath("/s1/",null), "/s1/");
        Assert.assertEquals(StringUtils.appendSegmentToPath(null,"/s2/"), "/s2/");
        Assert.assertNull(StringUtils.appendSegmentToPath(null,null));
    }
}