/*
 * Copyright 2018 Kerbaya Software
 * 
 * This file is part of reposync. 
 * 
 * reposync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * reposync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with reposync.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.maven.reposync;

import org.junit.Assert;
import org.junit.Test;

public class ExtraItemTest
{
	private static void assertProperties(
			String str, 
			String extension, 
			String classifier)
	{
		ExtraItem ei = new ExtraItem(str);
		Assert.assertEquals("extension", extension, ei.getExtension());
		Assert.assertEquals("classifier", classifier, ei.getClassifier());
	}
	
	private static void test(String str)
	{
		new ExtraItem(str);
	}
	
	@Test
	public void zeroToken()
	{
		assertProperties("", "", "");
	}

	@Test
	public void oneToken()
	{
		assertProperties("jar", "jar", "");
		assertProperties("jar:", "jar", "");
	}
	
	@Test
	public void twoToken()
	{
		assertProperties("jar:sources", "jar", "sources");
		assertProperties(":sources", "", "sources");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void threeTokenInvalid()
	{
		test("one:two:three");
	}
	
	@Test(expected=NullPointerException.class)
	public void nullExtensionInvalid()
	{
		new ExtraItem(null, "");
	}
	
	@Test(expected=NullPointerException.class)
	public void nullClassificationInvalid()
	{
		new ExtraItem("", null);
	}
}
