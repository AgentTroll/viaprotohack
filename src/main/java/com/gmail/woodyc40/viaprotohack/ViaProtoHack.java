package com.gmail.woodyc40.viaprotohack;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.providers.ViaProviders;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ViaProtoHack extends JavaPlugin {
    private boolean hacked = false;

    @Override
    public void onEnable() {
        this.getCommand("vph").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viaprotohack.vph")) {
            sender.sendMessage("No permission!");
            return true;
        }

        if (hacked) {
            hacked = false;

            for (Player player : Bukkit.getOnlinePlayers()) {
                doViaHack(player, true);
            }
        } else {
            hacked = true;

            for (Player player : Bukkit.getOnlinePlayers()) {
                doViaHack(player, false);
            }
        }

        return true;
    }

    private static void doViaHack(Player player, boolean restore) {
        UserConnection uc = Via.getManager().getConnection(player.getUniqueId());

        if (!restore) {
            uc.getStoredObjects().computeIfPresent(ProtocolInfo.class, (k, v) -> {
                ProtocolInfo pi = (ProtocolInfo) v;
                ProtocolPipeline pp = pi.getPipeline();
                if (!(pp instanceof WrapperProtocolPipeline)) {
                    pi.setPipeline(new WrapperProtocolPipeline(player, uc, pp));
                }

                return v;
            });
        } else {
            uc.getStoredObjects().computeIfPresent(ProtocolInfo.class, (k, v) -> {
                ProtocolInfo pi = (ProtocolInfo) v;
                ProtocolPipeline pp = pi.getPipeline();
                if (pp instanceof WrapperProtocolPipeline) {
                    ProtocolPipeline delegate = ((WrapperProtocolPipeline) pp).getDelegate();
                    pi.setPipeline(delegate);
                }

                return v;
            });
        }
    }

    private static void preTransformPacket(Player player, Direction direction, State state, PacketWrapper wrapper) throws Exception {
        if (direction == Direction.INCOMING && wrapper.getId() == 0x2D) {
            // Must use passthrough(), NOT read()
            int hand = wrapper.passthrough(Type.VAR_INT);
            player.sendMessage("Received a USE_ITEM packet for hand: " + hand);
        }
    }

    private static class WrapperProtocolPipeline extends ProtocolPipeline {
        private static final Method REGISTER_LISTENERS_METHOD =
                getMethod(Protocol.class, "registerListeners");
        private static final Method REGISTER_METHOD =
                getMethod(Protocol.class, "register", ViaProviders.class);
        private static final Method FILTER_PACKET_METHOD =
                getMethod(Protocol.class, "filterPacket", UserConnection.class, Object.class, List.class);

        private final Player player;
        private final ProtocolPipeline delegate;

        public WrapperProtocolPipeline(Player player, UserConnection userConnection, ProtocolPipeline delegate) {
            super(userConnection);
            this.player = player;
            this.delegate = delegate;
        }

        public ProtocolPipeline getDelegate() {
            return delegate;
        }

        @Override
        public void registerPackets() {
            // Suppress initialization from Protocol.super
        }

        @Override
        public void init(UserConnection userConnection) {
            // Suppress double initialization
        }

        @Override
        public void add(Protocol protocol) {
            delegate.add(protocol);
        }

        @Override
        public void transform(Direction direction, State state, PacketWrapper packetWrapper) throws Exception {
            preTransformPacket(this.player, direction, state, packetWrapper);
            packetWrapper.resetReader();

            delegate.transform(direction, state, packetWrapper);
        }

        @Override
        public boolean contains(Class<? extends Protocol> pipeClass) {
            return delegate.contains(pipeClass);
        }

        @Override
        public boolean filter(Object o, List list) throws Exception {
            return delegate.filter(o, list);
        }

        @Override
        public List<Protocol> pipes() {
            return delegate.pipes();
        }

        @Override
        public void cleanPipes() {
            delegate.cleanPipes();
        }

        @Override
        public boolean isFiltered(Class packetClass) {
            return delegate.isFiltered(packetClass);
        }

        @Override
        public void filterPacket(UserConnection info, Object packet, List output) throws Exception {
            invokeMethod(this, FILTER_PACKET_METHOD, info, packet, output);
        }

        @Override
        @Deprecated
        public void registerListeners() {
            invokeMethod(this, REGISTER_LISTENERS_METHOD);
        }

        @Override
        public void register(ViaProviders providers) {
            invokeMethod(this, REGISTER_METHOD, providers);
        }

        @Override
        public void registerIncoming(State state, int oldPacketID, int newPacketID) {
            delegate.registerIncoming(state, oldPacketID, newPacketID);
        }

        @Override
        public void registerIncoming(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper) {
            delegate.registerIncoming(state, oldPacketID, newPacketID, packetRemapper);
        }

        @Override
        public void registerIncoming(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper, boolean override) {
            delegate.registerIncoming(state, oldPacketID, newPacketID, packetRemapper, override);
        }

        @Override
        public void registerOutgoing(State state, int oldPacketID, int newPacketID) {
            delegate.registerOutgoing(state, oldPacketID, newPacketID);
        }

        @Override
        public void registerOutgoing(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper) {
            delegate.registerOutgoing(state, oldPacketID, newPacketID, packetRemapper);
        }

        @Override
        public void registerOutgoing(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper, boolean override) {
            delegate.registerOutgoing(state, oldPacketID, newPacketID, packetRemapper, override);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    // Reflection utils

    private static Method getMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeMethod(Object o, Method method, Object... params) {
        try {
            return method.invoke(o, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
