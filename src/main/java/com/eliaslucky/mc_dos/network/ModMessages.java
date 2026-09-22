package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.Computers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
	private static SimpleChannel INSTANCE;
	private static int packetId = 0;

	private static int id() {
		return packetId++;
	}

	public static void register() {
		SimpleChannel net = NetworkRegistry.ChannelBuilder
				.named(ResourceLocation.fromNamespaceAndPath(Computers.MODID, "messages"))
				.networkProtocolVersion(() -> "1.0")
				.clientAcceptedVersions(s -> true)
				.serverAcceptedVersions(s -> true)
				.simpleChannel();

		INSTANCE = net;

		// CLIENT -> SERVER
		net.messageBuilder(ServerboundCommandPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ServerboundCommandPacket::new)
				.encoder(ServerboundCommandPacket::encode)
				.consumerMainThread(ServerboundCommandPacket::handle)
				.add();

		net.messageBuilder(ServerboundCloseTerminalPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ServerboundCloseTerminalPacket::new)
				.encoder(ServerboundCloseTerminalPacket::encode)
				.consumerMainThread(ServerboundCloseTerminalPacket::handle)
				.add();

		net.messageBuilder(ServerboundFileWritePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
				.decoder(ServerboundFileWritePacket::new)
				.encoder(ServerboundFileWritePacket::encode)
				.consumerMainThread(ServerboundFileWritePacket::handle)
				.add();

		// SERVER -> CLIENT
		net.messageBuilder(ClientboundTerminalOutputPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
			.decoder(ClientboundTerminalOutputPacket::new)
			.encoder(ClientboundTerminalOutputPacket::encode)
			.consumerMainThread(ClientboundTerminalOutputPacket::handle)
			.add();
	}

	public static void sendToServer(Object message) {
		INSTANCE.sendToServer(message);
	}

	public static void sendToPlayer(Object message, ServerPlayer player) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}
