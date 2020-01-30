/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import java.net.InetSocketAddress;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SimpleActivateCommandIT extends BaseStartupIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId));

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  @Test
  public void testWrongParams_1() {
    int[] ports = this.ports.getPorts();
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Cluster name should be provided when node is specified"));
    ConfigTool.start("activate", "-s", "localhost:" + ports[0]);
  }

  @Test
  public void testWrongParams_2() {
    int[] ports = this.ports.getPorts();
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Either node or config properties file should be specified, not both"));
    ConfigTool.start("activate", "-s", "localhost:" + ports[0], "-f", "dummy.properties");
  }

  @Test
  public void testWrongParams_4() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("One of node or config properties file must be specified"));
    ConfigTool.start("activate");
  }

  @Test
  public void testSingleNodeActivation() throws Exception {
    activateCluster(() -> {
      waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

      waitedAssert(out::getLog, containsString("No license installed"));
      waitedAssert(out::getLog, containsString("came back up"));
      waitedAssert(out::getLog, containsString("Command successful"));
    });
  }

  @Test
  public void testSingleNodeActivationWithConfigFile() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.start("activate", "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString(), "-n", "my-cluster");
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));

    waitedAssert(out::getLog, containsString("No license installed"));
    waitedAssert(out::getLog, containsString("came back up"));
    waitedAssert(out::getLog, containsString("Command successful"));

    // TDB-4726
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(InetSocketAddress.createUnresolved("localhost", ports[0]), "diag", ofSeconds(10), ofSeconds(10), null)) {
      NodeContext runtimeNodeContext = diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext();
      assertThat(runtimeNodeContext.getCluster().getName(), is(equalTo("my-cluster")));
    }
  }
}
