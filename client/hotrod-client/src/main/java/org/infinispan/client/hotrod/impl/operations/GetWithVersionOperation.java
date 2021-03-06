package org.infinispan.client.hotrod.impl.operations;

import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.impl.VersionedValueImpl;
import org.infinispan.client.hotrod.impl.protocol.Codec;
import org.infinispan.client.hotrod.impl.protocol.HeaderParams;
import org.infinispan.client.hotrod.impl.protocol.HotRodConstants;
import org.infinispan.client.hotrod.impl.transport.netty.ByteBufUtil;
import org.infinispan.client.hotrod.impl.transport.netty.ChannelFactory;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import net.jcip.annotations.Immutable;

/**
 * Corresponds to getWithVersion operation as described by
 * <a href="http://community.jboss.org/wiki/HotRodProtocol">Hot Rod protocol specification</a>.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.1
 */
@Immutable
@Deprecated
public class GetWithVersionOperation<V> extends AbstractKeyOperation<VersionedValue<V>> {

   private static final Log log = LogFactory.getLog(GetWithVersionOperation.class);
   private static final boolean trace = log.isTraceEnabled();

   public GetWithVersionOperation(Codec codec, ChannelFactory channelFactory, Object key, byte[] keyBytes,
                                  byte[] cacheName, AtomicInteger topologyId, int flags,
                                  Configuration cfg) {
      super(codec, channelFactory, key, keyBytes, cacheName, topologyId, flags, cfg);
   }

   @Override
   protected void executeOperation(Channel channel) {
      HeaderParams header = headerParams(GET_WITH_VERSION);
      scheduleRead(channel, header);
      sendArrayOperation(channel, header, keyBytes);
   }

   @Override
   public VersionedValue<V> decodePayload(ByteBuf buf, short status) {
      if (HotRodConstants.isNotExist(status) || !HotRodConstants.isSuccess(status)) {
         return null;
      }
      long version = ByteBufUtil.readVLong(buf);
      if (trace) {
         log.tracef("Received version: %d", version);
      }
      V value = codec.readUnmarshallByteArray(buf, status, cfg.serialWhitelist(), channelFactory.getMarshaller());
      return new VersionedValueImpl<V>(version, value);
   }
}
