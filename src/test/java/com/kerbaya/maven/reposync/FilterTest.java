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

public class FilterTest
{
	private static final void check(boolean expected, Filter filter, String coords)
	{
		Assert.assertEquals(expected, filter.test(new DefaultArtifact(coords)));
	}
	
	private static final void matched(Filter filter, String coords)
	{
		check(true, filter, coords);
	}
	
	private static final void unmatched(Filter filter, String coords)
	{
		check(false, filter, coords);
	}
	
	@Test
	public void test()
	{
		matched(Filter.SNAPSHOT, "group:artifact:1-SNAPSHOT");
		matched(Filter.SNAPSHOT, "group:artifact:1-20220101.012345-1");
		unmatched(Filter.SNAPSHOT, "group:artifact:1");
		unmatched(Filter.RELEASE, "group:artifact:1-SNAPSHOT");
		unmatched(Filter.RELEASE, "group:artifact:1-20220101.012345-1");
		matched(Filter.RELEASE, "group:artifact:1");
	}
}
