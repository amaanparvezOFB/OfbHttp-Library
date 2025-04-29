package org.example;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;

public interface ExtendedFutureCallback<T> extends FutureCallback<T> {
    void onCancel();
}
