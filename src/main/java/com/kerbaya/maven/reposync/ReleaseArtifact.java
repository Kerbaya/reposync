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

import java.util.regex.Pattern;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.DelegatingArtifact;

class ReleaseArtifact extends DelegatingArtifact
{
	private static final Pattern SNAPSHOT_PATTERN = Pattern.compile(".+-\\d{8}\\.\\d{6}-\\d+");
	
	private ReleaseArtifact(Artifact delegate)
	{
		super(delegate);
	}
	
	@Override
	public boolean isSnapshot()
	{
		return false;
	}
	
	@Override
	public String getBaseVersion()
	{
		return getVersion();
	}
	
	@Override
	protected ReleaseArtifact newInstance(Artifact artifact)
	{
		return create(artifact);
	}

	public static ReleaseArtifact create(Artifact artifact)
	{
		return artifact instanceof ReleaseArtifact ?
				(ReleaseArtifact) artifact : new ReleaseArtifact(artifact);
	}
	
	public static Artifact timestampSnapshotAsRelease(Artifact artifact)
	{
		return artifact.isSnapshot() && SNAPSHOT_PATTERN.matcher(artifact.getVersion()).matches() ?
				create(artifact) : artifact;
	}
}
