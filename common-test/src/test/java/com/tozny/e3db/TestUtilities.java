package com.tozny.e3db;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtilities {
  private static final boolean INFINITE_WAIT = false;
  private static final int TIMEOUT = 7;
  /**
   * Runs an asynchronous action that requires a countdown latch
   * to indicate when it has finished. After {@link #TIMEOUT} seconds, an error is thrown to
   * indicate the action did not completed in time.
   */
  protected static void withTimeout(AsyncAction action) {
    try {
      CountDownLatch wait = new CountDownLatch(1);
      action.act(wait);
      if (!wait.await(INFINITE_WAIT ? Integer.MAX_VALUE : TIMEOUT, TimeUnit.SECONDS))
        throw new Error("Timed out.");
    }
    catch(Exception e) {
      throw new Error(e);
    }
  }

  /**
   * Wraps a result handler with logic to trigger a countdown latch, regardless of
   * the result of the handler.
   *
   * Should be used to wrap result handlers that contain assertion failures or that throw
   * exceptions. Otherwise, the count down latch may not be triggered and the test will
   * not complete until timeout occurs.
   * @param <T>
   */
  protected static final class ResultWithWaiting<T> implements ResultHandler<T> {
    private final CountDownLatch waiter;
    private final ResultHandler<T> handler;

    public ResultWithWaiting(CountDownLatch waiter, ResultHandler<T> handler) {
      this.waiter = waiter;
      this.handler = handler;
    }

    @Override
    public void handle(Result<T> r) {
      try {
        if(handler != null)
          handler.handle(r);
      }
      finally {
        waiter.countDown();
      }
    }
  }
}
