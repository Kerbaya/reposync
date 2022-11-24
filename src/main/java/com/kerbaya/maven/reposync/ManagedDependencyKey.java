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

import java.util.Objects;

import org.apache.maven.model.Dependency;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
class ManagedDependencyKey
{
	private static final String DEFAULT_TYPE = "jar";

	private final String groupId;
	private final String artifactId;
	private final String type;
	private final String classifier;
	
	public ManagedDependencyKey(Dependency dep)
	{
		this(
				Objects.requireNonNull(dep.getGroupId()),
				Objects.requireNonNull(dep.getArtifactId()),
				Utils.nullToDefault(dep.getType(), DEFAULT_TYPE),
				Utils.nullToEmpty(dep.getClassifier()));
	}
}
