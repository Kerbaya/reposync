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

public class ArtifactItemTest
{
	private static ArtifactItem test(String str)
	{
		return new ArtifactItem(str);
	}
	
	private static void assertProperties(
			String str, 
			String groupId, 
			String artifactId,
			String extension, 
			String classifier,
			String version)
	{
		ArtifactItem ai = new ArtifactItem(str);
		Assert.assertEquals("groupId", groupId, ai.getGroupId());
		Assert.assertEquals("artifactId", artifactId, ai.getArtifactId());
		Assert.assertEquals("extension", extension, ai.getExtension());
		Assert.assertEquals("classifier", classifier, ai.getClassifier());
		Assert.assertEquals("version", version, ai.getVersion());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void oneTokenInvalid()
	{
		test("one");
	}

	@Test(expected=IllegalArgumentException.class)
	public void twoTokenInvalid()
	{
		test("one:two");
	}

	@Test(expected=IllegalArgumentException.class)
	public void sixTokenInvalid()
	{
		test("one:two:three:four:five:six");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void emptyArtifactIdInvalid()
	{
		test(":two:three");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void emptyGroupIdInvalid()
	{
		test("one::three");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void emptyVersionInvalid()
	{
		test("one:two:");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void artifactIdCharInvalid()
	{
		test("one :two:three");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void groupIdCharInvalid()
	{
		test("one: two:three");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void versionCharInvalid()
	{
		test("one:two: three");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void extensionCharInvalid()
	{
		test("one:two:three: four");
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void classifierCharInvalid()
	{
		test("one:two:three:four: five");
	}
	
	@Test
	public void threeToken()
	{
		assertProperties("one:two:three", "one", "two", "jar", "", "three");
	}
	
	@Test
	public void fourToken()
	{
		assertProperties(
				"one:two:three:four", "one", "two", "three", "", "four");
	}
	
	@Test
	public void fiveToken()
	{
		assertProperties(
				"one:two:three:four:five", 
				"one", 
				"two", 
				"three", 
				"four", 
				"five");
	}
}
