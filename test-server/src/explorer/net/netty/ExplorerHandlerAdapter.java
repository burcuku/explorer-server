package explorer.net.netty;

import explorer.net.Handler;

import explorer.net.MessageSender;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class ExplorerHandlerAdapter extends ChannelInboundHandlerAdapter {

  private static Logger log = LoggerFactory.getLogger(ExplorerHandlerAdapter.class);
  private static final Set<Integer> CONTEXT_IDS = new HashSet<>();

  private final Handler handler;
  private final AttributeKey<Integer> idKey = AttributeKey.valueOf("id");

  ExplorerHandlerAdapter(Handler handler) {
    this.handler = handler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.attr(idKey).setIfAbsent(CONTEXT_IDS.size() + 1);
    int id = ctx.attr(idKey).get();
    log.info("Connected node: {}", id);

    CONTEXT_IDS.add(id);

    handler.onConnect(id, new MessageSenderAdapter(ctx));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    int id = ctx.attr(idKey).get();
    CONTEXT_IDS.remove(id);

    log.info("Disconnected node: {}", id);

    handler.onDisconnect(id);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    int id = ctx.attr(idKey).get();

    if (log.isDebugEnabled()) {
      log.debug("Received message from {}: {}", id, msg);
    }

    handler.onReceive(id, String.valueOf(msg));
  }

  private static class MessageSenderAdapter implements MessageSender {

    private ChannelHandlerContext channelHandlerContext;

    MessageSenderAdapter(ChannelHandlerContext channelHandlerContext) {
      this.channelHandlerContext = channelHandlerContext;
    }

    @Override
    public void send(String message) {
      channelHandlerContext.writeAndFlush(message); // async operation.
    }
  }
}
