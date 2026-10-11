package com.shadow.injector;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

/**
 * Small helpers so the Sketchware blocks can pass either an Activity
 * (<code>MainActivity.this</code>) or any View of the current screen.
 */
public class AndroidHelper {

    private AndroidHelper() {
    }

    /** Walks up the context chain of a view until an Activity is found. */
    public static Activity getActivity(View view) {
        if (view == null) {
            return null;
        }
        return getActivity(view.getContext());
    }

    public static Activity getActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    /** Any usable Context for a view (never null unless the view is null). */
    public static Context getContext(View view) {
        if (view == null) {
            return null;
        }
        return view.getContext();
    }
}
