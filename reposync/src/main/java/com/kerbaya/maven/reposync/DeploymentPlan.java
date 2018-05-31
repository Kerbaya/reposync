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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;

final class DeploymentPlan
{
	private static final class PlanKey
	{
		private final String groupId;
		private final String artifactId;
		private final String version;
		
		public PlanKey(Artifact artifact)
		{
			groupId = artifact.getGroupId();
			artifactId = artifact.getArtifactId();
			version = artifact.getVersion();
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(groupId, artifactId, version);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == this)
			{
				return true;
			}
			if (!(obj instanceof PlanKey))
			{
				return false;
			}
			PlanKey other = (PlanKey) obj;
			return Objects.equals(groupId, other.groupId)
					&& Objects.equals(artifactId, other.artifactId)
					&& Objects.equals(version, other.version);
		}
	}
	
	private final Map<PlanKey, Set<Artifact>> artifactMap = new HashMap<>();
	
	private final ArtifactResolver resolver;
	private final ProjectBuildingRequest request;
	private final ProjectBuilder builder;
	private final boolean includeJavadoc;
	private final boolean includeSources;
	private final boolean includePom;
	private final boolean includeParentPoms;

	public DeploymentPlan(
			ArtifactResolver resolver,
			ProjectBuildingRequest request, 
			ProjectBuilder builder,
			boolean includeJavadoc, 
			boolean includeSources, 
			boolean includePom,
			boolean includeParentPoms)
	{
		this.resolver = resolver;
		this.request = request;
		this.builder = builder;
		this.includeJavadoc = includeJavadoc;
		this.includeSources = includeSources;
		this.includePom = includePom;
		this.includeParentPoms = includeParentPoms;
	}

	private Artifact findExtra(
			Artifact artifact, String extension, String classifier)
	{
		DefaultArtifactCoordinate ac = new DefaultArtifactCoordinate();
		ac.setGroupId(artifact.getGroupId());
		ac.setArtifactId(artifact.getArtifactId());
		ac.setVersion(artifact.getVersion());
		ac.setExtension(extension);
		ac.setClassifier(classifier);
		final ArtifactResult extraResult;
		try
		{
			extraResult = resolver.resolveArtifact(request, ac);
		}
		catch (ArtifactResolverException e)
		{
			return null;
		}
		return extraResult.getArtifact();
	}

	private void addExtra(
			Collection<? super Artifact> set,
			Artifact artifact, 
			String extension, 
			String classifier)
	{
		artifact = findExtra(artifact, extension, classifier);
		if (artifact != null)
		{
			set.add(artifact);
		}
	}
	
	private Set<Artifact> insertArtifact(Artifact artifact)
	{
		PlanKey key = new PlanKey(artifact);
		Set<Artifact> set = artifactMap.get(key);
		if (set == null)
		{
			set = new HashSet<>();
			artifactMap.put(key, set);
		}
		return set.add(artifact) ? set : null;
	}

	
	public void addArtifact(Artifact artifact) throws MojoExecutionException
	{
		Set<Artifact> set = insertArtifact(artifact);
		
		if (set == null)
		{
			return;
		}
		
		
		if (includeJavadoc)
		{
			addExtra(set, artifact, "jar", "javadoc");
		}
		if (includeSources)
		{
			addExtra(set, artifact, "jar", "sources");
		}
		if (!includePom && !includeParentPoms)
		{
			return;
		}
		
		
		Artifact pom = findExtra(artifact, "pom", null);
		if (pom == null)
		{
			return;
		}
		
		
		if (includePom)
		{
			set.add(pom);
		}
		if (includeParentPoms)
		{
			while ((pom = getParent(pom)) != null)
			{
				if (insertArtifact(pom) == null)
				{
					break;
				}
			}
		}
	}
	
	private Artifact getParent(Artifact pom) throws MojoExecutionException
	{
		final ProjectBuildingResult buildResult;
		try
		{
			buildResult = builder.build(
					pom,
					new DefaultProjectBuildingRequest(request)
							.setValidationLevel(
									ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL));
		}
		catch (ProjectBuildingException e)
		{
			throw new MojoExecutionException("Failed to build project", e);
		}
		return buildResult.getProject().getParentArtifact();
	}
	
	public Collection<Set<Artifact>> getArtifactSets()
	{
		return artifactMap.values();
	}
}
