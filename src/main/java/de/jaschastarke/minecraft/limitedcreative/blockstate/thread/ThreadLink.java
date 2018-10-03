package de.jaschastarke.minecraft.limitedcreative.blockstate.thread;

import de.jaschastarke.bukkit.lib.ModuleLogger;
import de.jaschastarke.minecraft.limitedcreative.ModBlockStates;
import de.jaschastarke.minecraft.limitedcreative.blockstate.AbstractModel.HasBlockState;
import de.jaschastarke.minecraft.limitedcreative.blockstate.BlockState;
import de.jaschastarke.minecraft.limitedcreative.blockstate.DBModel.Cuboid;
import de.jaschastarke.minecraft.limitedcreative.blockstate.DBQueries;
import de.jaschastarke.minecraft.limitedcreative.blockstate.ThreadedModel;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class ThreadLink {
    private static final int BATCH_ACTION_LENGTH = 25;
    private static final int QUEUE_ACCESS_WARNING_DURATION = 5; // ms
    private static final int COUNT_WARNING_QUEUE = 5;
    private static final int COUNT_ERROR_QUEUE = 20;
    private static final int QUEUE_TIMING_DURATION = 500; // ms
    private static final int STARTUP_TIMING = 30000; // ms
    private static final int THREAD_SHUTDOWN_WAIT_MS = 30000;
    private long lastTimeout;
    private final Stack<Action> updateQueue = new Stack<Action>();

    private boolean shutdown;
    private ModuleLogger log;
    private ThreadedModel model;
    private Thread thread;
    
    public ThreadLink(ThreadedModel threadedModel, DBQueries queries) {
        model = threadedModel;
        log = threadedModel.getModel().getLog();
        
        /*
         * In theory we could add multiple threads, e.g. 1 write and 2 read threads.
         */
        createThread(queries);
        shutdown = true; //Don't allow the thread to run until it's started
    }
    
    private class DBThread extends Thread {
        private DBQueries q;
        public DBThread(DBQueries queries) {
            super();
            this.q = queries;
        }
        public void run() {
            if (getModule().isDebug())
                log.debug("DB-Thread '" + Thread.currentThread().getName() + "' started.");
            lastTimeout = System.currentTimeMillis() + STARTUP_TIMING;
            while (!shutdown || !updateQueue.isEmpty()) {
                try {
                    //Thread.sleep(1000);
                    //throw new RuntimeException("Test exception pls ignore");
                    List<Action> acts = new LinkedList<Action>();
                    synchronized (updateQueue) {
                        while (updateQueue.isEmpty() && !shutdown)
                            updateQueue.wait();
                        if (updateQueue.size() > (BATCH_ACTION_LENGTH * COUNT_ERROR_QUEUE)) {
                            if (System.currentTimeMillis() - lastTimeout > QUEUE_TIMING_DURATION) {
                                getLog().warn("Extrem large DB-Queue in " + Thread.currentThread().getName() + ": " + updateQueue.size());
                                lastTimeout = System.currentTimeMillis();
                            }
                        } else if (updateQueue.size() > (BATCH_ACTION_LENGTH * COUNT_WARNING_QUEUE)) {
                            if (System.currentTimeMillis() - lastTimeout > QUEUE_TIMING_DURATION) {
                                getLog().info("Large DB-Queue in " + Thread.currentThread().getName() + ": " + updateQueue.size());
                                lastTimeout = System.currentTimeMillis();
                            }
                        } else if (updateQueue.size() <= BATCH_ACTION_LENGTH) {
                            lastTimeout = System.currentTimeMillis();
                        }
                        for (int i = 0; i < BATCH_ACTION_LENGTH && !updateQueue.isEmpty(); i++) {
                            acts.add(updateQueue.pop());
                        }
                    }
                    long t = 0;
                    if (getModule().isDebug()) {
                        t = System.currentTimeMillis();
                        log.debug("DB-Thread '" + Thread.currentThread().getName() + "' run: " + acts.size());
                    }
                    for (Action act : acts) {
                        if (!shutdown || !(act instanceof CacheChunkAction)) {
                            if (act instanceof CallableAction) {
                                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                                synchronized (act) {
                                    act.process(ThreadLink.this, this.q);
                                    act.notify();
                                }
                            } else {
                                act.process(ThreadLink.this, this.q);
                            }
                        }
                    }
                    if (getModule().isDebug())
                        log.debug("DB-Thread '" + Thread.currentThread().getName() + "' execution time: " + (System.currentTimeMillis() - t) + "ms");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    log.severe("DB-Thread '" + Thread.currentThread().getName() + "' was harmfully interupted");
                }
                Thread.yield();
            }
            if (getModule().isDebug())
                log.debug("DB-Thread " + Thread.currentThread().getName() + " finished.");
        }
    }

    public void start() {
        shutdown = false;
        if (!thread.isAlive())
            thread.start();
    }
    
    
    public void queueUpdate(Block block) {
        restartThreadIfNeeded();
        long l = System.currentTimeMillis();
        synchronized (updateQueue) {
            updateQueue.add(new UpdateBlockStateAction(block));
            updateQueue.notifyAll();
        }
        long l2 = System.currentTimeMillis();
        if (l2 - l > QUEUE_ACCESS_WARNING_DURATION) {
            getLog().warn("queueUpdate-action took too long: " + (l2 - l) + "ms");
        }
    }
    
    public BlockState callUpdate(Block block) {
        restartThreadIfNeeded();
        FetchBlockStateAction action = new FetchBlockStateAction(block);
        synchronized (updateQueue) {
            updateQueue.push(action);
            updateQueue.notifyAll();
        }
        return action.getValue();
    }
    
    public void queue(Action act) {
        restartThreadIfNeeded();
        synchronized (updateQueue) {
            updateQueue.add(act);
            updateQueue.notifyAll();
        }
    }
    public <T> T call(CallableAction<T> act) {
        restartThreadIfNeeded();
        synchronized (updateQueue) {
            updateQueue.push(act);
            updateQueue.notifyAll();
        }
        return act.getValue();
    }
    
    public List<BlockState> callUpdate(Cuboid c) {
        FetchCuboidAction action = new FetchCuboidAction(c);
        synchronized (updateQueue) {
            updateQueue.push(action);
            updateQueue.notifyAll();
        }
        return action.getValue();
    }

    public void queueMetaMove(Location from, Location to) {
        restartThreadIfNeeded();
        synchronized (updateQueue) {
            updateQueue.add(new MoveBlockStateAction(from, to));
            updateQueue.notifyAll();
        }
    }

    public void queueChunkLoad(Chunk chunk) {
        restartThreadIfNeeded();
        synchronized (updateQueue) {
            updateQueue.add(new CacheChunkAction(chunk));
            updateQueue.notifyAll();
        }
    }

    public void queueTransaction(Transaction transaction) {
        restartThreadIfNeeded();
        synchronized (updateQueue) {
            updateQueue.add(transaction);
            updateQueue.notifyAll();
        }
    }

    public void shutdown() throws InterruptedException {
        synchronized (updateQueue) {
            shutdown = true;
            updateQueue.notifyAll();
        }
        thread.join(THREAD_SHUTDOWN_WAIT_MS);
        if (thread.isAlive())
            thread.interrupt(); //Wake it up
    }

    public HasBlockState getMetaState(Block block) {
        return model.getMetaState(block);
    }
    public void setMetaState(Block block, BlockState state) {
        model.setMetaState(block, state);
    }
    public void setSimpleMetaState(Block block, BlockState state) {
        model.setSimpleMetaDataState(block, state);
    }
    
    public ModBlockStates getModule() {
        return model.getModel();
    }
    
    public ModuleLogger getLog() {
        return log;
    }

    private void restartThreadIfNeeded() {
        if ((thread != null && thread.isAlive()) || shutdown)
            return;
        log.warn("Thread is dead, restarting!");
        new Exception("Thread-restarting update called").printStackTrace();
        createThread(((DBThread) thread).q);
        start();
    }

    private void createThread(DBQueries queries) {
        if (shutdown)
            return;
        thread = new DBThread(queries);
        thread.setName("LC BlockState DB-Thread");
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                e.printStackTrace();
                log.severe("Thread " + thread.getName() + " encoutered an uncaught Exception: " + e.getMessage());
            }
        });
    }
}
