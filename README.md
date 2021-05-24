# `viaprotohack`

One of the problems I've run into writing plugins for a
server supporting multiple versions is not being able to
detect when a player sends a packet available in a newer
version but not available in an older version.
[ViaVersion](https://github.com/ViaVersion/ViaVersion)
abstracts this away, so plugins can't figure out when a 1.14
player sends a USE_ITEM packet for example. By using the
`doViaHack(...)` method, plugins can now detect those
packets prior to being processed by ViaVersion.

# Building

``` shell
git clone https://github.com/caojohnny/viaprotohack.git
cd viaprotohack
mvn clean install
```

The demo can be found at `target/ViaProtoHack.jar`.

# Demo Usage

The demo jar can be placed into the plugins folder of your
1.8 server running ViaVersion. Enter the server on a 1.14
client and equip a sword and shield. The plugin will tell
the player when they are using the shield or just doing a
regular right click as detected by through the USE_ITEM
packet when you run /vph to toggle printing.

# Caveats

- To read a `PacketWrapper`, you MUST use `passthrough()`,
and NOT the `read()` function
- This is not a replacement for something like ProtocolLib.
If you want to listen to incoming or outgoing packets and
want to learn how, see
[TinyProtocol](https://github.com/aadnk/ProtocolLib/tree/master/modules/TinyProtocol).
- This is a demo plugin. It was designed for *developers* to
interact with ViaVersion. Demo functionality is not intended
to be a functional, standalone plugin.

# Credits

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)
