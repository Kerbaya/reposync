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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

/**
 * Methods to convert Maven repository objects to Aether counterparts
 * 
 * @author Glenn.Lane@kerbaya.com
 *
 */
final class AetherUtils
{
	private AetherUtils() {}
	
    public static Authentication createAuth(
    		org.apache.maven.artifact.repository.Authentication mavenAuth)
    {
    	return mavenAuth == null ?
    			null 
    			: new AuthenticationBuilder()
    					.addUsername(mavenAuth.getUsername())
    					.addPassword(mavenAuth.getPassword())
    					.addPrivateKey(
    							mavenAuth.getPrivateKey(), 
    							mavenAuth.getPassphrase()).build();
    }
    
    public static RepositoryPolicy createPolicy(ArtifactRepositoryPolicy policy)
    {
    	return policy == null ?
    			null
    			: new RepositoryPolicy(
    					policy.isEnabled(), 
    					policy.getUpdatePolicy(), 
    					policy.getChecksumPolicy());
    }
    
    public static Proxy createProxy(
    		org.apache.maven.repository.Proxy mavenProxy)
    {
    	return mavenProxy == null ?
    			null 
    			: new Proxy(
    					mavenProxy.getProtocol(), 
    					mavenProxy.getHost(), 
    					mavenProxy.getPort(), 
    					new AuthenticationBuilder()
    							.addUsername(mavenProxy.getUserName())
    							.addPassword(mavenProxy.getPassword())
    							.build());
    }
    
    public static List<RemoteRepository> createRepoList(
    		List<? extends ArtifactRepository> mavenRepos)
    {
    	if (mavenRepos == null)
    	{
    		return null;
    	}
    	List<RemoteRepository> result = new ArrayList<>(mavenRepos.size());
    	for (ArtifactRepository mavenRepo: mavenRepos)
    	{
    		result.add(createRepo(mavenRepo));
    	}
    	return result;
    }
    
    public static RemoteRepository createRepo(ArtifactRepository mavenRepo)
    {
    	return mavenRepo == null ?
    			null
    			: new RemoteRepository.Builder(
    					mavenRepo.getId(), 
    					mavenRepo.getLayout().getId(), 
    					mavenRepo.getUrl())
		    					.setAuthentication(createAuth(
										mavenRepo.getAuthentication()))
		    					.setMirroredRepositories(createRepoList(
		    							mavenRepo.getMirroredRepositories()))
		    					.setProxy(createProxy(mavenRepo.getProxy()))
		    					.setReleasePolicy(createPolicy(
		    							mavenRepo.getReleases()))
		    					.setSnapshotPolicy(createPolicy(
		    							mavenRepo.getSnapshots()))
		    					.build();
    }
    

}
