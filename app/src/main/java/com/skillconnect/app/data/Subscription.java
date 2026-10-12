package com.skillconnect.app.data;

/** Handle for a real-time listener. Call remove() to stop listening. */
public interface Subscription {
    void remove();
}
