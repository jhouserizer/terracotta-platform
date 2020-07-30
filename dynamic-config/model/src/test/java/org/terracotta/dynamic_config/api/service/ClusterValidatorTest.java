/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.api.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.util.Random;
import java.util.stream.Stream;

import static java.nio.file.Paths.get;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;

public class ClusterValidatorTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final Random random = new Random();

  @Test
  public void testDuplicateNodeNameSameStripe() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("foo", "localhost2");

    assertClusterValidationFails("Found duplicate node name: foo", newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testDuplicateNodeNameDifferentStripe() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("foo", "localhost2");

    assertClusterValidationFails("Found duplicate node name: foo", new Cluster(new Stripe(node1), new Stripe(node2)));
  }

  @Test
  public void testDuplicateAddress() {
    Node node1 = newTestNode("foo1", "localhost");
    Node node2 = newTestNode("foo2", "localhost");

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same address: 'localhost:9410'",
        newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testDuplicatePublicAddress() {
    Node node1 = newTestNode("foo1", "host1").setPublicHostname("public-host").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "host2").setPublicHostname("public-host").setPublicPort(9510);

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same public address: 'public-host:9510'",
        newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testNotAllNodesHavePublicAddress() {
    Node node1 = newTestNode("foo1", "host1").setPublicHostname("public-host").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "host2");

    assertClusterValidationFails(
        "Nodes with names: [foo2] don't have public addresses defined",
        newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testSamePublicAndPrivateAddressOnSameNode() {
    Node node = newTestNode("foo1", "host").setPort(9410).setPublicHostname("host").setPublicPort(9410);
    new ClusterValidator(newTestCluster(new Stripe(node))).validate();
  }

  @Test
  public void testSamePublicAndPrivateAddressAcrossNodes() {
    Node node1 = newTestNode("foo1", "host1").setPort(9410).setPublicHostname("host2").setPublicPort(9410);
    Node node2 = newTestNode("foo2", "host2").setPort(9410).setPublicHostname("host1").setPublicPort(9410);
    new ClusterValidator(newTestCluster(new Stripe(node1, node2))).validate();
  }

  @Test
  public void testDuplicatePrivateAddressWithDifferentPublicAddresses() {
    Node node1 = newTestNode("foo1", "localhost").setPublicHostname("public-host1").setPublicPort(9510);
    Node node2 = newTestNode("foo2", "localhost").setPublicHostname("public-host2").setPublicPort(9510);

    assertClusterValidationFails(
        "Nodes with names: foo1, foo2 have the same address: 'localhost:9410'",
        newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testMalformedPublicAddress_missingPublicPort() {
    Node node = newTestNode("foo", "localhost").setPublicHostname("public-host");
    assertClusterValidationFails("Public address: 'public-host:null' of node with name: foo isn't well-formed",
        newTestCluster(new Stripe(node)));
  }

  @Test
  public void testMalformedPublicAddress_missingPublicHostname() {
    Node node = newTestNode("foo", "localhost").setPublicPort(9410);
    assertClusterValidationFails("Public address: 'null:9410' of node with name: foo isn't well-formed",
        newTestCluster(new Stripe(node)));
  }

  @Test
  public void testDifferingDataDirectoryNames() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2");
    node1.putDataDir("dir-1", get("data"));
    node2.putDataDir("dir-2", get("data"));

    assertClusterValidationFails("Data directory names need to match across the cluster", newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testSetSameBackupPath_ok() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2");
    node1.setBackupDir(get("backup"));
    node2.setBackupDir(get("backup"));
    new ClusterValidator(newTestCluster(new Stripe(node1), new Stripe(node2))).validate();
  }

  @Test
  public void testSetDifferentBackupPaths_ok() {
    Node node1 = newTestNode("node1", "localhost1");
    Node node2 = newTestNode("node2", "localhost2");
    node1.setBackupDir(get("backup-1"));
    node2.setBackupDir(get("backup-2"));
    new ClusterValidator(newTestCluster(new Stripe(node1), new Stripe(node2))).validate();
  }

  @Test
  public void testSetBackupOnOneStripeOnly_fail() {
    Node node1 = newTestNode("foo", "localhost1");
    Node node2 = newTestNode("node2", "localhost2");
    node1.setBackupDir(get("backup"));

    assertClusterValidationFails(
        "Nodes with names: [foo] don't have backup directories defined",
        newTestCluster(new Stripe(node1, node2)));
  }

  @Test
  public void testValidCluster() {
    Node[] nodes = Stream.of(
        newTestNode("node1", "localhost1"),
        newTestNode("node2", "localhost2")
    ).map(node -> node
        .setSecurityAuditLogDir(get("audit-" + random.nextInt()))
        .setSecurityDir(get("security-root" + random.nextInt()))
        .putDataDir("dir-1", get("some-path" + random.nextInt()))
        .setBackupDir(get("backup-" + random.nextInt()))
        .setMetadataDir(get("metadata-" + random.nextInt()))
        .setLogDir(get("logs-" + random.nextInt()))
        .setName("-" + random.nextInt())
        .setHostname("host-" + random.nextInt())
        .setPort(1 + random.nextInt(65500))
        .setGroupPort(1 + random.nextInt(65500))
        .setBindAddress(generateAddress())
        .setGroupBindAddress(generateAddress())
    ).toArray(Node[]::new);
    Cluster cluster = newTestCluster(new Stripe(nodes))
        .setSecurityAuthc("file")
        .setSecuritySslTls(true)
        .setSecurityWhitelist(false)
        .putOffheapResource("main", 1L, GB)
        .setFailoverPriority(consistency())
        .setClientReconnectWindow(100L, SECONDS)
        .setClientLeaseDuration(100L, SECONDS);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_1() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-dir")).setSecurityAuditLogDir(get("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-dir")).setSecurityAuditLogDir(get("security-audit-dir"));

    Cluster cluster = newTestCluster(new Stripe(node1, node2)).setSecuritySslTls(false).setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_2() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-dir")).setSecurityAuditLogDir(get("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-dir")).setSecurityAuditLogDir(get("security-audit-dir"));

    Cluster cluster = newTestCluster(new Stripe(node1, node2)).setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_3() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-root-dir"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-root-dir"));
    Cluster cluster = newTestCluster(new Stripe(node1, node2)).setSecurityAuthc("file");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_4() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-root-dir"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-root-dir"));

    Cluster cluster = newTestCluster(new Stripe(node1, node2)).setSecuritySslTls(true).setSecurityAuthc("certificate");
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testGoodSecurity_5() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-root-dir")).setSecurityAuditLogDir(get("security-audit-dir"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-root-dir")).setSecurityAuditLogDir(get("security-audit-dir"));

    Cluster cluster = newTestCluster(new Stripe(node1, node2))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate")
        .setSecurityWhitelist(true);
    new ClusterValidator(cluster).validate();
  }

  @Test
  public void testBadSecurity_authcWithoutSslTls() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(new Stripe(node))
        .setSecuritySslTls(false)
        .setSecurityAuthc("certificate");

    assertClusterValidationFails("ssl-tls is required for authc=certificate", cluster);
  }

  @Test
  public void testBadSecurity_sslTlsAuthcWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(new Stripe(node))
        .setSecuritySslTls(true)
        .setSecurityAuthc("certificate");

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_sslTlsWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(new Stripe(node)).setSecuritySslTls(true);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_authcWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(new Stripe(node)).setSecurityAuthc("file");

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_auditLogDirWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1").setSecurityAuditLogDir(get("."));
    Cluster cluster = newTestCluster(new Stripe(node));

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_whitelistWithoutSecurityDir() {
    Node node = newTestNode("node1", "localhost1");
    Cluster cluster = newTestCluster(new Stripe(node)).setSecurityWhitelist(true);

    assertClusterValidationFails("security-dir is mandatory for any of the security configuration", cluster);
  }

  @Test
  public void testBadSecurity_securityDirWithoutSecurity() {
    Node node = newTestNode("node1", "localhost1").setSecurityDir(get("security-dir"));
    Cluster cluster = newTestCluster(new Stripe(node));

    assertClusterValidationFails("One of ssl-tls, authc, or whitelist is required for security configuration", cluster);
  }

  @Test
  public void testBadSecurity_notAllNodesHaveAuditLogDir() {
    Node node1 = newTestNode("node1", "localhost1").setSecurityDir(get("security-dir")).setSecurityAuditLogDir(get("audit"));
    Node node2 = newTestNode("node2", "localhost2").setSecurityDir(get("security-dir"));
    Cluster cluster = newTestCluster(new Stripe(node1, node2)).setSecurityWhitelist(true);

    assertClusterValidationFails("Nodes with names: [node2] don't have audit log directories defined", cluster);
  }

  private String generateAddress() {
    return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
  }

  private void assertClusterValidationFails(String message, Cluster cluster) {
    exception.expect(MalformedClusterException.class);
    exception.expectMessage(message);
    new ClusterValidator(cluster).validate();
  }
}