package com.victorgponce.entityrenderdisablerrewritefabric.client.culling;

public final class CullingEffects {
    private static int depth;

    private CullingEffects() {
    }

    public static boolean isSuppressed() {
        return depth > 0;
    }

    public static void run(boolean suppress, Runnable action) {
        if (suppress) {
            depth++;
        }
        try {
            action.run();
        } finally {
            if (suppress) {
                depth--;
            }
        }
    }
}
