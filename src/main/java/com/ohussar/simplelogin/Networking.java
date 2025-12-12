package com.ohussar.simplelogin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

public class Networking {
    public record PacketData(String uuid) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PacketData> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("simplelogin", "packetdata"));

        // Each pair of elements defines the stream codec of the element to encode/decode and the getter for the element to encode
        // 'name' will be encoded and decoded as a string
        // 'age' will be encoded and decoded as an integer
        // The final parameter takes in the previous parameters in the order they are provided to construct the payload object
        public static final StreamCodec<ByteBuf, PacketData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                PacketData::uuid,
                PacketData::new
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    @SubscribeEvent // on the mod event bus
    public static void register(final RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.NETWORK);
        registrar.playBidirectional(
                PacketData.TYPE,
                PacketData.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        Networking::handleClientData,
                        Networking::handleData
                )
        );
    }

    public static void handleClientData(final PacketData data, final IPayloadContext context){
        context.enqueueWork(() ->{
            String read = Config.readFromFile("password");
            JsonElement element = JsonParser.parseString(read);
            String uuid = element.getAsJsonObject().get("password").getAsString();
            PacketDistributor.sendToServer(new PacketData(uuid));
        }).exceptionally(e -> {
            context.disconnect(Component.literal("hahahahh"));
            return null;
        });


    }


    public static void handleData(final PacketData data, final IPayloadContext context){
        context.enqueueWork(() ->{

            String userUUID = data.uuid;
            if(context.player() instanceof ServerPlayer player){
                String username = player.getName().getString();
                UUID uuid = UUID.fromString(userUUID);
                if(simplelogin.userPasswords.containsKey(username)){
                    UUID inServer = simplelogin.userPasswords.get(username);
                    if(!inServer.equals(uuid)){
                        player.setPos(simplelogin.userPositionPreLogin.get(username));
                        player.setGameMode(GameType.SURVIVAL);
                        player.connection.disconnect(Component.literal("Você não é o jogador: " + username + ". Se isso for um erro, contate um administrador."));
                        simplelogin.usersAwaitingVerification.remove(username);
                        simplelogin.userPositionPreLogin.remove(username);
                    }else{
                        player.setPos(simplelogin.userPositionPreLogin.get(username));
                        player.setGameMode(GameType.SURVIVAL);
                        simplelogin.usersAwaitingVerification.remove(username);
                        simplelogin.userPositionPreLogin.remove(username);
                    }
                }else{
                    simplelogin.userPasswords.put(username, uuid);
                    JsonObject obj = new JsonObject();
                    obj.addProperty(username, userUUID);
                    simplelogin.serverPasswordsJson.getAsJsonObject().get("info").getAsJsonArray().add(obj);
                    Config.writeCustomFile("server_passwords", simplelogin.serverPasswordsJson.toString());
                }

            }


        }).exceptionally(e -> {
            context.disconnect(Component.literal("hahahahh"));
            return null;
        });
    }
}
