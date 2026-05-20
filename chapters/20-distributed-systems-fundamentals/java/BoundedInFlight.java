// Scenario: Checkout API limits concurrent payment calls during flash sale
// Demonstrates: semaphore as backpressure — fail fast with 503 when saturated
// Trade-off: reject at edge vs unbounded queue that OOMs the JVM

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

final class BoundedInFlight {
  private final Semaphore slots;

  BoundedInFlight(int maxConcurrent) {
    this.slots = new Semaphore(maxConcurrent);
  }

  <T> T runWithBackpressure(java.util.concurrent.Callable<T> work) throws Exception {
    if (!slots.tryAcquire(50, TimeUnit.MILLISECONDS)) {
      throw new ServiceUnavailableException("overloaded");
    }
    try {
      return work.call();
    } finally {
      slots.release();
    }
  }

  static final class ServiceUnavailableException extends RuntimeException {
    ServiceUnavailableException(String msg) { super(msg); }
  }
}
