
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.example.ExtendedFutureCallback;
import org.example.OfbHttp;
import org.example.OfbRequest;
import org.example.OfbResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class OfbHttpTest {
    private static final String TEST_URL = "https://httpbin.org/get";
    private static final String TAG1 = "tag1";
    private static final String TAG2 = "tag2";

    @AfterAll
    static void teardown() {
        OfbHttp.executorService.shutdownNow();
    }

    @Test
    void testCancelOneOfTwoRequests() throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        ListenableFuture<OfbResponse> future1 = OfbHttp.execute(new OfbRequest("GET", TEST_URL, headers, TAG1));
        ListenableFuture<OfbResponse> future2 = OfbHttp.execute(new OfbRequest("GET", TEST_URL, headers, TAG2));

        CountDownLatch latch = new CountDownLatch(2);

        Futures.addCallback(future1, new ExtendedFutureCallback<OfbResponse>() {
            @Override
            public void onSuccess(OfbResponse result) {
                fail("TAG1 request should have been canceled");
            }

            @Override
            public void onFailure(Throwable t) {
                assertTrue(t instanceof Exception, "TAG1 request should be canceled");
                latch.countDown();
            }

            @Override
            public void onCancel() {
                latch.countDown();
            }
        }, OfbHttp.executorService);

        Futures.addCallback(future2, new ExtendedFutureCallback<OfbResponse>() {
            @Override
            public void onSuccess(OfbResponse result) {
                assertNotNull(result);
                assertTrue(result.getResponseBody().contains("url"));
                System.out.println("GET Success: " + result.getResponseBody());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                fail("TAG2 request should not fail");
            }

            @Override
            public void onCancel() {
                fail("TAG2 request should not be canceled");
            }
        }, OfbHttp.executorService);

        // Cancel the first request
        OfbHttp.cancel(TAG1);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
    }
}
