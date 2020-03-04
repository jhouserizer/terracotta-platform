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
package org.terracotta.dynamic_config.xml.plugins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.testing.TmpDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DataDirectoriesElementTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testCreateDataDirectories() {
    Path dataRoot1 = temporaryFolder.getRoot().resolve("user-data-1");
    Path dataRoot2 = temporaryFolder.getRoot().resolve("user-data-2");
    Path metadataRoot = temporaryFolder.getRoot().resolve("metadata");

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    org.terracotta.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, metadataRoot, pathResolver).createDataDirectories();

    Map<String, Pair> expected = new HashMap<>();
    expected.put("data-root-1", new Pair(dataRoot1.toString(), false));
    expected.put("data-root-2", new Pair(dataRoot2.toString(), false));
    expected.put(DataDirectories.META_DATA_ROOT_NAME, new Pair(metadataRoot.toString(), true));

    List<DataRootMapping> dataRootMappings = dataDirectories.getDirectory();
    Map<String, Pair> actual = new HashMap<>();

    for (DataRootMapping dataRootMapping : dataRootMappings) {
      actual.put(dataRootMapping.getName(), new Pair(dataRootMapping.getValue(), dataRootMapping.isUseForPlatform()));
    }

    assertThat(actual, is(expected));
  }

  @Test
  public void testCreateDataDirectoriesWithOverlappingMetadataRoot() {
    Path dataRoot1 = temporaryFolder.getRoot().resolve("user-data-1");
    Path dataRoot2 = temporaryFolder.getRoot().resolve("user-data-2");

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    org.terracotta.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, dataRoot1, pathResolver).createDataDirectories();

    Map<String, Pair> expected = new HashMap<>();
    expected.put("data-root-1", new Pair(dataRoot1.toString(), true));
    expected.put("data-root-2", new Pair(dataRoot2.toString(), false));

    List<DataRootMapping> dataRootMappings = dataDirectories.getDirectory();
    Map<String, Pair> actual = new HashMap<>();

    for (DataRootMapping dataRootMapping : dataRootMappings) {
      actual.put(dataRootMapping.getName(), new Pair(dataRootMapping.getValue(), dataRootMapping.isUseForPlatform()));
    }

    assertThat(actual, is(expected));
  }

  private static class Pair {
    private final String path;
    private final boolean isPlatformRoot;

    private Pair(String path, boolean isPlatformRoot) {
      this.path = path;
      this.isPlatformRoot = isPlatformRoot;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Pair pair = (Pair) o;
      return isPlatformRoot == pair.isPlatformRoot &&
          Objects.equals(path, pair.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, isPlatformRoot);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Pair{");
      sb.append("path='").append(path).append('\'');
      sb.append(", isPlatformRoot=").append(isPlatformRoot);
      sb.append('}');
      return sb.toString();
    }
  }

}