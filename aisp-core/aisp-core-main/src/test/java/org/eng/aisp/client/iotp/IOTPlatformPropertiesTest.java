/*******************************************************************************
 * Copyright [2022] [IBM]
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
 *******************************************************************************/
package org.eng.aisp.client.iotp;

import java.util.List;
import java.util.Properties;

import org.eng.ENGTestUtils;
import org.eng.aisp.AISPLogger;
import org.junit.Assert;
import org.junit.Test;

public class IOTPlatformPropertiesTest {

	@Test
	public void testParseInstances() {
		IOTPlatformProperties.instance().setPropertyFilePath(ENGTestUtils.GetTestData("iotp/iotp.properties"), AISPLogger.logger);
		IOTPlatformProperties iotp = IOTPlatformProperties.instance();
		List<Properties> propsList = iotp.getApplicationClientProperties();
		Assert.assertTrue(propsList.size() == 5);

		// Find the platform that has this user.
		Properties p = iotp.getApplicationClientProperties("jr9k9ouser@gmail.com");
		Assert.assertTrue(p != null);
		Assert.assertTrue(p.getProperty(IOTPlatformProperties.ORG_ID_PROPERTY_NAME).equals("jr9k9o"));

		// Find the platform that has this user.
		p = iotp.getApplicationClientProperties("tkswmpuser@gmail.com");
		Assert.assertTrue(p != null);
		Assert.assertTrue(p.getProperty(IOTPlatformProperties.ORG_ID_PROPERTY_NAME).equals("tkswmp"));

		// Find the 1st platform that supports any user.
		p = iotp.getApplicationClientProperties("nonuser@gmail.com");
		Assert.assertTrue(p != null);
		Assert.assertTrue(p.getProperty(IOTPlatformProperties.ORG_ID_PROPERTY_NAME).equals("qxhqj9"));	// selected before sl0l32
		
		// Make sure we have 2 users for this platform using getEnabledUsers().
		String users[] = iotp.getEnabledUsers("tkswmp");
		Assert.assertTrue(users != null);
		Assert.assertTrue(users.length == 2);

		// An instance disabled because it has an empty user list.
		users = iotp.getEnabledUsers("nousers");
		Assert.assertTrue(users != null);
		Assert.assertTrue(users.length == 0);

//		iotp.connect(true);
//		iotp.disconnect(true);
	}

}
