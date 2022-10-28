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

import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import org.junit.Test;

public class RegExFilterTest
{
	private static void check(boolean expected, String pattern, String coords)
	{
		Assert.assertEquals(expected, new RegExFilter(pattern).test(new DefaultArtifact(coords)));
	}
	
	private static void matched(String pattern, String coords)
	{
		check(true, pattern, coords);
	}
	
	private static void unmatched(String pattern, String coords)
	{
		check(false, pattern, coords);
	}
	
	@Test
	public void test()
	{
		matched("group:artifact:extension:version", "group:artifact:extension:version");
		matched("group:artifact:extension:1-SNAPSHOT", "group:artifact:extension:1-SNAPSHOT");
		matched("group:artifact:extension:classifier:version", "group:artifact:extension:classifier:version");
		matched("g.*p:a.*t:e.*n:c.*r:v.*n", "group:artifact:extension:classifier:version");
		unmatched("group:artifact:extension:classifier2:version", "group:artifact:extension:classifier:version");
	}
}
