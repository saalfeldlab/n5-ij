package org.janelia.saalfeldlab.n5;

import java.util.Optional;
import java.util.function.Supplier;

public class TestRunners {
	
	public static <T> Optional<T> tryWaitRepeat(Supplier<T> supplier) {

		return tryWaitRepeat(supplier, 5, 50, 2);
	}

	public static <T> Optional<T> tryWaitRepeat(Supplier<T> supplier, int nTries) {

		return tryWaitRepeat(supplier, nTries, 50, 2);
	}
	
	public static <T> Optional<T> tryWaitRepeat(Supplier<T> supplier, int nTries, long waitTimeMillis) {

		return tryWaitRepeat(supplier, nTries, waitTimeMillis, 2);
	}

	/**
	 * Attempts to execute a provided {@link Supplier} multiple times, with an increasing wait period
	 * between each attempt. If the supplier returns a non-null result, it is wrapped in an 
	 * {@code Optional} and returned. If all attempts fail or return null, an empty {@link Optional} is returned.
	 *
	 * <p>The wait time between attempts increases after each failure, multiplied by a specified factor.
	 *
	 * @param <T> the type of result provided by the supplier
	 * @param supplier the {@link Supplier} function that provides the result to be evaluated. The 
	 *        function may throw a {@link RuntimeException} if it fails, which will be caught and retried.
	 * @param nTries the maximum number of attempts to invoke the supplier
	 * @param initialWaitTimeMillis the initial wait time in milliseconds before retrying after the first failure
	 * @param waitTimeMultiplier the multiplier to apply to the wait time after each failure, increasing 
	 *        the wait time for subsequent retries
	 * @return an {@link Optional} containing the result from the supplier if a non-null result is returned 
	 *         before the maximum number of tries, or an empty {@code Optional} if all attempts fail or 
	 *         return null
	 */
	public static <T> Optional<T> tryWaitRepeat(
			final Supplier<T> supplier, 
			final int nTries, 
			final long initialWaitTimeMillis, 
			final int waitTimeMultiplier ) {
		
		int i = 0;
		long waitTime = initialWaitTimeMillis;
		while (i < nTries) {

			if (i == nTries)
				break;

			try {
				T result = supplier.get();
				if (result != null)
					return Optional.of(result);
			} catch (RuntimeException e) {}

			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {}

			System.out.println("try again");
			waitTime *= waitTimeMultiplier;
			i++;
		}

		return Optional.empty();
	}

}
