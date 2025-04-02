package org.example;

import com.google.common.util.concurrent.FutureCallback;

public interface ExtendedFutureCallback<T> extends FutureCallback<T> {
    void onCancel();
}
