package com.skillconnect.app.data;

/**
 * Generic asynchronous result callback used by every repository method.
 * Both methods are always invoked on the main (UI) thread.
 */
public interface Callback<T> {
    void onSuccess(T result);

    void onError(String message);
}
