package explorer.net.netty;

import explorer.net.Handler;
import explorer.net.TestingServer;
import explorer.utils.ListenableFutureAdapter;
import explorer.utils.NettyFutureAdapter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyServer implements TestingServer {

  private static Logger log = LoggerFactory.getLogger(NettyServer.class);

  private final int portNumber;
  private final int numClients;
  private final Handler handler;

  private Channel channel;

  public NettyServer(int portNumber, int numClients, Handler handler) {
    this.portNumber = portNumber;
    this.numClients = numClients;
    this.handler = handler;
  }

  @Override
  public void run() {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup(1);
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new ExplorerInitializer(handler))
          .option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.SO_KEEPALIVE, true);

      ChannelFuture f = bootstrap.bind("0.0.0.0", portNumber).sync();
      channel = f.channel();
      channel.closeFuture().sync();
    } catch (InterruptedException e) {
      log.error("Interrupted during sync");
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }

  @Override
  public void stop() {
   if (channel != null) {
     try {
       channel.close().sync();
     } catch (InterruptedException e) {
       log.error("Interrupted during close");
     }
   }
  }

  private static class ExplorerInitializer extends ChannelInitializer<SocketChannel> {

    private Handler handler;

    ExplorerInitializer(Handler handler) {
      this.handler = handler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
      ch.pipeline().addLast(
          new LineBasedFrameDecoder(1000, true, true),
          new StringDecoder(CharsetUtil.UTF_8),
          new ExplorerHandlerAdapter(handler)
      );
    }
  }
}