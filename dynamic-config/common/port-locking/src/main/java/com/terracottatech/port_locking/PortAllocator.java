/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

public interface PortAllocator {
  /**
   * Get portCount consecutive ports
   * @param portCount the number of ports to allocate
   * @return the lowest port
   */
  int allocatePorts(int portCount);
}
