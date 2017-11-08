package com.tozny.e3db;

import java.util.concurrent.CountDownLatch;

/**
 * Represents an action that should execute asynchronously. The {@link wait}
 * parameter is given so the action can indicate when it has completed (by calling
 * {@link CountDownLatch#countDown()}.
 */
public interface AsyncAction {
  void act(CountDownLatch wait) throws Exception;
}
