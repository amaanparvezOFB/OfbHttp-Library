import static org.junit.jupiter.api.Assertions.*;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.example.ExtendedFutureCallback;
import org.example.OfbHttp;
import org.example.OfbRequest;
import org.example.OfbResponse;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

class OfbHttpTest2 {

    private static final String TEST_URL = "https://httpbin.org/get";

    @Test
    void testConcurrentRequests() throws InterruptedException, ExecutionException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        CountDownLatch latch = new CountDownLatch(100);
        final int[] successfulResponses = {0};
        final int[] failedResponses = {0};

        List<ListenableFuture<OfbResponse>> futures = new ArrayList<>();

        // Fire 100 concurrent GET requests
        for (int i = 0; i < 100; i++) {
            String tag = "TAG" + i;
            ListenableFuture<OfbResponse> future = OfbHttp.execute(new OfbRequest("GET", TEST_URL, headers, tag));
            futures.add(future);

            Futures.addCallback(future, new ExtendedFutureCallback<OfbResponse>() {
                @Override
                public void onSuccess(OfbResponse result) {
                    synchronized (successfulResponses) {
                        successfulResponses[0]++;
                    }
                    latch.countDown();
                }

                @Override
                public void onFailure(Throwable t) {
                    synchronized (failedResponses) {
                        failedResponses[0]++;
                    }
                    latch.countDown();
                }


                @Override
                public void onCancel() {
                    synchronized (successfulResponses) {
                        failedResponses[0]++;
                    }
                    latch.countDown();
                }
            }, OfbHttp.executorService);
        }

        OfbHttp.cancel("TAG98");

        // Wait for all requests to complete or timeout after 10 seconds
        boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

        System.out.println("Total Requests Sent: " + 100);
        System.out.println("Successful Responses: " + successfulResponses[0]);
        System.out.println("Failed/Canceled Responses: " + failedResponses[0]);

        // Assertions
        assertTrue(completed, "Not all requests completed in time.");
        assertEquals(100, successfulResponses[0] + failedResponses[0], "Mismatch in total responses received.");
    }
}
