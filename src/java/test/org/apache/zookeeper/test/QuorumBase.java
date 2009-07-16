/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.zookeeper.PortAssignment;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import org.junit.After;

public class QuorumBase extends ClientBase {
    private static final Logger LOG = Logger.getLogger(QuorumBase.class);

    File s1dir, s2dir, s3dir, s4dir, s5dir;
    QuorumPeer s1, s2, s3, s4, s5;
    private int port1;
    private int port2;
    private int port3;
    private int port4;
    private int port5;

    @Override
    protected void setUp() throws Exception {
        LOG.info("STARTING " + getName());
        setupTestEnv();

        JMXEnv.setUp();

        setUpAll();

        port1 = PortAssignment.unique();
        port2 = PortAssignment.unique();
        port3 = PortAssignment.unique();
        port4 = PortAssignment.unique();
        port5 = PortAssignment.unique();
        hostPort = "127.0.0.1:" + port1
            + ",127.0.0.1:" + port2
            + ",127.0.0.1:" + port3
            + ",127.0.0.1:" + port4
            + ",127.0.0.1:" + port5;
        LOG.info("Ports are: " + hostPort);

        s1dir = ClientBase.createTmpDir();
        s2dir = ClientBase.createTmpDir();
        s3dir = ClientBase.createTmpDir();
        s4dir = ClientBase.createTmpDir();
        s5dir = ClientBase.createTmpDir();

        startServers();

        LOG.info("Setup finished");
    }
    void startServers() throws Exception {
        int tickTime = 2000;
        int initLimit = 3;
        int syncLimit = 3;
        HashMap<Long,QuorumServer> peers = new HashMap<Long,QuorumServer>();
        peers.put(Long.valueOf(1), new QuorumServer(1, new InetSocketAddress("127.0.0.1", port1 + 1000)));
        peers.put(Long.valueOf(2), new QuorumServer(2, new InetSocketAddress("127.0.0.1", port2 + 1000)));
        peers.put(Long.valueOf(3), new QuorumServer(3, new InetSocketAddress("127.0.0.1", port3 + 1000)));
        peers.put(Long.valueOf(4), new QuorumServer(4, new InetSocketAddress("127.0.0.1", port4 + 1000)));
        peers.put(Long.valueOf(5), new QuorumServer(5, new InetSocketAddress("127.0.0.1", port5 + 1000)));

        LOG.info("creating QuorumPeer 1 port " + port1);
        s1 = new QuorumPeer(peers, s1dir, s1dir, port1, 0, 1, tickTime, initLimit, syncLimit);
        assertEquals(port1, s1.getClientPort());
        LOG.info("creating QuorumPeer 2 port " + port2);
        s2 = new QuorumPeer(peers, s2dir, s2dir, port2, 0, 2, tickTime, initLimit, syncLimit);
        assertEquals(port2, s2.getClientPort());
        LOG.info("creating QuorumPeer 3 port " + port3);
        s3 = new QuorumPeer(peers, s3dir, s3dir, port3, 0, 3, tickTime, initLimit, syncLimit);
        assertEquals(port3, s3.getClientPort());
        LOG.info("creating QuorumPeer 4 port " + port4);
        s4 = new QuorumPeer(peers, s4dir, s4dir, port4, 0, 4, tickTime, initLimit, syncLimit);
        assertEquals(port4, s4.getClientPort());
        LOG.info("creating QuorumPeer 5 port " + port5);
        s5 = new QuorumPeer(peers, s5dir, s5dir, port5, 0, 5, tickTime, initLimit, syncLimit);
        assertEquals(port5, s5.getClientPort());
        LOG.info("start QuorumPeer 1");
        s1.start();
        LOG.info("start QuorumPeer 2");
        s2.start();
        LOG.info("start QuorumPeer 3");
        s3.start();
        LOG.info("start QuorumPeer 4");
        s4.start();
        LOG.info("start QuorumPeer 5");
        s5.start();
        LOG.info("started QuorumPeer 5");

        LOG.info ("Closing ports " + hostPort);
        for (String hp : hostPort.split(",")) {
            assertTrue("waiting for server up",
                       ClientBase.waitForServerUp(hp,
                                    CONNECTION_TIMEOUT));
            LOG.info(hp + " is accepting client connections");
        }

        // interesting to see what's there...
        JMXEnv.dump();
        // make sure we have these 5 servers listed
        Set<String> ensureNames = new LinkedHashSet<String>();
        for (int i = 1; i <= 5; i++) {
            ensureNames.add("InMemoryDataTree");
        }
        for (int i = 1; i <= 5; i++) {
            ensureNames.add("name0=ReplicatedServer_id" + i
                 + ",name1=replica." + i + ",name2=");
        }
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 5; j++) {
                ensureNames.add("name0=ReplicatedServer_id" + i
                     + ",name1=replica." + j);
            }
        }
        for (int i = 1; i <= 5; i++) {
            ensureNames.add("name0=ReplicatedServer_id" + i);
        }
        JMXEnv.ensureAll(ensureNames.toArray(new String[ensureNames.size()]));
    }

    @After
    @Override
    protected void tearDown() throws Exception {
        LOG.info("TearDown started");
        shutdown(s1);
        shutdown(s2);
        shutdown(s3);
        shutdown(s4);
        shutdown(s5);

        for (String hp : hostPort.split(",")) {
            assertTrue("waiting for server down",
                       ClientBase.waitForServerDown(hp,
                                           ClientBase.CONNECTION_TIMEOUT));
            LOG.info(hp + " is no longer accepting client connections");
        }

        JMXEnv.tearDown();

        LOG.info("FINISHED " + getName());
    }

    protected void shutdown(QuorumPeer qp) {
        try {
            qp.shutdown();
            qp.join(30000);
            if (qp.isAlive()) {
                fail("QP failed to shutdown in 30 seconds");
            }
        } catch (InterruptedException e) {
            LOG.debug("QP interrupted", e);
        }
    }

    protected ZooKeeper createClient()
        throws IOException, InterruptedException
    {
        return createClient(hostPort);
    }

    protected ZooKeeper createClient(String hp)
        throws IOException, InterruptedException
    {
        CountdownWatcher watcher = new CountdownWatcher();
        return createClient(watcher, hp);
    }
}
