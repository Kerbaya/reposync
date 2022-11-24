/*
 * Copyright 2022 Kerbaya Software
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
	private static void testStrCtor(String input, String groupId, String artifactId, String extension, String classifier, String version)
	{
		ArtifactItem a = new ArtifactItem(input);
		Assert.assertEquals(groupId, a.getGroupId());
		Assert.assertEquals(artifactId, a.getArtifactId());
		Assert.assertEquals(extension, a.getExtension());
		Assert.assertEquals(classifier, a.getClassifier());
		Assert.assertEquals(version, a.getVersion());
	}
	
	private static void testToString(String input, String expected)
	{
		Assert.assertEquals(expected, new ArtifactItem(input).toString());
	}
	
	@Test
	public void stringInput()
	{
		testStrCtor("org.myorg:artifactId:", "org.myorg", "artifactId", "jar", "", "");
		testStrCtor("org.myorg:artifactId:version", "org.myorg", "artifactId", "jar", "", "version");
		testStrCtor("org.myorg:artifactId:ext:", "org.myorg", "artifactId", "ext", "", "");
		testStrCtor("org.myorg:artifactId:ext:version", "org.myorg", "artifactId", "ext", "", "version");
		testStrCtor("org.myorg:artifactId:ext:class:", "org.myorg", "artifactId", "ext", "class", "");
		testStrCtor("org.myorg:artifactId:ext:class:version", "org.myorg", "artifactId", "ext", "class", "version");
	}
	
	@Test
	public void toStringTest()
	{
		testToString("org.myorg:artifactId:", "org.myorg:artifactId:jar:");
		testToString("org.myorg:artifactId:version", "org.myorg:artifactId:jar:version");
		testToString("org.myorg:artifactId:ext:", "org.myorg:artifactId:ext:");
		testToString("org.myorg:artifactId:ext:version", "org.myorg:artifactId:ext:version");
		testToString("org.myorg:artifactId:ext:class:", "org.myorg:artifactId:ext:class:");
		testToString("org.myorg:artifactId:ext:class:version", "org.myorg:artifactId:ext:class:version");
	}
}
