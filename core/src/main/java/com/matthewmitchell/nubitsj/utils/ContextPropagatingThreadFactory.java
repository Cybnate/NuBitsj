package com.matthewmitchell.nubitsj.utils;

import com.google.common.base.*;
import com.matthewmitchell.nubitsj.core.*;
import org.slf4j.*;

import java.util.concurrent.*;

/**
 * A {@link java.util.concurrent.ThreadFactory} that propagates a {@link com.matthewmitchell.nubitsj.core.Context} from the creating
 * thread into the new thread. This factory creates daemon threads.
 */
public class ContextPropagatingThreadFactory implements ThreadFactory {
    private static final Logger log = LoggerFactory.getLogger(ContextPropagatingThreadFactory.class);
    private final String name;
    private final int priority;

    public ContextPropagatingThreadFactory(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public ContextPropagatingThreadFactory(String name) {
        this(name, Thread.NORM_PRIORITY);
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Context context = Context.get();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Context.propagate(context);
                    r.run();
                } catch (Exception e) {
                    log.error("Exception in thread", e);
                    Throwables.propagate(e);
                }
            }
        }, name);
        thread.setPriority(priority);
        thread.setDaemon(true);
        Thread.UncaughtExceptionHandler handler = Threading.uncaughtExceptionHandler;
        if (handler != null)
            thread.setUncaughtExceptionHandler(handler);
        return thread;
    }
}
