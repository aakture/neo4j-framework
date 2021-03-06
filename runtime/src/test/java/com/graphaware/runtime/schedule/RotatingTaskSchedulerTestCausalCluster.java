/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.runtime.schedule;

import static com.graphaware.runtime.config.RuntimeConfiguration.TX_MODULES_PROPERTY_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.graphaware.common.policy.role.AnyRole;
import com.graphaware.common.policy.role.MasterOnly;
import com.graphaware.common.policy.role.SlavesOnly;
import com.graphaware.common.policy.role.WritableRole;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import com.graphaware.runtime.config.FluentRuntimeConfiguration;
import com.graphaware.runtime.metadata.GraphPropertiesMetadataRepository;
import com.graphaware.runtime.metadata.ModuleMetadataRepository;
import com.graphaware.test.integration.cluster.CausalClusterDatabasesintegrationTest;

public class RotatingTaskSchedulerTestCausalCluster extends CausalClusterDatabasesintegrationTest{

	private RotatingTaskScheduler rotatingTaskSchedulerLeader;
	private RotatingTaskScheduler rotatingTaskSchedulerFollower;
	private RotatingTaskScheduler rotatingTaskSchedulerReplica;

	@Before
	public void setUp() throws Exception{
		super.setUp();
		rotatingTaskSchedulerLeader = buildScheduler(getLeaderDatabase()); 	
		rotatingTaskSchedulerFollower = buildScheduler(getOneFollowerDatabase()); 	
		rotatingTaskSchedulerReplica = buildScheduler(getOneReplicaDatabase()); 	
	}

	private RotatingTaskScheduler buildScheduler(GraphDatabaseService database) {
		ModuleMetadataRepository txRepo = new GraphPropertiesMetadataRepository(database,
				FluentRuntimeConfiguration.defaultConfiguration(database), TX_MODULES_PROPERTY_PREFIX);
		
		AdaptiveTimingStrategy timingStrategy = AdaptiveTimingStrategy.defaultConfiguration().withBusyThreshold(10)
				.withDefaultDelayMillis(1000L).withMinimumDelayMillis(100L).withMaximumDelayMillis(10_000L)
				.withDelta(100).withMaxSamples(10).withMaxTime(1000);

		timingStrategy.initialize(database);
		RotatingTaskScheduler rts = new RotatingTaskScheduler(database, txRepo, timingStrategy);
		return rts;
	}
	
	@Test
	public void testHasCorrectRole_MASTER_ONLY() {
		assertFalse(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(MasterOnly.getInstance())));
		assertFalse(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(MasterOnly.getInstance())));
		assertFalse(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(MasterOnly.getInstance())));
	}
	
	@Test
	public void testHasCorrectRole_SLAVES_ONLY() {
		assertFalse(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(SlavesOnly.getInstance())));
		assertFalse(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(SlavesOnly.getInstance())));
		assertFalse(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(SlavesOnly.getInstance())));
	}

	@Test
	public void testHasCorrectRole_ANY() {
		assertTrue(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(AnyRole.getInstance())));
		assertTrue(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(AnyRole.getInstance())));
		assertTrue(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(AnyRole.getInstance())));
	}
	
	@Test
	public void testHasCorrectRole_WRITABLE() {
		assertTrue(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(WritableRole.getInstance())));
		assertFalse(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(WritableRole.getInstance())));
		assertFalse(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(WritableRole.getInstance())));
	}
	
//	@Test
//	public void testHasCorrectRole_FOLLOWERS_ONLY() {
//		assertFalse(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.FOLLOWERS_ONLY)));
//		assertTrue(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.FOLLOWERS_ONLY)));
//		assertFalse(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.FOLLOWERS_ONLY)));
//	}
//
//	@Test
//	public void testHasCorrectRole_READ_REPLICAS_ONLY() {
//		assertFalse(rotatingTaskSchedulerLeader.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.READ_REPLICAS_ONLY)));
//		assertFalse(rotatingTaskSchedulerFollower.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.READ_REPLICAS_ONLY)));
//		assertTrue(rotatingTaskSchedulerReplica.hasCorrectRole(MockTimerModuleContext.buildModule(InstanceRolePolicy.READ_REPLICAS_ONLY)));
//	}
}