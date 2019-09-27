package explorer.utils;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.util.concurrent.CompletableFuture;

public class NettyFutureAdapter<T> {

  private final CompletableFuture<T> completableFuture;

  private NettyFutureAdapter(Future<T> nettyFuture) {
    this.completableFuture = new CompletableFuture<T>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = nettyFuture.cancel(mayInterruptIfRunning);
        super.cancel(cancelled);
        return cancelled;
      }
    };

    nettyFuture.addListener((FutureListener<T>) tFuture -> {
      if (tFuture.isSuccess()) {
        completableFuture.complete(tFuture.get());
      } else {
        completableFuture.completeExceptionally(tFuture.cause());
      }
    });
  }

  private CompletableFuture<T> getCompletableFuture() {
    return completableFuture;
  }

  public static <T> CompletableFuture<T> toCompletable(Future<T> nettyFuture) {
    NettyFutureAdapter<T> listenableFutureAdapter = new NettyFutureAdapter<>(nettyFuture);
    return listenableFutureAdapter.getCompletableFuture();
  }




}
