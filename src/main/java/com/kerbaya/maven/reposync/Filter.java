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

import java.util.function.Predicate;

import org.eclipse.aether.artifact.Artifact;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Filter implements Predicate<Artifact>
{
	SNAPSHOT(true),
	RELEASE(false),
	;
	
	private final boolean snapshot;
	
	@Override
	public boolean test(Artifact t)
	{
		return t.isSnapshot() == snapshot;
	}
}
